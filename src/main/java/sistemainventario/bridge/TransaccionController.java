package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Entrada;
import sistemainventario.model.Salida;
import sistemainventario.model.Venta;
import sistemainventario.service.GestorMovimientos;
import sistemainventario.service.GestorVentas;
import sistemainventario.util.JsonUtil;

/**
 * <b>GRASP Controller</b> de los casos de uso que mueven stock:
 * entradas, salidas y ventas.
 */
public class TransaccionController {

    private static final String LISTA_VACIA = "[]";

    private final AppContext contexto;

    public TransaccionController(AppContext contexto) {
        this.contexto = contexto;
    }

    public String obtenerEntradas() {
        return contexto.getServiciosActivos()
                .map(servicios -> RespuestaBridge.json(servicios.movimientos().listarEntradas()))
                .orElse(LISTA_VACIA);
    }

    public String registrarEntrada(String jsonEntrada) {
        return RespuestaBridge.ejecutarOk(() ->
                movimientos().registrarEntrada(JsonUtil.fromJson(jsonEntrada, Entrada.class)));
    }

    public String obtenerSalidas() {
        return contexto.getServiciosActivos()
                .map(servicios -> RespuestaBridge.json(servicios.movimientos().listarSalidas()))
                .orElse(LISTA_VACIA);
    }

    public String registrarSalida(String jsonSalida) {
        return RespuestaBridge.ejecutarOk(() ->
                movimientos().registrarSalida(JsonUtil.fromJson(jsonSalida, Salida.class)));
    }

    public String obtenerVentas() {
        return contexto.getServiciosActivos()
                .map(servicios -> RespuestaBridge.json(servicios.ventas().listarTodas()))
                .orElse(LISTA_VACIA);
    }

    public String crearVenta(String jsonVenta) {
        return RespuestaBridge.ejecutar(() ->
                RespuestaBridge.json(ventas().registrar(JsonUtil.fromJson(jsonVenta, Venta.class))));
    }

    private GestorMovimientos movimientos() {
        return contexto.requerirServiciosActivos().movimientos();
    }

    private GestorVentas ventas() {
        return contexto.requerirServiciosActivos().ventas();
    }
}
