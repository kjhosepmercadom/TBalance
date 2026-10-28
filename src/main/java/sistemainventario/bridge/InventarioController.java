package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Producto;
import sistemainventario.service.GestorInventario;
import sistemainventario.service.ServiciosPerfil;
import sistemainventario.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * <b>GRASP Controller</b> de los casos de uso de productos y del dashboard.
 */
public class InventarioController {

    private static final String LISTA_VACIA = "[]";

    private final AppContext contexto;

    public InventarioController(AppContext contexto) {
        this.contexto = contexto;
    }

    public String obtenerProductos() {
        return consultar(GestorInventario::listarTodos);
    }

    public String buscarProductos(String texto) {
        return consultar(inventario -> inventario.buscarPorTexto(texto));
    }

    public String filtrarProductosPorCategoria(String categoria) {
        return consultar(inventario -> inventario.filtrarPorCategoria(categoria));
    }

    public String obtenerProductosVencidos() {
        return consultar(GestorInventario::getVencidos);
    }

    public String obtenerProductosPorVencer(int dias) {
        return consultar(inventario -> inventario.getPorVencer(dias));
    }

    public String guardarProducto(String jsonProducto) {
        return RespuestaBridge.ejecutarOk(() ->
                inventario().guardar(JsonUtil.fromJson(jsonProducto, Producto.class)));
    }

    public String eliminarProducto(String id) {
        return RespuestaBridge.ejecutar(() ->
                inventario().eliminar(id) ? RespuestaBridge.OK : RespuestaBridge.error("Producto no encontrado"));
    }

    /**
     * @param incluirVencimientos si el perfil activo controla vencimientos
     * @return indicadores clave del perfil activo en JSON
     */
    public String obtenerEstadisticas(boolean incluirVencimientos) {
        return RespuestaBridge.ejecutar(() -> {
            ServiciosPerfil servicios = contexto.requerirServiciosActivos();
            GestorInventario inventario = servicios.inventario();
            Map<String, Object> estadisticas = new LinkedHashMap<>();
            estadisticas.put("totalProductos", inventario.listarTodos().size());
            estadisticas.put("bajoStock", inventario.getBajoStock().size());
            estadisticas.put("agotados", inventario.getAgotados().size());
            estadisticas.put("totalCategorias", servicios.categorias().listarTodos().size());
            estadisticas.put("totalProveedores", servicios.proveedores().listarTodos().size());
            estadisticas.put("totalVentas", servicios.ventas().getTotalVentas());
            estadisticas.put("totalTransacciones", servicios.ventas().getTotalTransacciones());
            if (incluirVencimientos) {
                estadisticas.put("vencidos", inventario.getVencidos().size());
                estadisticas.put("porVencer7", inventario.getPorVencer(7).size());
                estadisticas.put("porVencer30", inventario.getPorVencer(30).size());
            }
            return RespuestaBridge.json(estadisticas);
        });
    }

    private String consultar(Function<GestorInventario, List<Producto>> consulta) {
        return contexto.getServiciosActivos()
                .map(servicios -> RespuestaBridge.json(consulta.apply(servicios.inventario())))
                .orElse(LISTA_VACIA);
    }

    private GestorInventario inventario() {
        return contexto.requerirServiciosActivos().inventario();
    }
}
