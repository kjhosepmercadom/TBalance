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
        return repo.buscarPorId(id);
    }

    public Categoria buscarPorNombre(String nombre) {
        return repo.buscarPorNombre(nombre);
    }

    public boolean agregar(Categoria c) {
        if (c == null || repo.buscarPorId(c.getId()) != null) return false;
        if (c.getId() == null || c.getId().isBlank())
            c.setId("CAT-" + System.currentTimeMillis());
        repo.agregar(c);
        return true;
    }

    public boolean actualizar(Categoria c) {
        return repo.actualizar(x -> x.getId().equals(c.getId()), c);
    }

    public boolean eliminar(String id) {
        return repo.eliminar(c -> c.getId().equals(id));
    }
}
