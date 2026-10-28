package sistemainventario.service;

import sistemainventario.exception.ValidacionException;
import sistemainventario.model.Perfil;
import sistemainventario.model.PerfilNegocio;
import sistemainventario.repository.DirectorioPerfiles;
import sistemainventario.repository.Repository;
import sistemainventario.util.TextoUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ciclo de vida de los perfiles: crear, editar, eliminar y seleccionar.
 *
 * <p><b>GRASP Creator:</b> registra los perfiles, por eso crea las instancias
 * de {@link Perfil}. <b>SRP:</b> delega el sistema de archivos en
 * {@link DirectorioPerfiles}, el archivo de configuración en
 * {@link GestorConfiguracionGlobal} y la construcción de servicios en
 * {@link FabricaServiciosPerfil}.</p>
 */
public class GestorPerfiles {

    private final Repository<Perfil> perfiles;
    private final GestorConfiguracionGlobal configuracion;
    private final DirectorioPerfiles directorios;
    private final FabricaServiciosPerfil fabricaServicios;

    public GestorPerfiles(Repository<Perfil> perfiles,
                          GestorConfiguracionGlobal configuracion,
                          DirectorioPerfiles directorios,
                          FabricaServiciosPerfil fabricaServicios) {
        this.perfiles = Objects.requireNonNull(perfiles);
        this.configuracion = Objects.requireNonNull(configuracion);
        this.directorios = Objects.requireNonNull(directorios);
        this.fabricaServicios = Objects.requireNonNull(fabricaServicios);
    }

    public List<Perfil> listarTodos() {
        return perfiles.listarTodos();
    }

    public Optional<Perfil> buscarPorId(String id) {
        return perfiles.buscarPorId(id);
    }

    /** @return el perfil activo, si existe */
    public Optional<Perfil> getPerfilActivo() {
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
        Optional<String> activo = configuracion.getPerfilActivoId().filter(perfiles::existe);
        if (activo.isPresent()) {
            return activo;
        }
        Optional<String> primero = perfiles.listarTodos().stream().map(Perfil::getId).findFirst();
        primero.ifPresent(configuracion::establecerPerfilActivo);
        return primero;
    }

    /**
     * <b>Creator:</b> crea el perfil, su carpeta y, si el tipo de negocio lo
     * indica, sus categorías predefinidas.
     *
     * @param datos datos enviados por la vista (nombre, descripción, color, tipo)
     * @return el perfil creado
     * @throws ValidacionException si el nombre está vacío
     */
    public Perfil crear(Perfil datos) {
        if (datos == null) {
            throw new ValidacionException("No se recibieron datos del perfil");
        }
        Perfil perfil = new Perfil(
                perfiles.siguienteIdNumerico(),
                TextoUtil.recortar(datos.getNombre()),
                TextoUtil.recortar(datos.getDescripcion()),
                datos.getColor(),
                datos.getTipoNegocio());
        perfil.validar();
        perfiles.agregar(perfil);
        directorios.crear(perfil.getId());
        crearCategoriasIniciales(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    private void crearCategoriasIniciales(Perfil perfil) {
        PerfilNegocio tipo = perfil.getPerfilNegocio();
        if (tipo.tieneCategorias()) {
            fabricaServicios.crear(perfil.getId()).categorias().crearPredefinidas(tipo);
        }
    }

    /**
     * Actualiza nombre, descripción y color; conserva el resto de datos.
     *
     * @param cambios datos enviados por la vista (debe incluir el ID)
     * @return el perfil actualizado
     * @throws ValidacionException si el perfil no existe o el nombre está vacío
     */
    public Perfil actualizar(Perfil cambios) {
        Perfil perfil = obtener(cambios == null ? null : cambios.getId());
        perfil.actualizarDatos(cambios.getNombre(), cambios.getDescripcion(), cambios.getColor());
        perfiles.actualizar(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    /**
     * Elimina el perfil y todos sus datos. Si era el activo, deja el sistema
     * sin perfil activo.
     *
     * @param id ID del perfil
     * @throws ValidacionException si el perfil no existe
     */
    public void eliminar(String id) {
        obtener(id);
        perfiles.eliminar(id);
        directorios.eliminar(id);
        if (configuracion.getPerfilActivoId().filter(id::equals).isPresent()) {
            configuracion.establecerPerfilActivo(null);
        }
        sincronizarConfiguracion();
    }

    /**
     * @param id ID del perfil a activar, o vacío/{@code null} para ninguno
     * @return servicios del perfil activado, o vacío si se desactivó
     * @throws ValidacionException si el perfil no existe
     */
    public Optional<ServiciosPerfil> seleccionar(String id) {
        if (TextoUtil.esVacio(id)) {
            configuracion.establecerPerfilActivo(null);
            return Optional.empty();
        }
        obtener(id);
        configuracion.establecerPerfilActivo(id);
        return Optional.of(fabricaServicios.crear(id));
    }

    /**
     * @param id ID del perfil
     * @return servicios para operar sobre los datos de ese perfil
     */
    public ServiciosPerfil crearServicios(String id) {
        return fabricaServicios.crear(id);
    }

    private Perfil obtener(String id) {
        return perfiles.buscarPorId(id)
                .orElseThrow(() -> new ValidacionException("Perfil no encontrado"));
    }

    private void sincronizarConfiguracion() {
        configuracion.sincronizarPerfiles(perfiles.listarTodos());
    }
}
