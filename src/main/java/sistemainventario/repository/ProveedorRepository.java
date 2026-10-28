package sistemainventario.repository;

import sistemainventario.model.Proveedor;
import java.nio.file.Path;

public class ProveedorRepository extends JsonRepository<Proveedor> {

    public ProveedorRepository(Path dirBase) {
        super(dirBase, "proveedores.json", Proveedor.class);
    }
}
