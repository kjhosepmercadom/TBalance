package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.QuantityUtil;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Unidad en que se cuenta y se vende un producto (v3.6.0).
 *
 * <p>Se usa por <b>composición</b>: cualquier producto, simple o con variantes,
 * tiene una unidad, y la unidad decide si acepta cantidades con decimales.
 * Así la ferretería vende 1,5 metros de cable y la tienda vende 3 gaseosas.</p>
 */
public enum UnidadMedida {

    UNIDAD("UND", "und", "Unidad", false),
    METRO("M", "m", "Metro", true),
    KILO("KG", "kg", "Kilo", true),
    LITRO("L", "L", "Litro", true),
    CAJA("CAJA", "caja", "Caja", false);

    private final String codigo;
    private final String simbolo;
    private final String etiqueta;
    private final boolean fraccionable;

    UnidadMedida(String codigo, String simbolo, String etiqueta, boolean fraccionable) {
        this.codigo = codigo;
        this.simbolo = simbolo;
        this.etiqueta = etiqueta;
        this.fraccionable = fraccionable;
    }

    /** @return código guardado en la columna UNIDAD_MEDIDA */
    public String getCodigo() {
        return codigo;
    }

    /** @return abreviatura que se muestra junto a la cantidad ("1,5 m") */
    public String getSimbolo() {
        return simbolo;
    }

    /** @return nombre para la pantalla */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** @return {@code true} si acepta cantidades con decimales */
    public boolean isFraccionable() {
        return fraccionable;
    }

    /**
     * Revisa una cantidad que se va a vender, entrar o sacar.
     *
     * @param cantidad       cantidad pedida
     * @param negocioFracciona {@code true} si el negocio tiene activa la venta por fracción
     * @param producto       nombre del producto, para el mensaje
     * @return la cantidad normalizada
     * @throws ValidationException si no es positiva o trae decimales donde no se permiten
     */
    public BigDecimal validar(BigDecimal cantidad, boolean negocioFracciona, String producto) {
        if (!QuantityUtil.esPositiva(cantidad)) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
        BigDecimal limpia = QuantityUtil.normal(cantidad);
        if (!QuantityUtil.esEntera(limpia) && !(fraccionable && negocioFracciona)) {
            throw new ValidationException(producto + " se maneja por unidades enteras (" + etiqueta.toLowerCase()
                    + "). Escriba una cantidad sin decimales.");
        }
        return limpia;
    }

    /**
     * @param codigo código de la base, nombre del enum o símbolo
     * @return la unidad, o {@link #UNIDAD} si no se reconoce (datos de versiones anteriores)
     */
    public static UnidadMedida desdeCodigo(String codigo) {
        String c = codigo == null ? "" : codigo.trim();
        return Arrays.stream(values())
                .filter(u -> u.codigo.equalsIgnoreCase(c) || u.name().equalsIgnoreCase(c) || u.simbolo.equals(c))
                .findFirst().orElse(UNIDAD);
    }
}
