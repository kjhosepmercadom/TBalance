package sistemainventario.repository;

import sistemainventario.model.Producto;
import java.nio.file.Path;

public class ProductoRepository extends JsonRepository<Producto> {

    public ProductoRepository(Path dirBase) {
        super(dirBase, "productos.json", Producto.class);
    }
}
