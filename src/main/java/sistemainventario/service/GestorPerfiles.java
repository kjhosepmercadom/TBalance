package sistemainventario.service;

import sistemainventario.model.Categoria;
import sistemainventario.model.Perfil;
import sistemainventario.model.PerfilNegocio;
import sistemainventario.repository.CategoriaRepository;
import sistemainventario.repository.ConfiguracionGlobalRepository;
import sistemainventario.repository.PerfilRepository;
import sistemainventario.util.IdGenerator;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio central para la administración de perfiles de trabajo.
 *
 * Responsabilidad única (SRP): crear, editar, eliminar, seleccionar
 * perfiles y mantener la lista persistente.
 *
 * Los perfiles son independientes: cada uno tiene su propio subdirectorio
 * dentro de {@code ~/.inventariopro/data/perfiles/{id}/} donde se almacenan
 * sus datos (productos, categorías, ventas, etc.) de forma aislada.
 *
 * Opcionalmente, al crear un perfil el usuario puede indicar un
 * {@link PerfilNegocio} y el sistema creará automáticamente las categorías
 * predefinidas asociadas en el subdirectorio del nuevo perfil.
 */
public class GestorPerfiles {

    private final ConfiguracionGlobalRepository configRepo;
    private final PerfilRepository              perfilRepo;
    private final Path dirBase;

    public GestorPerfiles(Path dirBase,
                          ConfiguracionGlobalRepository configRepo,
                          PerfilRepository perfilRepo) {
        this.dirBase    = dirBase;
        this.configRepo = configRepo;
        this.perfilRepo = perfilRepo;
    }

    // ── LISTAR / BUSCAR ────────────────────────────────────────────────
    public List<Perfil> listarTodos() {
        return perfilRepo.listarTodos();
    }

    public Perfil buscarPorId(String id) {
        return perfilRepo.buscarPorId(id).orElse(null);
    }

    public Perfil getPerfilActivo() {
        return configRepo.cargar() != null
                ? configRepo.cargar().getPerfilActivo()
                : null;
    }

    public String getPerfilActivoId() {
        return configRepo.cargar() != null
                ? configRepo.cargar().getPerfilActivoId()
                : null;
    }

    // ── CREAR ─────────────────────────────────────────────────────────
    public Perfil crear(String nombre, String descripcion,
                        String color, String tipoNegocio) {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre del perfil es obligatorio");

        Perfil p = new Perfil(
                IdGenerator.siguienteNumerico(
                        perfilRepo.listarTodos().stream()
                                .map(Perfil::getId)
                                .collect(Collectors.toList())),
                nombre.trim(),
                descripcion != null ? descripcion.trim() : null,
                color,
                tipoNegocio
        );
        perfilRepo.agregar(p);
        crearDirectorioPerfil(p.getId());

        // Si el usuario eligió un tipo de negocio con categorías predefinidas,
        // las creamos en el subdirectorio del nuevo perfil.
        PerfilNegocio tipo = PerfilNegocio.desdeClave(tipoNegocio);
        if (tipo.tieneCategorias()) {
            crearCategoriasPredefinidas(p.getId(), tipo);
        }

        return p;
    }

    private void crearCategoriasPredefinidas(String perfilId, PerfilNegocio tipo) {
        CategoriaRepository catRepo = new CategoriaRepository(getDirPerfil(perfilId));
        for (String nombreCat : tipo.getCategoriasPredefinidas()) {
            catRepo.agregar(new Categoria(
                    null,
                    nombreCat,
                    "Categoria predefinida para " + tipo.getEtiqueta()
            ));
        }
    }

    // ── EDITAR ────────────────────────────────────────────────────────
    public boolean actualizar(Perfil perfil) {
        return perfilRepo.actualizar(perfil);
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────
    public boolean eliminar(String id) {
        if (id == null) return false;
        if (!perfilRepo.existe(id)) return false;
        boolean ok = perfilRepo.eliminar(id);
        if (ok) {
            eliminarDirectorioPerfil(id);
            if (id.equals(getPerfilActivoId())) {
                seleccionar(null);
            }
        }
        return ok;
    }

    // ── SELECCIONAR / CAMBIAR ─────────────────────────────────────────
    public void seleccionar(String perfilId) {
        var cfg = configRepo.cargar();
        if (cfg == null) return;
        cfg.setPerfilActivoId(perfilId);
        configRepo.guardar(cfg);
    }

    // ── PERSISTIR LISTA DE PERFILES EN CONFIG ────────────────────────
    public void sincronizarConfiguracion() {
        var cfg = configRepo.cargar();
        if (cfg == null) {
            cfg = new sistemainventario.model.ConfiguracionGlobal();
        }
        cfg.setPerfiles(perfilRepo.listarTodos());
        configRepo.guardar(cfg);
    }

    // ── UTILIDADES ────────────────────────────────────────────────────
    public Path getDirPerfil(String perfilId) {
        return dirBase.resolve("perfiles").resolve(perfilId);
    }

    private void crearDirectorioPerfil(String perfilId) {
        try {
            Files.createDirectories(getDirPerfil(perfilId));
        } catch (IOException e) {
            System.err.println("No se pudo crear directorio del perfil " + perfilId + ": " + e.getMessage());
        }
    }

    private void eliminarDirectorioPerfil(String perfilId) {
        Path dir = getDirPerfil(perfilId);
        if (!Files.exists(dir)) return;
        try {
            try (var stream = Files.walk(dir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); }
                            catch (IOException ignored) {}
                        });
            }
        } catch (IOException e) {
            System.err.println("No se pudo eliminar directorio del perfil " + perfilId);
        }
    }
}
