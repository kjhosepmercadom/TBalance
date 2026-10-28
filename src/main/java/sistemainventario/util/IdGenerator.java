package sistemainventario.util;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generador de identificadores únicos.
 *
 * <p>Conserva los dos formatos que ya existen en los archivos JSON de P2:
 * IDs numéricos incrementales (productos, proveedores, perfiles) e IDs con
 * prefijo y marca de tiempo (categorías, entradas, salidas y ventas).</p>
 */
public final class IdGenerator {

    /** Primer valor usado cuando aún no existen IDs numéricos (el primero será 1001). */
    private static final int BASE_NUMERICA = 1000;

    private static final AtomicLong ULTIMA_MARCA = new AtomicLong();

    private IdGenerator() {
    }

    /**
     * Genera un ID con el formato {@code PREFIJO-marcaDeTiempo}.
     * Garantiza unicidad aunque se invoque varias veces en el mismo milisegundo.
     *
     * @param prefijo prefijo del tipo de entidad, por ejemplo {@code "VTA"}
     * @return identificador único
     */
    public static String conPrefijo(String prefijo) {
        long marca = ULTIMA_MARCA.updateAndGet(anterior -> Math.max(anterior + 1, System.currentTimeMillis()));
        return prefijo + "-" + marca;
    }

    /**
     * Calcula el siguiente ID numérico a partir de los existentes.
     *
     * @param idsExistentes IDs actuales; los que no son numéricos se ignoran
     * @return el mayor ID numérico + 1 (o {@code 1001} si no hay ninguno)
     */
    public static String siguienteNumerico(Collection<String> idsExistentes) {
        int maximo = idsExistentes.stream()
                .mapToInt(IdGenerator::aNumero)
                .max()
                .orElse(BASE_NUMERICA);
        return String.valueOf(maximo + 1);
    }

    private static int aNumero(String id) {
        if (id == null) {
            return 0;
        }
        try {
            return Integer.parseInt(id.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
