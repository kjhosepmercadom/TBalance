package sistemainventario.repository;

import sistemainventario.model.Producto;

import java.nio.file.Path;

/**
 * Persistencia de los productos de un perfil en {@code productos.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class ProductoRepository extends JsonRepository<Producto> {

    public static final String ARCHIVO = "productos.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public ProductoRepository(Path directorio) {
        super(directorio, ARCHIVO, Producto.class);
    }
}
