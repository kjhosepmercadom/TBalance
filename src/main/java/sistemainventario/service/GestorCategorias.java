package sistemainventario.service;

import sistemainventario.model.Categoria;
import sistemainventario.model.PerfilNegocio;
import sistemainventario.repository.Repository;
import sistemainventario.util.IdGenerator;

import java.util.List;

/**
 * Casos de uso de las categorías de un perfil.
 *
 * <p><b>GRASP Creator:</b> es quien agrega y registra las categorías, por eso
 * crea las categorías predefinidas de un tipo de negocio
 * ({@link #crearPredefinidas(PerfilNegocio)}).</p>
 */
public class GestorCategorias extends GestorEntidad<Categoria> {

    /** Prefijo que P2 ya usaba en {@code categorias.json}. */
    public static final String PREFIJO_ID = "CAT";

    public GestorCategorias(Repository<Categoria> repositorio) {
        super(repositorio);
        asignarIdsFaltantes();
    }

    /**
     * Migración de datos de P2: las categorías predefinidas se guardaban con
     * {@code id = null}, lo que impedía editarlas o eliminarlas. Al cargar el
     * perfil se les asigna un ID único.
     */
    private void asignarIdsFaltantes() {
        List<Categoria> sinId = repositorio.filtrar(categoria -> !categoria.tieneId());
        sinId.stream().map(Categoria::getId).distinct().forEach(repositorio::eliminar);
        sinId.forEach(categoria -> {
            categoria.setId(generarId());
            repositorio.agregar(categoria);
        });
    }

    @Override
    protected void validar(Categoria categoria) {
        categoria.validar();
    }

    @Override
    protected String generarId() {
        return IdGenerator.conPrefijo(PREFIJO_ID);
    }

    @Override
    protected String nombreEntidad() {
        return "Categoria";
    }

    /**
     * <b>Creator:</b> crea las categorías iniciales de un tipo de negocio.
     * En P2 se guardaban sin ID; ahora cada una recibe un ID único.
     *
     * @param tipoNegocio tipo elegido al crear el perfil
     */
    public void crearPredefinidas(PerfilNegocio tipoNegocio) {
        String descripcion = "Categoria predefinida para " + tipoNegocio.getEtiqueta();
        for (String nombre : tipoNegocio.getCategoriasPredefinidas()) {
            guardar(new Categoria(null, nombre, descripcion));
        }
    }
}
