package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Expense;
import sistemainventario.service.ExpenseService;
import sistemainventario.util.JsonUtil;

/**
 * Gastos operativos y utilidad neta real. Quién registra un gasto lo pone Java con la sesión,
 * nunca la página.
 */
public class ExpenseController {

    private final AppContext contexto;

    /** @param contexto contexto de la aplicación */
    public ExpenseController(AppContext contexto) {
        this.contexto = contexto;
    }

    /**
     * @param periodo mes, "yyyy-MM"
     * @return gastos del mes (crea antes las copias de los gastos fijos)
     */
    public String obtenerGastos(String periodo) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(gastos().listarMes(periodo)));
    }

    /** @return gastos fijos que se copian cada mes */
    public String obtenerGastosRecurrentes() {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(gastos().listarRecurrentes()));
    }

    /**
     * @param jsonGasto periodo, fechaPago, categoria, descripcion, monto, recurrente (y id si se corrige uno)
     * @return el gasto guardado
     */
    public String guardarGasto(String jsonGasto) {
        return BridgeResponse.ejecutar(() -> {
            Expense gasto = JsonUtil.fromJson(jsonGasto, Expense.class);
            return BridgeResponse.json(gastos().guardar(gasto, contexto.getSesion().requerir().getNombre()));
        });
    }

    /**
     * @param id ID del gasto
     * @return OK o el error
     */
    public String eliminarGasto(String id) {
        return BridgeResponse.ejecutarOk(() -> gastos().eliminar(id));
    }

    /**
     * @param periodo mes, "yyyy-MM"
     * @return utilidad neta real, punto de equilibrio y gastos por clase
     */
    public String obtenerUtilidadNeta(String periodo) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(gastos().resumen(periodo)));
    }

    /** @return porcentaje de gastos sobre ventas (el del negocio o el del mes anterior), o vacío si no hay con qué calcularlo */
    public String obtenerCargaGastos() {
        return BridgeResponse.ejecutar(() -> {
            Double carga = gastos().cargaGastosPct();
            return carga == null ? "null" : BridgeResponse.json(carga);
        });
    }

    private ExpenseService gastos() {
        return contexto.requerirServiciosActivos().gastos();
    }
}
