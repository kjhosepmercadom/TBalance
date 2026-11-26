package sistemainventario.model;

import java.util.Arrays;

/**
 * Cómo se controla el inventario de un producto (v3.6.0).
 *
 * <p>Cada producto tiene el suyo y de él depende la estrategia que lo maneja
 * ({@code InventoryHandling}): un producto simple tiene un solo stock; uno con
 * variantes reparte su stock por talla y color. Las fases siguientes agregan
 * {@code LOTES} (vencimiento con FEFO) y {@code SERIALES} (IMEI y garantía) sin
 * cambiar el código que vende.</p>
 */
public enum ManejoProducto {

    /** Un solo stock para todo el producto (lo de siempre). */
    SIMPLE("Simple"),
    /** Stock por talla y color (Boutique). */
    VARIANTES("Con tallas y colores");

    private final String etiqueta;

    ManejoProducto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** @return nombre para la pantalla */
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @param codigo nombre guardado en la base o en el JSON
     * @return el manejo, o {@link #SIMPLE} si no se reconoce (datos de versiones anteriores)
     */
    public static ManejoProducto desdeCodigo(String codigo) {
        return Arrays.stream(values()).filter(m -> m.name().equalsIgnoreCase(codigo == null ? "" : codigo.trim()))
                .findFirst().orElse(SIMPLE);
    }
}
