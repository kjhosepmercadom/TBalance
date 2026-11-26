package sistemainventario.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import sistemainventario.model.Expense;

/** Gastos operativos del negocio, en la tabla GASTO. */
public class ExpenseJdbcRepository extends JdbcRepository<Expense> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public ExpenseJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "GASTO";
    }

    @Override
    protected List<String> columnas() {
        return List.of("PERIODO", "FECHA_PAGO", "CATEGORIA", "DESCRIPCION", "MONTO", "RECURRENTE", "ORIGEN_ID",
                "ACTIVO", "REGISTRADO_POR", "FECHA_REGISTRO");
    }

    @Override
    protected Object[] valores(Expense g) {
        return new Object[]{g.getPeriodo(), dia(g.getFechaPago()), g.getCategoria(), texto(g.getDescripcion()),
                g.getMonto(), g.isRecurrente() ? 1 : 0, referenciaGasto(g.getOrigenId()), g.isActivo() ? 1 : 0,
                texto(g.getRegistradoPor()), momentoORegistro(g.getFechaRegistro())};
    }

    @Override
    protected Expense leer(ResultSet fila) throws SQLException {
        Expense g = new Expense();
        g.setId(fila.getString("ID"));
        g.setPeriodo(fila.getString("PERIODO"));
        g.setFechaPago(leerDia(fila, "FECHA_PAGO"));
        g.setCategoria(fila.getString("CATEGORIA"));
        g.setDescripcion(fila.getString("DESCRIPCION"));
        g.setMonto(leerNumero(fila, "MONTO"));
        g.setRecurrente(fila.getInt("RECURRENTE") == 1);
        g.setOrigenId(fila.getString("ORIGEN_ID"));
        g.setActivo(fila.getInt("ACTIVO") == 1);
        g.setRegistradoPor(fila.getString("REGISTRADO_POR"));
        g.setFechaRegistro(leerMomento(fila, "FECHA_REGISTRO"));
        return g;
    }

    /** El gasto fijo de origen debe existir en el negocio; si no, queda sin origen. */
    private Object referenciaGasto(String id) {
        return referenciaValida("GASTO", id);
    }

    /** La columna es NOT NULL: si el gasto no trae fecha de registro, se usa la de ahora. */
    private static Object momentoORegistro(String iso) {
        return momento(iso == null || iso.isBlank() ? java.time.LocalDateTime.now().withNano(0).toString() : iso);
    }
}
