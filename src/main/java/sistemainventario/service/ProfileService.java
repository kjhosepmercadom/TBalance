package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.BusinessCapabilities;
import sistemainventario.model.ControlVencimiento;
import sistemainventario.model.Product;
import sistemainventario.model.Profile;
import sistemainventario.model.BusinessType;
import sistemainventario.repository.ProfileDirectory;
import sistemainventario.repository.Repository;
import sistemainventario.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ciclo de vida de los perfiles: crear, editar, eliminar y seleccionar.
 *
 * <p><b>GRASP Creator:</b> registra los perfiles, por eso crea las instancias
 * de {@link Profile}. <b>SRP:</b> delega el sistema de archivos en
 * {@link ProfileDirectory}, el archivo de configuración en
 * {@link GlobalConfigService} y la construcción de servicios en
 * {@link ProfileServicesFactory}.</p>
 */
public class ProfileService {

    private final Repository<Profile> perfiles;
    private final GlobalConfigService configuracion;
    private final ProfileDirectory directorios;
    private final ProfileServicesFactory fabricaServicios;

    public ProfileService(Repository<Profile> perfiles,
                          GlobalConfigService configuracion,
                          ProfileDirectory directorios,
                          ProfileServicesFactory fabricaServicios) {
        this.perfiles = Objects.requireNonNull(perfiles);
        this.configuracion = Objects.requireNonNull(configuracion);
        this.directorios = Objects.requireNonNull(directorios);
        this.fabricaServicios = Objects.requireNonNull(fabricaServicios);
    }

    public List<Profile> listarTodos() {
        return perfiles.listarTodos();
    }

    public Optional<Profile> buscarPorId(String id) {
        return perfiles.buscarPorId(id);
    }

    /** @return el perfil activo, si existe */
    public Optional<Profile> getPerfilActivo() {
        return configuracion.getPerfilActivoId().flatMap(perfiles::buscarPorId);
    }

    /** @return {@code true} si aún no se ha creado ningún perfil */
    public boolean esPrimerArranque() {
        return configuracion.esPrimerArranque();
    }

    /**
     * Determina qué perfil cargar al iniciar: el activo guardado o, si no hay,
     * el primero de la lista (comportamiento de P2).
     *
     * @return ID del perfil a cargar, o vacío si no hay perfiles
     */
    public Optional<String> resolverPerfilInicial() {
        Optional<String> activo = configuracion.getPerfilActivoId()
                .filter(id -> perfiles.buscarPorId(id).filter(Profile::isActivo).isPresent());
        if (activo.isPresent()) {
            return activo;
        }
        Optional<String> primero = perfiles.listarTodos().stream().filter(Profile::isActivo)
                .map(Profile::getId).findFirst();
        primero.ifPresent(configuracion::establecerPerfilActivo);
        return primero;
    }

    /**
     * <b>Creator:</b> crea el perfil, su carpeta y, si el tipo de negocio lo
     * indica, sus categorías predefinidas.
     *
     * @param datos datos enviados por la vista (nombre, descripción, color, tipo)
     * @return el perfil creado
     * @throws ValidationException si el nombre está vacío
     */
    public Profile crear(Profile datos) {
        if (datos == null) {
            throw new ValidationException("No se recibieron datos del perfil");
        }
        Profile perfil = new Profile(
                perfiles.siguienteIdNumerico(),
                TextUtil.recortar(datos.getNombre()),
                TextUtil.recortar(datos.getDescripcion()),
                datos.getColor(),
                datos.getTipoNegocio());
        perfil.copiarAjustesDePago(datos);
        perfil.copiarDatosComprobante(datos);
        if (BusinessCapabilities.estanGuardadas(datos.getConfiguracion())) {
            perfil.setCapacidades(perfil.capacidadesPedidas(datos));
        }
        perfil.validar();
        perfiles.agregar(perfil);
        directorios.crear(perfil.getId());
        crearCategoriasIniciales(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    private void crearCategoriasIniciales(Profile perfil) {
        BusinessType tipo = perfil.getPerfilNegocio();
        if (tipo.tieneCategorias()) {
            fabricaServicios.crear(perfil.getId()).categorias().crearPredefinidas(tipo);
        }
    }

    /**
     * Actualiza nombre, descripción y color; conserva el resto de datos.
     *
     * @param cambios datos enviados por la vista (debe incluir el ID)
     * @return el perfil actualizado
     * @throws ValidationException si el perfil no existe o el nombre está vacío
     */
    public Profile actualizar(Profile cambios) {
        Profile perfil = obtener(cambios == null ? null : cambios.getId());
        BusinessCapabilities pedidas = perfil.capacidadesPedidas(cambios);
        if (BusinessCapabilities.estanGuardadas(cambios.getConfiguracion())) {
            validarCapacidadesEnUso(perfil, pedidas);
        }
        perfil.actualizarDatos(cambios.getNombre(), cambios.getDescripcion(), cambios.getColor());
        perfil.copiarAjustesDePago(cambios);
        perfil.copiarDatosComprobante(cambios);
        if (BusinessCapabilities.estanGuardadas(cambios.getConfiguracion())) {
            perfil.setCapacidades(pedidas);
        }
        perfiles.actualizar(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    /**
     * Guarda el margen y el redondeo con que el negocio fija sus precios (v3.8.0).
     *
     * @param id       ID del negocio
     * @param politica margen y redondeo elegidos
     * @return el negocio actualizado
     */
    public Profile guardarPoliticaPrecio(String id, sistemainventario.model.PoliticaPrecio politica) {
        Profile perfil = obtener(id);
        perfil.setPoliticaPrecio(politica);
        perfiles.actualizar(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    /**
     * Enciende o apaga las fotos de producto del negocio (v3.9.0). Apagarlas no borra las
     * que ya están guardadas.
     *
     * @param id  ID del negocio
     * @param usa {@code true} para usar fotos
     * @return el negocio actualizado
     */
    public Profile guardarUsoDeFotos(String id, boolean usa) {
        Profile perfil = obtener(id);
        perfil.setUsaFotos(usa);
        perfiles.actualizar(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    /**
     * No deja apagar una capacidad que los productos ya usan: primero hay que
     * cambiar esos productos (por ejemplo, de "Con tallas y colores" a "Simple").
     */
    private void validarCapacidadesEnUso(Profile perfil, BusinessCapabilities pedidas) {
        BusinessCapabilities actuales = perfil.getCapacidades();
        List<Product> productos = crearServicios(perfil.getId()).inventario().listarTodos();
        if (actuales.variantes() && !pedidas.variantes()) {
            long n = productos.stream().filter(Product::tieneVariantes).count();
            if (n > 0) {
                throw new ValidationException("No puede quitar tallas y colores: " + n
                        + " producto(s) las usan. Cámbielos a Simple primero.");
            }
        }
        if (actuales.fraccion() && !pedidas.fraccion()) {
            long n = productos.stream().filter(p -> p.getUnidadMedida() != null
                    && p.getUnidadMedida().isFraccionable()).count();
            if (n > 0) {
                throw new ValidationException("No puede quitar la venta fraccionada: " + n
                        + " producto(s) se miden en metros, kilos o litros. Cambie su unidad primero.");
            }
        }
        if (actuales.vencimiento() != ControlVencimiento.NINGUNO
                && pedidas.vencimiento() == ControlVencimiento.NINGUNO) {
            long n = productos.stream().filter(Product::isRequiereVencimiento).count();
            if (n > 0) {
                throw new ValidationException("No puede quitar el vencimiento: " + n
                        + " producto(s) tienen fecha de vencimiento. Quítela de esos productos primero.");
            }
        }
    }

    /**
     * Deshabilita un negocio (borrado lógico): sus productos, ventas y demás
     * datos se conservan y se puede volver a habilitar. Si era el activo, se
     * activa otro negocio habilitado.
     *
     * @param id ID del perfil
     * @throws ValidationException si no existe o es el único habilitado
     */
    public void deshabilitar(String id) {
        Profile perfil = obtener(id);
        Optional<String> otro = perfiles.listarTodos().stream()
                .filter(p -> p.isActivo() && !p.getId().equals(id))
                .map(Profile::getId).findFirst();
        if (otro.isEmpty()) {
            throw new ValidationException("Debe quedar al menos un negocio habilitado");
        }
        perfil.setActivo(false);
        perfiles.actualizar(perfil);
        if (configuracion.getPerfilActivoId().filter(id::equals).isPresent()) {
            configuracion.establecerPerfilActivo(otro.get());
        }
        sincronizarConfiguracion();
    }

    /**
     * Vuelve a habilitar un negocio deshabilitado.
     *
     * @param id ID del perfil
     * @throws ValidationException si el perfil no existe
     */
    public void habilitar(String id) {
        Profile perfil = obtener(id);
        perfil.setActivo(true);
        perfiles.actualizar(perfil);
        sincronizarConfiguracion();
    }

    /**
     * @param id ID del perfil a activar, o vacío/{@code null} para ninguno
     * @return servicios del perfil activado, o vacío si se desactivó
     * @throws ValidationException si el perfil no existe
     */
    public Optional<ProfileServices> seleccionar(String id) {
        if (TextUtil.esVacio(id)) {
            configuracion.establecerPerfilActivo(null);
            return Optional.empty();
        }
        if (!obtener(id).isActivo()) {
            throw new ValidationException("Ese negocio está deshabilitado. Habilítelo en Mis negocios.");
        }
        configuracion.establecerPerfilActivo(id);
        return Optional.of(fabricaServicios.crear(id));
    }

    /**
     * @param id ID del perfil
     * @return servicios para operar sobre los datos de ese perfil
     */
    public ProfileServices crearServicios(String id) {
        return fabricaServicios.crear(id);
    }

    /**
     * Revisa la categoría de los productos de todos los negocios (los datos de
     * versiones en que era opcional quedan en "Sin categoría").
     *
     * @return cuántos productos se corrigieron en total
     */
    public int normalizarCategorias() {
        return perfiles.listarTodos().stream()
                .mapToInt(perfil -> fabricaServicios.normalizarCategorias(perfil.getId())).sum();
    }

    private Profile obtener(String id) {
        return perfiles.buscarPorId(id)
                .orElseThrow(() -> new ValidationException("Perfil no encontrado"));
    }

    /**
     * Copia la lista de perfiles al archivo de configuración. Al abrir una base
     * de datos que ya tiene negocios, evita que TBalance crea que es la primera vez.
     */
    public void sincronizarConfiguracion() {
        configuracion.sincronizarPerfiles(perfiles.listarTodos());
    }
}
