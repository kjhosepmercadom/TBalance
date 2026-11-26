package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.QuantityUtil;
import sistemainventario.util.TextUtil;

import java.math.BigDecimal;

/**
 * Una talla y/o un color de un producto, con su propio stock (v3.6.0).
 *
 * <p>Ejemplo: la "Camiseta deportiva" tiene las variantes S, M y L, cada una con
 * su stock, su stock mínimo y su código. El producto padre guarda la suma de
 * todas (ver {@link Product#sincronizarStockConVariantes()}).</p>
 *
 * <p>Talla o color vacíos se guardan como cadena vacía {@code ''}, nunca como
 * {@code NULL}: en PostgreSQL dos {@code NULL} no son iguales para una
 * restricción UNIQUE, y se podría repetir "M sin color" en el mismo producto.
 * Por eso la tabla PRODUCTO_VARIANTE tiene {@code COLOR DEFAULT '' NOT NULL}.</p>
 */
public class ProductVariant {

    private String id;
    private String talla;
    private String color;
    private String codigo;
    private BigDecimal stock;
    private BigDecimal stockMin;
    /** {@code null}: usa el precio de venta del producto. */
    private Double precioVenta;
    private Boolean activo;

    public ProductVariant() {
    }

    /**
     * @param id    ID de la variante
     * @param talla talla ("M", "38") o vacío
     * @param color color o vacío
     * @param stock unidades disponibles
     */
    public ProductVariant(String id, String talla, String color, BigDecimal stock) {
        this.id = id;
        setTalla(talla);
        setColor(color);
        setStock(stock);
    }

    // ── Reglas (Experto) ───────────────────────────────────────────────

    /** @return por ejemplo "M / Azul", "M" o "Azul" */
    public String getDescripcion() {
        String t = getTalla();
        String c = getColor();
        if (t.isEmpty()) {
            return c;
        }
        return c.isEmpty() ? t : t + " / " + c;
    }

    /**
     * @param otra otra variante del mismo producto
     * @return {@code true} si es la misma talla y el mismo color (sin importar mayúsculas)
     */
    public boolean mismaCombinacion(ProductVariant otra) {
        return getTalla().equalsIgnoreCase(otra.getTalla()) && getColor().equalsIgnoreCase(otra.getColor());
    }

    /**
     * @param cantidad cantidad pedida
     * @return {@code true} si alcanza
     */
    public boolean tieneStockPara(BigDecimal cantidad) {
        return getStock().compareTo(cantidad) >= 0;
    }

    /** @param cantidad unidades que entran */
    public void aumentarStock(BigDecimal cantidad) {
        stock = QuantityUtil.normal(getStock().add(cantidad));
    }

    /**
     * @param cantidad     unidades que salen
     * @param nombreProducto para el mensaje de error
     */
    public void disminuirStock(BigDecimal cantidad, String nombreProducto) {
        if (!tieneStockPara(cantidad)) {
            throw new ValidationException("Stock insuficiente para " + nombreProducto + " (" + getDescripcion()
                    + "). Disponible: " + QuantityUtil.texto(getStock()));
        }
        stock = QuantityUtil.normal(getStock().subtract(cantidad));
    }

    /** @return {@code true} si el stock llegó al mínimo */
    public boolean estaBajoStock() {
        return getStock().signum() > 0 && getStock().compareTo(getStockMin()) <= 0;
    }

    /**
     * @param precioProducto precio de venta del producto padre
     * @return el precio propio de la variante, o el del producto si no tiene
     */
    public double precioVentaCon(double precioProducto) {
        return precioVenta != null && precioVenta > 0 ? precioVenta : precioProducto;
    }

    // ── Getters y setters ──────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** @return la talla, o cadena vacía si no tiene */
    public String getTalla() {
        return limpiar(talla);
    }

    public void setTalla(String talla) {
        this.talla = limpiar(talla);
    }

    /** @return el color, o cadena vacía si no tiene */
    public String getColor() {
        return limpiar(color);
    }

    public void setColor(String color) {
        this.color = limpiar(color);
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = TextUtil.esVacio(codigo) ? null : codigo.trim();
    }

    public BigDecimal getStock() {
        return stock == null ? BigDecimal.ZERO : stock;
    }

    public void setStock(BigDecimal stock) {
        this.stock = QuantityUtil.normal(stock);
    }

    public BigDecimal getStockMin() {
        return stockMin == null ? BigDecimal.ZERO : stockMin;
    }

    public void setStockMin(BigDecimal stockMin) {
        this.stockMin = QuantityUtil.normal(stockMin);
    }

    public Double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(Double precioVenta) {
        this.precioVenta = precioVenta != null && precioVenta > 0 ? precioVenta : null;
    }

    public boolean isActivo() {
        return !Boolean.FALSE.equals(activo);
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /** {@code null} y los espacios quedan como cadena vacía. */
    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
