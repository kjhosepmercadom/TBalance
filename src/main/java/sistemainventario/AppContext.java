package sistemainventario;

import sistemainventario.exception.ValidacionException;
import sistemainventario.model.Respaldo;
import sistemainventario.repository.ArchivadorZip;
import sistemainventario.repository.ConfiguracionGlobalRepository;
import sistemainventario.repository.DirectorioPerfiles;
import sistemainventario.repository.EtiquetaRepository;
import sistemainventario.repository.JsonFileStore;
import sistemainventario.repository.PerfilRepository;
import sistemainventario.repository.RespaldoRepository;
import sistemainventario.service.FabricaServiciosPerfil;
import sistemainventario.service.GestorConfiguracionGlobal;
import sistemainventario.service.GestorEtiquetas;
import sistemainventario.service.GestorPerfiles;
import sistemainventario.service.GestorRespaldo;
import sistemainventario.service.ServiciosPerfil;
import sistemainventario.util.GeneradorQR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Contexto de la aplicación (Singleton) y raíz de composición.
 *
 * <p>Es el único lugar donde se conectan las implementaciones concretas
 * (repositorios JSON) con los servicios. Mantiene los servicios del perfil
 * activo y los reemplaza al cambiar de perfil.</p>
 *
 * <p>Cambios respecto a P2: los campos dejaron de ser públicos y mutables
 * (encapsulamiento) y ya no se crean repositorios duplicados para el mismo
 * archivo. Después de restaurar una copia de seguridad, {@link #recargar()}
 * vuelve a leer todos los datos desde disco.</p>
 */
public final class AppContext {

    private static final Path DIRECTORIO_DATOS =
            Paths.get(System.getProperty("user.home"), ".inventariopro", "data");

    private static final String CARPETA_RESPALDOS = "respaldos";
    private static final String CARPETA_ETIQUETAS = "etiquetas";

    private static AppContext instancia;

    private final Path directorioDatos;
    private final GestorRespaldo gestorRespaldo;
    private final GestorEtiquetas gestorEtiquetas;
    private GestorPerfiles gestorPerfiles;
    private ServiciosPerfil serviciosActivos;

    private AppContext(Path directorioDatos) {
        this.directorioDatos = directorioDatos;
        JsonFileStore.crearDirectorio(directorioDatos);
        Path carpetaRespaldos = directorioDatos.resolveSibling(CARPETA_RESPALDOS);
        this.gestorRespaldo = new GestorRespaldo(directorioDatos,
                new RespaldoRepository(carpetaRespaldos), new ArchivadorZip());
        this.gestorEtiquetas = new GestorEtiquetas(new GeneradorQR(),
                new EtiquetaRepository(directorioDatos.resolveSibling(CARPETA_ETIQUETAS)));
        recargar();
    }

    /**
     * Construye de nuevo los repositorios y servicios leyendo los archivos
     * JSON. Se usa al iniciar y después de restaurar una copia de seguridad.
     */
    public void recargar() {
        JsonFileStore.crearDirectorio(directorioDatos);
        DirectorioPerfiles directorios = new DirectorioPerfiles(directorioDatos);
        this.gestorPerfiles = new GestorPerfiles(
                new PerfilRepository(directorioDatos),
                new GestorConfiguracionGlobal(new ConfiguracionGlobalRepository(directorioDatos)),
                directorios,
                new FabricaServiciosPerfil(directorios));
        this.serviciosActivos = gestorPerfiles.resolverPerfilInicial()
                .map(gestorPerfiles::crearServicios)
                .orElse(null);
    }

    /**
     * Restaura una copia de seguridad y recarga todos los datos en memoria.
     *
     * @param nombre archivo .zip a restaurar
     * @return la copia automática del estado anterior
     */
    public Respaldo restaurarRespaldo(String nombre) {
        Respaldo copiaPrevia = gestorRespaldo.restaurar(nombre);
        recargar();
        return copiaPrevia;
    }

    public GestorRespaldo getGestorRespaldo() {
        return gestorRespaldo;
    }

    public GestorEtiquetas getGestorEtiquetas() {
        return gestorEtiquetas;
    }

    /** @return la instancia única, creada la primera vez que se solicita */
    public static synchronized AppContext get() {
        if (instancia == null) {
            instancia = new AppContext(DIRECTORIO_DATOS);
        }
        return instancia;
    }

    public GestorPerfiles getGestorPerfiles() {
        return gestorPerfiles;
    }

    /** @return servicios del perfil activo, si lo hay */
    public Optional<ServiciosPerfil> getServiciosActivos() {
        return Optional.ofNullable(serviciosActivos);
    }

    /**
     * @return servicios del perfil activo
     * @throws ValidacionException si no hay perfil activo
     */
    public ServiciosPerfil requerirServiciosActivos() {
        return getServiciosActivos()
                .orElseThrow(() -> new ValidacionException("No hay un perfil activo"));
    }

    /**
     * Activa un perfil y reconstruye sus servicios (aislamiento de datos).
     *
     * @param perfilId ID del perfil, o vacío para no dejar ninguno activo
     */
    public void activarPerfil(String perfilId) {
        serviciosActivos = gestorPerfiles.seleccionar(perfilId).orElse(null);
    }

    /**
     * Elimina un perfil y libera sus servicios si era el activo.
     *
     * @param perfilId ID del perfil a eliminar
     */
    public void eliminarPerfil(String perfilId) {
        gestorPerfiles.eliminar(perfilId);
        if (gestorPerfiles.getPerfilActivo().isEmpty()) {
            serviciosActivos = null;
        }
    }

    /** @return {@code true} si todavía no existe ningún perfil */
    public boolean esPrimerArranque() {
        return gestorPerfiles.esPrimerArranque();
    }
}
