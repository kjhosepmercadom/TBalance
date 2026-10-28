package sistemainventario.repository;

import sistemainventario.model.Categoria;
import java.nio.file.Path;

public class CategoriaRepository extends JsonRepository<Categoria> {

    public CategoriaRepository(Path dirBase) {
        super(dirBase, "categorias.json", Categoria.class);
    }

    public Categoria buscarPorNombre(String nombre) {
        if (nombre == null) return null;
        return cache.stream()
                .filter(c -> nombre.equalsIgnoreCase(c.getNombre()))
                .findFirst().orElse(null);
    }
}
