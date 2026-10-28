package sistemainventario.repository;

import sistemainventario.model.Usuario;
import java.nio.file.Path;

public class UsuarioRepository extends JsonRepository<Usuario> {

    public UsuarioRepository(Path dirBase) {
        super(dirBase, "usuarios.json", Usuario.class);
    }

    public Usuario buscarPorUsuario(String usuario) {
        return cache.stream()
                .filter(u -> u.getUsuario().equalsIgnoreCase(usuario))
                .findFirst().orElse(null);
    }
}
