package sistemainventario.repository;

import sistemainventario.model.Venta;
import java.nio.file.Path;

public class VentaRepository extends JsonRepository<Venta> {

    public VentaRepository(Path dirBase) {
        super(dirBase, "ventas.json", Venta.class);
    }
}
