package sistemainventario.model;

import java.util.Arrays;

/**
 * Qué tanto controla un negocio las fechas de vencimiento (v3.6.0).
 */
public enum ControlVencimiento {

    /** No se pide fecha (Boutique, Ferretería, Tecnología). */
    NINGUNO("No maneja vencimiento"),
    /** Cada producto decide si la pide (Tienda de barrio, Supermercado). */
    OPCIONAL("Opcional por producto"),
    /** Todos los productos la piden (Farmacia). En la fase 2 se controla por lote con FEFO. */
    OBLIGATORIO("Obligatorio en todos los productos");

    private final String etiqueta;

    ControlVencimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** @return texto para la pantalla */
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @param texto nombre guardado
     * @param defecto valor si no se reconoce
     * @return el control de vencimiento
     */
    public static ControlVencimiento desdeTexto(String texto, ControlVencimiento defecto) {
        return Arrays.stream(values()).filter(c -> c.name().equalsIgnoreCase(texto == null ? "" : texto.trim()))
                .findFirst().orElse(defecto);
    }
}
