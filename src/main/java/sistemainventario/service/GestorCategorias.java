package sistemainventario.service;

import sistemainventario.model.Categoria;
import sistemainventario.repository.CategoriaRepository;
import sistemainventario.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

public class GestorCategorias {

    private final CategoriaRepository repo;

    public GestorCategorias(CategoriaRepository repo) {
        this.repo = repo;
    }

    public List<Categoria> listarTodos() {
        return repo.listarTodos();
    }

    public Categoria buscarPorId(String id) {
        return repo.buscarPorId(id).orElse(null);
    }

    public Categoria buscarPorNombre(String nombre) {
        if (nombre == null) return null;
        return repo.filtrar(c -> nombre.equalsIgnoreCase(c.getNombre())).stream().findFirst().orElse(null);
    }

    public boolean agregar(Categoria c) {
        if (c == null || repo.existe(c.getId())) return false;
        if (c.getId() == null || c.getId().isBlank())
            c.setId("CAT-" + System.currentTimeMillis());
        repo.agregar(c);
        return true;
    }

    public boolean actualizar(Categoria c) {
        return repo.actualizar(c);
    }

    public boolean eliminar(String id) {
        return repo.eliminar(id);
    }
}
