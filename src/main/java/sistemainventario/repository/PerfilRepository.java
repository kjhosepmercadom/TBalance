package sistemainventario.repository;

import sistemainventario.model.Perfil;

import java.nio.file.Path;

/**
 * Persistencia global de la lista de perfiles en {@code perfiles.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class PerfilRepository extends JsonRepository<Perfil> {

    public static final String ARCHIVO = "perfiles.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public PerfilRepository(Path directorio) {
        super(directorio, ARCHIVO, Perfil.class);
    }
}
