package sistemainventario;

import sistemainventario.exception.ValidacionException;
import sistemainventario.repository.ConfiguracionGlobalRepository;
import sistemainventario.repository.DirectorioPerfiles;
import sistemainventario.repository.JsonFileStore;
import sistemainventario.repository.PerfilRepository;
import sistemainventario.service.FabricaServiciosPerfil;
import sistemainventario.service.GestorConfiguracionGlobal;
import sistemainventario.service.GestorPerfiles;
import sistemainventario.service.ServiciosPerfil;

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
 * archivo.</p>
 */
public final class AppContext {

    private static final Path DIRECTORIO_DATOS =
            Paths.get(System.getProperty("user.home"), ".inventariopro", "data");

    private static AppContext instancia;

    private final GestorPerfiles gestorPerfiles;
    private ServiciosPerfil serviciosActivos;

    private AppContext(Path directorioDatos) {
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
