package sistemainventario.repository;

import sistemainventario.model.Perfil;
import java.nio.file.Path;

public class PerfilRepository extends JsonRepository<Perfil> {

    public PerfilRepository(Path dirBase) {
        super(dirBase, "perfiles.json", Perfil.class);
    }
}
