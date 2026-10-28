package sistemainventario.repository;

import sistemainventario.model.EntidadConId;
import sistemainventario.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Repositorio genérico con persistencia en JSON.
 *
 * Aplica POLIMORFISMO: trabaja con cualquier subclase de {@link EntidadConId}.
 * Cada subclase concreta es responsable de definir su propio ID y comportamientos
 * específicos (su clase de modelo).
 *
 * Aplica SRP: solo gestiona CRUD + persistencia. La lógica de negocio
 * reside en los servicios (Gestor*).
 */
public abstract class JsonRepository<T extends EntidadConId> {

    protected final Path archivo;
    protected final Class<T> tipo;
    protected List<T> cache;

    protected JsonRepository(Path dirBase, String nombreArchivo, Class<T> tipo) {
        this.archivo = dirBase.resolve(nombreArchivo);
        this.tipo    = tipo;
        crearDirectorio(dirBase);
        this.cache   = cargar();
    }

    public List<T> listarTodos() {
        return new ArrayList<>(cache);
    }

    public void guardarTodos(List<T> lista) {
        this.cache = new ArrayList<>(lista);
        persistir();
    }

    public void agregar(T entidad) {
        cache.add(entidad);
        persistir();
    }

    public boolean eliminar(Predicate<T> condicion) {
        boolean removido = cache.removeIf(condicion);
        if (removido) persistir();
        return removido;
    }

    public boolean actualizar(Predicate<T> condicion, T nueva) {
        for (int i = 0; i < cache.size(); i++) {
            if (condicion.test(cache.get(i))) {
                cache.set(i, nueva);
                persistir();
                return true;
            }
        }
        return false;
    }

    public List<T> filtrar(Predicate<T> condicion) {
        return cache.stream().filter(condicion).collect(Collectors.toList());
    }

    public T buscarPorId(String id) {
        if (id == null) return null;
        return cache.stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst().orElse(null);
    }

    public boolean idExiste(String id) {
        return buscarPorId(id) != null;
    }

    protected List<T> cargar() {
        if (!Files.exists(archivo)) return new ArrayList<>();
        try {
            String json = Files.readString(archivo, StandardCharsets.UTF_8);
            return JsonUtil.fromJsonList(json, tipo);
        } catch (IOException e) {
            System.err.println("[JsonRepository] Error cargando " + archivo + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    protected void persistir() {
        try {
            Files.writeString(archivo,
                    JsonUtil.toJson(cache),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[JsonRepository] Error guardando " + archivo + ": " + e.getMessage());
        }
    }

    private void crearDirectorio(Path dir) {
        try { Files.createDirectories(dir); }
        catch (IOException e) { System.err.println("No se pudo crear: " + dir); }
    }

    public Path getArchivo() { return archivo; }
}
