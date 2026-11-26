package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Category;
import sistemainventario.model.PoliticaPrecio;
import sistemainventario.model.Product;
import sistemainventario.model.ProductVariant;
import sistemainventario.util.JsonUtil;
import sistemainventario.util.TextUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Lo que hace rápido el registro de productos (v3.8.0): sugerir el precio con
 * el margen del negocio, reconocer un código ya conocido, partir de un producto
 * parecido y cargar muchos productos de una vez pegándolos desde Excel.
 *
 * <p>No guarda nada por su cuenta, salvo {@link #importar}: todo el guardado pasa por
 * {@link InventoryService#guardar}, así las reglas de siempre (categoría, tallas,
 * vencimiento) valen igual para un producto cargado por lista.</p>
 */
public class AltaAgilService {

    /** Márgenes que la pantalla ofrece como botones, además del del negocio. */
    public static final List<Integer> MARGENES_RAPIDOS = List.of(20, 30, 40, 50);

    /** Una opción de precio: el margen, el precio que resulta y cuánto se gana por unidad. */
    public record OpcionPrecio(int margenPct, double precio, double ganancia) {
    }

    /** Respuesta a "¿a cuánto lo vendo?". */
    public record SugerenciaPrecio(double costoTotal, double margenPct, String redondeo,
                                   double precioSugerido, double ganancia, List<OpcionPrecio> opciones) {
    }

    /** Estado de una fila al revisar o cargar una lista. */
    public enum Estado { LISTA, DUPLICADO, ERROR, CREADO }

    /** Una fila de la lista con lo que se hará con ella. */
    public record FilaImportacion(int linea, String nombre, Double costo, Double precio, BigDecimal cantidad,
                                  String codigo, String categoria, boolean precioSugerido,
                                  Estado estado, String mensaje) {
    }

    /** Resumen de revisar o cargar una lista. */
    public record ResultadoImportacion(List<FilaImportacion> filas, int listas, int duplicados,
                                       int errores, int creadas) {
    }

    private final InventoryService inventario;
    private final CategoryService categorias;
    private final Supplier<PoliticaPrecio> politica;

    /**
     * @param inventario productos del negocio
     * @param categorias categorías del negocio
     * @param politica   de dónde sale el margen y el redondeo vigentes
     */
    public AltaAgilService(InventoryService inventario, CategoryService categorias, Supplier<PoliticaPrecio> politica) {
        this.inventario = inventario;
        this.categorias = categorias;
        this.politica = politica;
    }

    // ── Precio ──────────────────────────────────────────────────────────

    /**
     * @param costo      precio de compra por unidad
     * @param costoOculto empaque, bolsa o flete por unidad (puede ser 0)
     * @return el precio con el margen del negocio y las opciones de los botones
     * @throws ValidationException si el costo no es un número válido
     */
    public SugerenciaPrecio sugerirPrecio(double costo, double costoOculto) {
        if (Double.isNaN(costo) || Double.isNaN(costoOculto) || costo < 0 || costoOculto < 0) {
            throw new ValidationException("El costo no puede ser negativo");
        }
        PoliticaPrecio p = politica.get();
        double total = costo + costoOculto;
        double precio = p.precioSugerido(total);
        List<OpcionPrecio> opciones = new ArrayList<>();
        for (int margen : MARGENES_RAPIDOS) {
            double precioOpcion = p.precioParaMargen(total, margen);
            opciones.add(new OpcionPrecio(margen, precioOpcion, precioOpcion - total));
        }
        return new SugerenciaPrecio(total, p.margenPct(), p.redondeo().name(), precio,
                precio > 0 ? precio - total : 0, opciones);
    }

    // ── Reconocer y copiar ──────────────────────────────────────────────

    /**
     * @param codigo código de barras o código propio, tal como lo leyó el lector
     * @return el producto habilitado que ya tiene ese código (o una talla con ese código)
     */
    public Optional<Product> buscarPorCodigo(String codigo) {
        if (TextUtil.esVacio(codigo)) {
            return Optional.empty();
        }
        String buscado = codigo.trim();
        return inventario.listarTodos().stream()
                .filter(Product::isActivo)
                .filter(p -> buscado.equalsIgnoreCase(p.getCodigo())
                        || p.getVariantes().stream().anyMatch(v -> buscado.equalsIgnoreCase(v.getCodigo())))
                .findFirst();
    }

    /**
     * Arma un producto nuevo a partir de uno parecido, sin guardarlo: la pantalla lo
     * muestra para que el usuario cambie solo lo distinto (nombre, color, precio).
     * Se copian categoría, proveedor, precios, unidad, manejo y las tallas y colores;
     * no se copian el ID, el código de barras, el stock ni el vencimiento.
     *
     * @param idOrigen producto del que se parte
     * @return el producto nuevo, sin ID y con el stock en cero
     */
    public Product plantillaDesde(String idOrigen) {
        Product origen = inventario.obtener(idOrigen);
        Product copia = JsonUtil.fromJson(JsonUtil.toJson(origen), Product.class);
        copia.setId(null);
        copia.setCodigo(null);
        copia.setStock(BigDecimal.ZERO);
        copia.setFechaVencimiento(null);
        copia.setActivo(true);
        List<ProductVariant> variantes = new ArrayList<>();
        for (ProductVariant original : origen.variantesActivas()) {
            ProductVariant v = new ProductVariant(null, original.getTalla(), original.getColor(), BigDecimal.ZERO);
            v.setStockMin(original.getStockMin());
            variantes.add(v);
        }
        copia.setVariantes(variantes);
        return copia;
    }

    // ── Lista pegada desde Excel ────────────────────────────────────────

    /**
     * Revisa una lista sin guardar nada: dice qué filas se cargarían, cuáles ya existen y cuáles tienen error.
     *
     * @param texto lo pegado en la pantalla
     * @return el resultado fila por fila
     */
    public ResultadoImportacion revisar(String texto) {
        return procesar(texto, false);
    }

    /**
     * Carga las filas buenas. Las que ya existen (mismo nombre) o tienen error se saltan y
     * quedan en el resultado; una fila mala no frena a las demás.
     *
     * @param texto lo pegado en la pantalla
     * @return el resultado fila por fila, con las creadas marcadas
     */
    public ResultadoImportacion importar(String texto) {
        return procesar(texto, true);
    }

    private ResultadoImportacion procesar(String texto, boolean guardar) {
        List<ImportadorProductos.Fila> leidas = ImportadorProductos.leer(texto);
        if (leidas.isEmpty()) {
            throw new ValidationException("Pegue la lista de productos: un producto por fila");
        }
        Set<String> existentes = inventario.listarTodos().stream()
                .map(p -> clave(p.getNombre())).collect(Collectors.toCollection(java.util.HashSet::new));
        PoliticaPrecio p = politica.get();
        List<FilaImportacion> salida = new ArrayList<>();
        int listas = 0;
        int duplicados = 0;
        int errores = 0;
        int creadas = 0;
        for (ImportadorProductos.Fila fila : leidas) {
            if (!fila.valida()) {
                salida.add(fila(fila, false, Estado.ERROR, fila.error()));
                errores++;
                continue;
            }
            if (existentes.contains(clave(fila.nombre()))) {
                salida.add(fila(fila, false, Estado.DUPLICADO, "Ya existe un producto con ese nombre"));
                duplicados++;
                continue;
            }
            double costo = fila.costo() == null ? 0 : fila.costo();
            boolean sugerido = (fila.precio() == null || fila.precio() == 0) && costo > 0;
            double precio = sugerido ? p.precioSugerido(costo) : fila.precio() == null ? 0 : fila.precio();
            try {
                Product producto = construir(fila, costo, precio);
                String aviso = sugerido ? "Precio calculado con el margen del negocio" : null;
                if (guardar) {
                    inventario.guardar(producto);
                    salida.add(new FilaImportacion(fila.linea(), fila.nombre(), costo, precio, fila.cantidad(),
                            fila.codigo(), producto.getCategoria(), sugerido, Estado.CREADO, aviso));
                    creadas++;
                } else {
                    validarSinGuardar(producto);
                    salida.add(new FilaImportacion(fila.linea(), fila.nombre(), costo, precio, fila.cantidad(),
                            fila.codigo(), producto.getCategoria(), sugerido, Estado.LISTA, aviso));
                    listas++;
                }
                existentes.add(clave(fila.nombre()));
            } catch (ValidationException e) {
                salida.add(fila(fila, sugerido, Estado.ERROR, e.getMessage()));
                errores++;
            }
        }
        return new ResultadoImportacion(salida, listas, duplicados, errores, creadas);
    }

    private Product construir(ImportadorProductos.Fila fila, double costo, double precio) {
        if (precio <= 0) {
            throw new ValidationException("Falta el precio de venta (o el costo, para calcularlo)");
        }
        Product producto = new Product();
        producto.setNombre(fila.nombre());
        producto.setPrecioCompra(costo);
        producto.setPrecioVenta(precio);
        producto.setStock(fila.cantidad());
        producto.setStockMin(BigDecimal.valueOf(5));
        producto.setCodigo(fila.codigo());
        Category categoria = elegirCategoria(fila.categoria());
        producto.setCategoriaId(categoria.getId());
        producto.setCategoria(categoria.getNombre());
        return producto;
    }

    private Category elegirCategoria(String nombre) {
        return categorias.buscarPorNombre(nombre).filter(Category::isActivo).orElseGet(categorias::sinCategoria);
    }

    /** La revisión usa las mismas reglas que el guardado, pero sin tocar la base. */
    private void validarSinGuardar(Product producto) {
        producto.validar(java.time.LocalDate.now());
        sistemainventario.manejo.InventoryHandlingFactory.para(producto)
                .validar(producto, inventario.capacidadesActuales());
    }

    private static FilaImportacion fila(ImportadorProductos.Fila f, boolean sugerido, Estado estado, String mensaje) {
        return new FilaImportacion(f.linea(), f.nombre(), f.costo(), f.precio(), f.cantidad(),
                f.codigo(), f.categoria(), sugerido, estado, mensaje);
    }

    private static String clave(String nombre) {
        return nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
