package sistemainventario.repository;

import sistemainventario.model.Entrada;
import java.nio.file.Path;

public class EntradaRepository extends JsonRepository<Entrada> {

    public EntradaRepository(Path dirBase) {
        super(dirBase, "entradas.json", Entrada.class);
    }
}
