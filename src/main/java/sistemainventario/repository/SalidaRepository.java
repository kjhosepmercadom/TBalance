package sistemainventario.repository;

import sistemainventario.model.Salida;
import java.nio.file.Path;

public class SalidaRepository extends JsonRepository<Salida> {

    public SalidaRepository(Path dirBase) {
        super(dirBase, "salidas.json", Salida.class);
    }
}
