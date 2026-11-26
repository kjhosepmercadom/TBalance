package sistemainventario.manejo;

import java.math.BigDecimal;

/**
 * De dónde sale (o a dónde entra) una parte de lo vendido.
 *
 * <p>Una estrategia la arma en {@link InventoryHandling#reservar}: un producto
 * simple devuelve una sola línea sin variante; uno con tallas, la línea de la
 * talla elegida. En la fase 2 una venta de farmacia podrá devolver varias
 * líneas, una por lote (FEFO), y en la fase 3 una por serial.</p>
 *
 * @param productoId ID del producto
 * @param varianteId talla/color, o {@code null}
 * @param loteId     lote (fase 2), hoy siempre {@code null}
 * @param serial     serial o IMEI (fase 3), hoy siempre {@code null}
 * @param cantidad   cuánto
 */
public record StockLine(String productoId, String varianteId, String loteId, String serial, BigDecimal cantidad) {

    /**
     * @param productoId ID del producto
     * @param varianteId talla/color, o {@code null}
     * @param cantidad   cuánto
     * @return la línea, sin lote ni serial
     */
    public static StockLine de(String productoId, String varianteId, BigDecimal cantidad) {
        return new StockLine(productoId, varianteId, null, null, cantidad);
    }
}
