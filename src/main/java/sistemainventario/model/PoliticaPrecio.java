package sistemainventario.model;

import sistemainventario.exception.ValidationException;

/**
 * El margen y el redondeo con que un negocio fija sus precios (v3.8.0).
 *
 * <p>El margen se mide sobre el precio de venta, igual que la barra de utilidad
 * del formulario: costo $52.000 y margen 42 % dan un precio de $89.655, que con
 * redondeo "termina en 900" queda en $89.900. Fórmula: {@code precio = costo / (1 - margen)}.</p>
 *
 * @param margenPct porcentaje de ganancia sobre el precio, entre 1 y 90
 * @param redondeo  cómo termina el precio
 */
public record PoliticaPrecio(double margenPct, RedondeoPrecio redondeo) {

    public static final double MARGEN_MINIMO = 1;
    public static final double MARGEN_MAXIMO = 90;
    /** Lo que se usa mientras el dueño no elija otra cosa. */
    public static final PoliticaPrecio POR_DEFECTO = new PoliticaPrecio(40, RedondeoPrecio.CIEN);

    public PoliticaPrecio {
        if (Double.isNaN(margenPct) || margenPct < MARGEN_MINIMO || margenPct > MARGEN_MAXIMO) {
            throw new ValidationException("El margen debe estar entre 1 % y 90 %");
        }
        redondeo = redondeo == null ? RedondeoPrecio.CIEN : redondeo;
    }

    /**
     * @param costoTotal lo que cuesta cada unidad: precio de compra más costo oculto
     * @return precio de venta sugerido, ya redondeado; 0 si todavía no hay costo
     */
    public double precioSugerido(double costoTotal) {
        return precioParaMargen(costoTotal, margenPct);
    }

    /**
     * @param costoTotal lo que cuesta cada unidad
     * @param margen     margen a probar, distinto del que tiene la política (los chips de la pantalla)
     * @return precio de venta con ese margen y el redondeo de la política
     */
    public double precioParaMargen(double costoTotal, double margen) {
        if (costoTotal <= 0 || margen < MARGEN_MINIMO || margen > MARGEN_MAXIMO) {
            return 0;
        }
        return redondeo.aplicar(costoTotal / (1 - margen / 100.0));
    }

    /**
     * @param costoTotal costo por unidad
     * @param precio     precio de venta
     * @return margen real sobre el precio, en porcentaje; 0 si no hay precio
     */
    public static double margenReal(double costoTotal, double precio) {
        return precio <= 0 ? 0 : (precio - costoTotal) / precio * 100.0;
    }
}
