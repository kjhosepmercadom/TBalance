package sistemainventario.repository;

import sistemainventario.model.Salida;

import java.nio.file.Path;

/**
 * Persistencia de las salidas de inventario de un perfil en {@code salidas.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class SalidaRepository extends JsonRepository<Salida> {

    public static final String ARCHIVO = "salidas.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public SalidaRepository(Path directorio) {
        super(directorio, ARCHIVO, Salida.class);
    }
}
