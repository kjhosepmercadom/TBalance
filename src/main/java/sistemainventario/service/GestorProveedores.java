package sistemainventario.service;

import sistemainventario.model.Proveedor;
import sistemainventario.repository.ProveedorRepository;
import sistemainventario.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

public class GestorProveedores {

    private final ProveedorRepository repo;

    public GestorProveedores(ProveedorRepository repo) {
        this.repo = repo;
    }

    public List<Proveedor> listarTodos() {
        return repo.listarTodos();
    }

    public Proveedor buscarPorId(String id) {
        return repo.buscarPorId(id);
    }

    public boolean agregar(Proveedor p) {
        if (p == null || repo.buscarPorId(p.getId()) != null) return false;
        if (p.getId() == null || p.getId().isBlank())
            p.setId(IdGenerator.siguienteNumerico(
                    repo.listarTodos().stream().map(Proveedor::getId).collect(Collectors.toList())));
        repo.agregar(p);
        return true;
    }

    public boolean actualizar(Proveedor p) {
        return repo.actualizar(x -> x.getId().equals(p.getId()), p);
    }

    public boolean eliminar(String id) {
        return repo.eliminar(p -> p.getId().equals(id));
    }

    public List<Proveedor> buscar(String texto) {
        String q = texto.toLowerCase().trim();
        return repo.filtrar(p ->
                p.getNombre().toLowerCase().contains(q) ||
                (p.getContacto() != null && p.getContacto().toLowerCase().contains(q)));
    }

    public String generarNuevoId() {
        return IdGenerator.siguienteNumerico(
                repo.listarTodos().stream().map(Proveedor::getId).collect(Collectors.toList()));
    }
}
