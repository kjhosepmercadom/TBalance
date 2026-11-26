package sistemainventario.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lee una lista de productos pegada desde Excel o escrita a mano (v3.8.0).
 *
 * <p>Una fila por producto. Las columnas van separadas por tabulador (lo que
 * Excel copia) o por punto y coma. Sin encabezado el orden es: nombre, costo,
 * precio, cantidad, código, categoría; con encabezado se reconocen por nombre
 * en cualquier orden. Los números se leen como se escriben en Colombia:
 * {@code 52.000} son cincuenta y dos mil y {@code 89,5} es ochenta y nueve y medio.</p>
 *
 * <p>Esta clase solo <b>lee</b>: no consulta ni guarda nada, así se prueba sola.</p>
 */
public final class ImportadorProductos {

    /** Tope de filas por pegada: más que eso se hace en dos tandas. */
    public static final int MAXIMO_FILAS = 500;

    /** Una fila leída; {@code error} queda vacío si la fila se entendió. */
    public record Fila(int linea, String nombre, Double costo, Double precio, BigDecimal cantidad,
                       String codigo, String categoria, String error) {
        public boolean valida() {
            return error == null;
        }
    }

    private static final List<String> NOMBRE = List.of("nombre", "producto", "articulo", "artículo", "descripcion", "descripción");
    private static final List<String> COSTO = List.of("costo", "compra", "precio compra", "precio de compra", "costo unitario");
    private static final List<String> PRECIO = List.of("precio", "venta", "precio venta", "precio de venta", "pvp");
    private static final List<String> CANTIDAD = List.of("cantidad", "stock", "existencias", "unidades", "inventario");
    private static final List<String> CODIGO = List.of("codigo", "código", "barras", "codigo de barras", "código de barras", "sku", "ref", "referencia");
    private static final List<String> CATEGORIA = List.of("categoria", "categoría", "linea", "línea");

    private ImportadorProductos() {
    }

    /**
     * @param texto lo pegado en la pantalla
     * @return las filas en orden; las vacías se saltan y las que no se entienden traen su error
     */
    public static List<Fila> leer(String texto) {
        List<Fila> filas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return filas;
        }
        String[] lineas = texto.split("\\r?\\n");
        int[] columnas = {0, 1, 2, 3, 4, 5};
        int desde = 0;
        int primera = primeraNoVacia(lineas);
        if (primera >= 0) {
            int[] porEncabezado = encabezado(separar(lineas[primera]));
            if (porEncabezado != null) {
                columnas = porEncabezado;
                desde = primera + 1;
            }
        }
        for (int i = desde; i < lineas.length; i++) {
            if (lineas[i].isBlank()) {
                continue;
            }
            if (filas.size() >= MAXIMO_FILAS) {
                filas.add(new Fila(i + 1, "", null, null, null, null, null,
                        "Son demasiadas filas: pegue máximo " + MAXIMO_FILAS + " a la vez"));
                break;
            }
            filas.add(leerFila(i + 1, separar(lineas[i]), columnas));
        }
        return filas;
    }

    private static int primeraNoVacia(String[] lineas) {
        for (int i = 0; i < lineas.length; i++) {
            if (!lineas[i].isBlank()) {
                return i;
            }
        }
        return -1;
    }

    static String[] separar(String linea) {
        String separador = linea.indexOf('\t') >= 0 ? "\t" : ";";
        return linea.split(separador, -1);
    }

    /** @return la posición de cada dato según el encabezado, o {@code null} si la fila no es un encabezado */
    private static int[] encabezado(String[] celdas) {
        int[] posicion = {-1, -1, -1, -1, -1, -1};
        List<List<String>> nombres = List.of(NOMBRE, COSTO, PRECIO, CANTIDAD, CODIGO, CATEGORIA);
        for (int c = 0; c < celdas.length; c++) {
            String celda = celdas[c].trim().toLowerCase(Locale.ROOT);
            for (int k = 0; k < nombres.size(); k++) {
                if (posicion[k] < 0 && nombres.get(k).contains(celda)) {
                    posicion[k] = c;
                    break;
                }
            }
        }
        // Es encabezado solo si reconoce el nombre y al menos otra columna
        long reconocidas = java.util.Arrays.stream(posicion).filter(p -> p >= 0).count();
        return posicion[0] >= 0 && reconocidas >= 2 ? posicion : null;
    }

    private static Fila leerFila(int linea, String[] celdas, int[] col) {
        String nombre = celda(celdas, col[0]);
        if (nombre.isEmpty()) {
            return error(linea, "", "Falta el nombre del producto");
        }
        Double costo = numero(celda(celdas, col[1]));
        Double precio = numero(celda(celdas, col[2]));
        Double cantidad = numero(celda(celdas, col[3]));
        if (!celda(celdas, col[1]).isEmpty() && costo == null) {
            return error(linea, nombre, "El costo no es un número: " + celda(celdas, col[1]));
        }
        if (!celda(celdas, col[2]).isEmpty() && precio == null) {
            return error(linea, nombre, "El precio no es un número: " + celda(celdas, col[2]));
        }
        if (!celda(celdas, col[3]).isEmpty() && cantidad == null) {
            return error(linea, nombre, "La cantidad no es un número: " + celda(celdas, col[3]));
        }
        if ((costo != null && costo < 0) || (precio != null && precio < 0) || (cantidad != null && cantidad < 0)) {
            return error(linea, nombre, "No puede haber valores negativos");
        }
        String codigo = celda(celdas, col[4]);
        String categoria = celda(celdas, col[5]);
        return new Fila(linea, nombre, costo, precio,
                cantidad == null ? BigDecimal.ZERO : BigDecimal.valueOf(cantidad),
                codigo.isEmpty() ? null : codigo, categoria.isEmpty() ? null : categoria, null);
    }

    private static Fila error(int linea, String nombre, String mensaje) {
        return new Fila(linea, nombre, null, null, null, null, null, mensaje);
    }

    private static String celda(String[] celdas, int posicion) {
        return posicion < 0 || posicion >= celdas.length ? "" : celdas[posicion].trim();
    }

    /**
     * @param texto número como se escribe en Colombia: {@code $ 52.000}, {@code 89,5}, {@code 1.250,75}
     * @return el valor, o {@code null} si está vacío o no es un número
     */
    public static Double numero(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.replace("$", "").replace(" ", "").replace(" ", "");
        if (t.isEmpty()) {
            return null;
        }
        if (t.contains(".") && t.contains(",")) {
            t = t.replace(".", "").replace(',', '.');
        } else if (t.contains(".")) {
            if (t.matches("\\d{1,3}(\\.\\d{3})+")) {
                t = t.replace(".", "");
            }
        } else if (t.contains(",")) {
            t = t.replace(',', '.');
        }
        try {
            double valor = Double.parseDouble(t);
            return Double.isNaN(valor) || Double.isInfinite(valor) ? null : valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
