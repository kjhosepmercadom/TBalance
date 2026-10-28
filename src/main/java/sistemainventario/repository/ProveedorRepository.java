package sistemainventario.repository;

import sistemainventario.model.Proveedor;

import java.nio.file.Path;

/**
 * Persistencia de los proveedores de un perfil en {@code proveedores.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class ProveedorRepository extends JsonRepository<Proveedor> {

    public static final String ARCHIVO = "proveedores.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public ProveedorRepository(Path directorio) {
        super(directorio, ARCHIVO, Proveedor.class);
    }
}
