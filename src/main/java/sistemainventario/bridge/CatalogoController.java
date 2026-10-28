package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Categoria;
import sistemainventario.model.Proveedor;
import sistemainventario.service.GestorCategorias;
import sistemainventario.service.GestorProveedores;
import sistemainventario.util.JsonUtil;

/**
 * <b>GRASP Controller</b> de los casos de uso de categorías y proveedores.
 */
public class CatalogoController {

    private static final String LISTA_VACIA = "[]";

    private final AppContext contexto;

    public CatalogoController(AppContext contexto) {
        this.contexto = contexto;
    }

    // ── Categorías ──────────────────────────────────────────────────────

    public String obtenerCategorias() {
        return contexto.getServiciosActivos()
                .map(servicios -> RespuestaBridge.json(servicios.categorias().listarTodos()))
                .orElse(LISTA_VACIA);
    }

    public String guardarCategoria(String jsonCategoria) {
        return RespuestaBridge.ejecutarOk(() ->
                categorias().guardar(JsonUtil.fromJson(jsonCategoria, Categoria.class)));
    }

    public String eliminarCategoria(String id) {
        return RespuestaBridge.ejecutar(() ->
                categorias().eliminar(id) ? RespuestaBridge.OK : RespuestaBridge.error("Categoria no encontrada"));
    }

    // ── Proveedores ─────────────────────────────────────────────────────

    public String obtenerProveedores() {
        return contexto.getServiciosActivos()
                .map(servicios -> RespuestaBridge.json(servicios.proveedores().listarTodos()))
                .orElse(LISTA_VACIA);
    }

    public String guardarProveedor(String jsonProveedor) {
        return RespuestaBridge.ejecutarOk(() ->
                proveedores().guardar(JsonUtil.fromJson(jsonProveedor, Proveedor.class)));
    }

    public String eliminarProveedor(String id) {
        return RespuestaBridge.ejecutar(() ->
                proveedores().eliminar(id) ? RespuestaBridge.OK : RespuestaBridge.error("Proveedor no encontrado"));
    }

    private GestorCategorias categorias() {
        return contexto.requerirServiciosActivos().categorias();
    }

    private GestorProveedores proveedores() {
        return contexto.requerirServiciosActivos().proveedores();
    }
}
