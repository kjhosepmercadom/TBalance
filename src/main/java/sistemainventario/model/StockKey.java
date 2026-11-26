package sistemainventario.model;

/**
 * De qué se descuenta una cantidad: un producto y, si tiene tallas, una variante.
 *
 * @param productoId ID del producto
 * @param varianteId ID de la variante, o {@code null} en un producto simple
 */
public record StockKey(String productoId, String varianteId) {
}
