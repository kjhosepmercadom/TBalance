package sistemainventario.repository;

import sistemainventario.model.Sale;
import sistemainventario.model.SaleItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventas de un negocio: la venta en VENTA y sus productos en DETALLE_VENTA.
 * La venta y su detalle se guardan en la misma transacción.
 */
public class SaleJdbcRepository extends JdbcRepository<Sale> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public SaleJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "VENTA";
    }

    @Override
    protected List<String> columnas() {
        return List.of("CLIENTE", "METODO_PAGO", "OBSERVACION", "REFERENCIA_PAGO", "FECHA", "ESTADO", "TOTAL",
                "FECHA_ANULACION", "MOTIVO_ANULACION", "ANULADA_POR", "USUARIO_ID", "VENDEDOR", "FECHA_REGISTRO", "CLIENTE_DOCUMENTO",
                "NUMERO");
    }

    @Override
    protected Object[] valores(Sale v) {
        v.recalcularTotal();
        return new Object[]{texto(v.getCliente()), texto(v.getMetodoPago()), texto(v.getObservacion()),
                texto(v.getReferenciaPago()), momento(v.getFecha()), texto(v.getEstado()), v.getTotal(),
                momento(v.getFechaAnulacion()), texto(v.getMotivoAnulacion()), texto(v.getAnuladaPor()),
                texto(v.getUsuarioId()), texto(v.getVendedor()), momento(v.getFechaRegistro()),
                texto(v.getClienteDocumento()), v.getNumero()};
    }

    @Override
    protected Sale leer(ResultSet fila) throws SQLException {
        Sale v = new Sale();
        v.setId(fila.getString("ID"));
        v.setCliente(fila.getString("CLIENTE"));
        v.setMetodoPago(fila.getString("METODO_PAGO"));
        v.setObservacion(fila.getString("OBSERVACION"));
        v.setReferenciaPago(fila.getString("REFERENCIA_PAGO"));
        v.setFecha(leerMomento(fila, "FECHA"));
        v.setEstado(fila.getString("ESTADO"));
        v.setAnulacion(leerMomento(fila, "FECHA_ANULACION"), fila.getString("MOTIVO_ANULACION"));
        v.setAnuladaPor(fila.getString("ANULADA_POR"));
        v.setUsuarioId(fila.getString("USUARIO_ID"));
        v.setVendedor(fila.getString("VENDEDOR"));
        v.setFechaRegistro(leerMomento(fila, "FECHA_REGISTRO"));
        v.setClienteDocumento(fila.getString("CLIENTE_DOCUMENTO"));
        v.setNumero(fila.getLong("NUMERO"));
        return v;
    }

    /** Lee todo el detalle del negocio en una sola consulta y lo reparte entre las ventas. */
    @Override
    protected void completar(List<Sale> ventas) {
        Map<String, List<SaleItem>> porVenta = new LinkedHashMap<>();
        bd.consultar("SELECT VENTA_ID, PRODUCTO_ID, VARIANTE_ID, DESCRIPCION_VARIANTE, NOMBRE_PRODUCTO, CANTIDAD, "
                        + "PRECIO_UNITARIO, COSTO_UNITARIO, COSTO_OCULTO FROM DETALLE_VENTA WHERE PERFIL_ID = ? ORDER BY VENTA_ID, LINEA",
                fila -> {
                    SaleItem item = new SaleItem();
                    item.setProductoId(fila.getString("PRODUCTO_ID"));
                    item.setNombreProducto(fila.getString("NOMBRE_PRODUCTO"));
                    item.setVarianteId(fila.getString("VARIANTE_ID"));
                    item.setDescripcionVariante(fila.getString("DESCRIPCION_VARIANTE"));
                    item.setCantidad(leerCantidad(fila, "CANTIDAD"));
                    item.setPrecioUnitario(leerNumero(fila, "PRECIO_UNITARIO"));
                    item.setCostoUnitario(leerNumero(fila, "COSTO_UNITARIO"));
                    item.setCostoOculto(leerNumero(fila, "COSTO_OCULTO"));
                    porVenta.computeIfAbsent(fila.getString("VENTA_ID"), k -> new ArrayList<>()).add(item);
                    return item;
                }, perfilId);
        for (Sale venta : ventas) {
            venta.setItems(porVenta.getOrDefault(venta.getId(), List.of()));
            venta.recalcularTotal();
        }
    }

    /** Reescribe el detalle de la venta (al editar se borra el anterior). */
    /**
     * Da el número de comprobante dentro de la misma transacción que guarda la venta: la fila de
     * CONSECUTIVO queda bloqueada hasta que termina, así dos cajas no repiten ni se saltan números.
     */
    @Override
    protected void antesDeInsertar(Connection conexion, Sale venta) throws SQLException {
        Database.actualizar(conexion, "INSERT INTO CONSECUTIVO (PERFIL_ID, TIPO, ULTIMO) SELECT ?, 'COMPROBANTE', 0 "
                + "WHERE NOT EXISTS (SELECT 1 FROM CONSECUTIVO WHERE PERFIL_ID = ? AND TIPO = 'COMPROBANTE')",
                perfilId, perfilId);
        if (venta.getNumero() > 0) {
            // venta que ya trae número (copia desde archivos): el consecutivo no puede quedar atrás
            Database.actualizar(conexion, "UPDATE CONSECUTIVO SET ULTIMO = ? WHERE PERFIL_ID = ? AND TIPO = 'COMPROBANTE' AND ULTIMO < ?",
                    venta.getNumero(), perfilId, venta.getNumero());
            return;
        }
        Database.actualizar(conexion, "UPDATE CONSECUTIVO SET ULTIMO = ULTIMO + 1 WHERE PERFIL_ID = ? AND TIPO = 'COMPROBANTE'",
                perfilId);
        try (java.sql.PreparedStatement consulta = conexion.prepareStatement(
                "SELECT ULTIMO FROM CONSECUTIVO WHERE PERFIL_ID = ? AND TIPO = 'COMPROBANTE'")) {
            consulta.setString(1, perfilId);
            try (ResultSet fila = consulta.executeQuery()) {
                fila.next();
                venta.setNumero(fila.getLong(1));
            }
        }
    }

    @Override
    protected void guardarHijos(Connection conexion, Sale venta, boolean nueva) throws SQLException {
        if (!nueva) {
            Database.actualizar(conexion, "DELETE FROM DETALLE_VENTA WHERE PERFIL_ID = ? AND VENTA_ID = ?",
                    perfilId, venta.getId());
        }
        int linea = 1;
        for (SaleItem item : venta.getItems()) {
            Database.actualizar(conexion,
                    "INSERT INTO DETALLE_VENTA (PERFIL_ID, VENTA_ID, LINEA, PRODUCTO_ID, VARIANTE_ID, DESCRIPCION_VARIANTE, "
                            + "NOMBRE_PRODUCTO, CANTIDAD, PRECIO_UNITARIO, SUBTOTAL, COSTO_UNITARIO, COSTO_OCULTO) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    perfilId, venta.getId(), linea++, item.getProductoId(), texto(item.getVarianteId()),
                    texto(item.getDescripcionVariante()), texto(item.getNombreProducto()),
                    item.getCantidad(), item.getPrecioUnitario(), item.getSubtotal(), item.getCostoUnitario(),
                    item.getCostoOculto());
        }
    }
}
