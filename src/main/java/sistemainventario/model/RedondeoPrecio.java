package sistemainventario.model;

/**
 * Cómo termina el precio de venta que sugiere TBalance (v3.8.0).
 *
 * <p>Siempre se redondea <b>hacia arriba</b>, nunca hacia abajo: así el margen
 * que eligió el dueño no se pierde por el redondeo.</p>
 */
public enum RedondeoPrecio {

    /** Sin redondear: se cobra el valor exacto, al peso. */
    EXACTO("Exacto", 0),
    /** Al siguiente múltiplo de 100 (89.655 pasa a 89.700). */
    CIEN("A 100", 100),
    /** Al siguiente múltiplo de 500. */
    QUINIENTOS("A 500", 500),
    /** Al siguiente múltiplo de 1.000. */
    MIL("A 1.000", 1000),
    /** Termina en 900 (89.655 pasa a 89.900), el precio "psicológico" del comercio. */
    TERMINA_900("Termina en 900", 0);

    private final String etiqueta;
    private final int multiplo;

    RedondeoPrecio(String etiqueta, int multiplo) {
        this.etiqueta = etiqueta;
        this.multiplo = multiplo;
    }

    /** @return texto para mostrar en la pantalla */
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @param valor precio sin redondear, mayor o igual que cero
     * @return el precio redondeado hacia arriba según la regla
     */
    public double aplicar(double valor) {
        if (valor <= 0 || Double.isNaN(valor) || Double.isInfinite(valor)) {
            return 0;
        }
        switch (this) {
            case EXACTO:
                return Math.ceil(valor);
            case TERMINA_900:
                double base = Math.floor(valor / 1000) * 1000 + 900;
                return base >= valor ? base : base + 1000;
            default:
                return Math.ceil(valor / multiplo) * multiplo;
        }
    }

    /**
     * @param texto nombre de la regla (sin importar mayúsculas)
     * @param defecto lo que se devuelve si el texto está vacío o no es una regla
     * @return la regla pedida
     */
    public static RedondeoPrecio desde(String texto, RedondeoPrecio defecto) {
        if (texto == null || texto.isBlank()) {
            return defecto;
        }
        try {
            return valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return defecto;
        }
    }
}
