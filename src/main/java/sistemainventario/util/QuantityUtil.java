package sistemainventario.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cantidades de inventario con decimales (1,5 metros, 0,25 kilos).
 *
 * <p>Desde la v3.6.0 las cantidades son {@link BigDecimal} y no {@code int}: un
 * {@code double} sumaría mal (0,1 + 0,2 no da 0,3) y el inventario se
 * descuadraría. En la base se guardan con 3 decimales ({@code NUMERIC(13,3)}).</p>
 */
public final class QuantityUtil {

    /** Decimales que se guardan en la base. */
    public static final int DECIMALES = 3;

    private QuantityUtil() {
    }

    /**
     * Deja la cantidad con 3 decimales como máximo y sin ceros de sobra
     * ({@code 10.000} queda {@code 10}, {@code 1.50} queda {@code 1.5}).
     *
     * @param cantidad cantidad, puede ser {@code null}
     * @return la cantidad normalizada ({@code 0} si era {@code null})
     */
    public static BigDecimal normal(BigDecimal cantidad) {
        if (cantidad == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal limpia = cantidad.setScale(DECIMALES, RoundingMode.HALF_UP).stripTrailingZeros();
        return limpia.scale() < 0 ? limpia.setScale(0) : limpia;
    }

    /**
     * @param numero número entero o decimal
     * @return la cantidad como {@link BigDecimal}
     */
    public static BigDecimal de(double numero) {
        return normal(BigDecimal.valueOf(numero));
    }

    /**
     * @param numero número entero
     * @return la cantidad como {@link BigDecimal}
     */
    public static BigDecimal de(long numero) {
        return BigDecimal.valueOf(numero);
    }

    /**
     * @param cantidad cantidad
     * @return {@code true} si no tiene parte decimal
     */
    public static boolean esEntera(BigDecimal cantidad) {
        return cantidad == null || normal(cantidad).scale() <= 0;
    }

    /**
     * @param cantidad cantidad
     * @return {@code true} si es mayor que cero
     */
    public static boolean esPositiva(BigDecimal cantidad) {
        return cantidad != null && cantidad.signum() > 0;
    }

    /**
     * @param cantidad cantidad
     * @return {@code true} si es menor que cero
     */
    public static boolean esNegativa(BigDecimal cantidad) {
        return cantidad != null && cantidad.signum() < 0;
    }

    /**
     * @param cantidad cantidad
     * @return la cantidad escrita como la ve el usuario: {@code 10}, {@code 1,5}
     */
    public static String texto(BigDecimal cantidad) {
        return normal(cantidad).toPlainString().replace('.', ',');
    }
}
