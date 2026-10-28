package sistemainventario;

import sistemainventario.repository.*;
import sistemainventario.service.*;

import java.nio.file.*;

/**
 * Contexto central de la aplicación (Singleton).
 *
 * Encargado de inicializar y mantener todos los repositorios y servicios.
 *
 * Como cada perfil posee su propio espacio de datos, los repositorios de
 * negocio (productos, categorías, etc.) se reconstruyen cada vez que
 * se activa un perfil distinto. Esto se hace en {@link #cargarServiciosDePerfil(String)}.
 *
 * Aplica SRP: solo se ocupa del cableado (wiring) y el cambio dinámico
 * de perfil. La lógica de negocio reside en los Gestor* correspondientes.
 */
public class AppContext {

    private static AppContext INSTANCE;

    private final Path dirBase;

    // ── Repos GLOBALES (no asociados a un perfil) ─────────────────
    public final ConfiguracionGlobalRepository configuracionGlobalRepo;
    public final PerfilRepository              perfilRepo;

    // ── Servicios GLOBALES ────────────────────────────────────────
    public final GestorConfiguracionGlobal gestorConfiguracionGlobal;
    public final GestorPerfiles            gestorPerfiles;

    // ── Repos y servicios del PERFIL ACTIVO (recreados al cambiar) ──
    public ProductoRepository    productoRepo;
    public CategoriaRepository   categoriaRepo;
    public ProveedorRepository   proveedorRepo;
    public EntradaRepository     entradaRepo;
    public SalidaRepository      salidaRepo;
    public VentaRepository       ventaRepo;

    public GestorInventario   gestorInventario;
    public GestorCategorias   gestorCategorias;
    public GestorProveedores  gestorProveedores;
    public GestorMovimientos  gestorMovimientos;
    public GestorVentas       gestorVentas;

    private String perfilActivoId;

    private AppContext() {
        this.dirBase = Paths.get(System.getProperty("user.home"), ".inventariopro", "data");
        try { Files.createDirectories(dirBase); } catch (Exception ignored) {}

        this.configuracionGlobalRepo = new ConfiguracionGlobalRepository(dirBase);
        this.perfilRepo               = new PerfilRepository(dirBase);

        this.gestorConfiguracionGlobal = new GestorConfiguracionGlobal(configuracionGlobalRepo);
        this.gestorPerfiles            = new GestorPerfiles(dirBase, configuracionGlobalRepo, perfilRepo);

        // Determinar perfil activo
        var cfg = configuracionGlobalRepo.cargar();
        String idActivo = cfg != null ? cfg.getPerfilActivoId() : null;

        // Si no hay activo pero sí hay perfiles, usar el primero
        if (idActivo == null && !perfilRepo.listarTodos().isEmpty()) {
            idActivo = perfilRepo.listarTodos().get(0).getId();
            if (cfg == null) cfg = new sistemainventario.model.ConfiguracionGlobal();
            cfg.setPerfilActivoId(idActivo);
            configuracionGlobalRepo.guardar(cfg);
        }

        this.perfilActivoId = idActivo;

        if (idActivo != null) {
            cargarServiciosDePerfil(idActivo);
        }
    }

    /**
     * Recrea los repos y servicios del perfil activo.
     * Se invoca al cambiar de perfil para garantizar aislamiento total.
     */
    public void cargarServiciosDePerfil(String perfilId) {
        this.perfilActivoId = perfilId;
        Path dirPerfil = dirBase.resolve("perfiles").resolve(perfilId);
        try { Files.createDirectories(dirPerfil); } catch (Exception ignored) {}

        productoRepo   = new ProductoRepository(dirPerfil);
        categoriaRepo  = new CategoriaRepository(dirPerfil);
        proveedorRepo  = new ProveedorRepository(dirPerfil);
        entradaRepo    = new EntradaRepository(dirPerfil);
        salidaRepo     = new SalidaRepository(dirPerfil);
        ventaRepo      = new VentaRepository(dirPerfil);

        gestorInventario  = new GestorInventario(productoRepo);
        gestorCategorias  = new GestorCategorias(categoriaRepo);
        gestorProveedores = new GestorProveedores(proveedorRepo);
        gestorMovimientos = new GestorMovimientos(gestorInventario, dirPerfil);
        gestorVentas      = new GestorVentas(gestorInventario, gestorMovimientos, dirPerfil);
    }

    public String getPerfilActivoId() { return perfilActivoId; }

    public Path getDirBase() { return dirBase; }

    public boolean esPrimerArranque() {
        return gestorConfiguracionGlobal.esPrimerArranque();
    }

    public static AppContext get() {
        if (INSTANCE == null) INSTANCE = new AppContext();
        return INSTANCE;
    }
}
