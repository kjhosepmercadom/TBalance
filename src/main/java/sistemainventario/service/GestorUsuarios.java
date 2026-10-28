package sistemainventario.service;

import sistemainventario.model.Usuario;
import sistemainventario.repository.UsuarioRepository;
import sistemainventario.util.IdGenerator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class GestorUsuarios {

    private final UsuarioRepository repo;

    public GestorUsuarios(UsuarioRepository repo) {
        this.repo = repo;
    }

    public List<Usuario> listarTodos() {
        return repo.listarTodos();
    }

    public Usuario buscarPorId(String id) {
        return repo.buscarPorId(id);
    }

    public Usuario autenticar(String usuario, String password) {
        Usuario u = repo.buscarPorUsuario(usuario);
        if (u != null && u.isActivo() && u.getPasswordHash().equals(hash(password))) {
            return u;
        }
        return null;
    }

    public boolean agregar(Usuario u) {
        if (u == null || repo.buscarPorUsuario(u.getUsuario()) != null) return false;
        if (u.getId() == null || u.getId().isBlank())
            u.setId(IdGenerator.siguienteNumerico(
                    repo.listarTodos().stream().map(Usuario::getId).collect(Collectors.toList())));
        u.setPasswordHash(hash(u.getPasswordHash()));
        repo.agregar(u);
        return true;
    }

    public boolean actualizar(Usuario u) {
        return repo.actualizar(x -> x.getId().equals(u.getId()), u);
    }

    public boolean eliminar(String id) {
        return repo.eliminar(u -> u.getId().equals(id));
    }

    private String hash(String pwd) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pwd.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return pwd;
        }
    }
}
