package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.DateUtil;
import sistemainventario.util.TextUtil;

import sistemainventario.util.QuantityUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Artículo del inventario de un perfil.
 *
 * <p><b>GRASP Experto en Información:</b> el producto es quien posee el stock,
 * el stock mínimo y la fecha de vencimiento; por eso es él quien decide su
 * estado, si tiene existencias suficientes, si está vencido y cómo se
 * incrementa o descuenta su stock. Los servicios ya no manipulan
 * {@code setStock(...)} directamente.</p>
 *
 * <p>Los nombres de los atributos no cambian respecto a P2 para mantener la
 * compatibilidad con {@code productos.json}.</p>
 *
 * <p><b>v3.6.0:</b> el stock es {@link BigDecimal} (se puede vender 1,5 metros),
 * cada producto dice cómo se controla ({@link ManejoProducto}) y en qué unidad
 * se cuenta ({@link UnidadMedida}). Si maneja tallas, {@link #stock} es siempre
 * la suma de sus {@link ProductVariant variantes}: así los informes, Hoy, Costos
 * y el Excel siguen leyendo un solo número por producto.</p>
 */
public class Product extends IdentifiedEntity implements Deshabilitable {

    public static final String ESTADO_AGOTADO = "Agotado";
    public static final String ESTADO_BAJO_STOCK = "Bajo Stock";
    public static final String ESTADO_DISPONIBLE = "Disponible";

    private String nombre;
    private String codigo;
    private String descripcion;
    private String categoria;
    private String categoriaId;
    private String proveedorId;
    private double precioCompra;
    private double precioVenta;
    /** Costo por unidad que no se ve en la venta (empaque, bolsa, flete, comisión). */
    private double costoOculto;
    private BigDecimal stock;
    private BigDecimal stockMin;
    private ManejoProducto manejo;
    private UnidadMedida unidadMedida;
    private List<ProductVariant> variantes = new ArrayList<>();
    private String tipo;
    private boolean requiereVencimiento;
    private String fechaVencimiento;

    /** {@code null} en los datos de versiones anteriores, que cuentan como habilitados. */
    private Boolean activo;

    /** Constructor vacío requerido por Gson. */
    public Product() {
        super();
    }

    public Product(String id, String nombre, String descripcion,
                    String categoria, String categoriaId, String proveedorId,
                    double precioCompra, double precioVenta,
                    BigDecimal stock, BigDecimal stockMin, String tipo) {
        super(id);
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.categoriaId = categoriaId;
        this.proveedorId = proveedorId;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        setStock(stock);
        setStockMin(stockMin);
        this.tipo = tipo;
    }

    // ── Validación ──────────────────────────────────────────────────────

    /**
     * Verifica las reglas de negocio del producto antes de guardarlo.
     *
     * @param hoy fecha de referencia para validar el vencimiento
     * @throws ValidationException si algún dato es inválido
     */
    public void validar(LocalDate hoy) {
        if (TextUtil.esVacio(nombre)) {
            throw new ValidationException("El nombre del producto es obligatorio");
        }
        if (precioCompra < 0 || precioVenta < 0) {
            throw new ValidationException("Los precios no pueden ser negativos");
        }
        if (QuantityUtil.esNegativa(getStock()) || QuantityUtil.esNegativa(getStockMin())) {
            throw new ValidationException("El stock no puede ser negativo");
        }
        validarFechaVencimiento(hoy);
    }

    private void validarFechaVencimiento(LocalDate hoy) {
        if (!requiereVencimiento) {
            return;
        }
        if (TextUtil.esVacio(fechaVencimiento)) {
            throw new ValidationException("La fecha de vencimiento es obligatoria");
        }
        LocalDate fecha = getFechaVencimientoComoFecha()
                .orElseThrow(() -> new ValidationException("Formato de fecha invalido (use yyyy-MM-dd)"));
        if (fecha.isBefore(hoy)) {
            throw new ValidationException("La fecha de vencimiento no puede ser anterior a hoy");
        }
    }

    // ── Stock (Experto) ─────────────────────────────────────────────────

    /** @return el estado legible según el stock actual y el mínimo */
    public String getEstado() {
        if (estaAgotado()) {
            return ESTADO_AGOTADO;
        }
        return estaBajoStock() ? ESTADO_BAJO_STOCK : ESTADO_DISPONIBLE;
    }

    /** @return {@code true} si no quedan unidades */
    public boolean estaAgotado() {
        return getStock().signum() <= 0;
    }

    /** @return {@code true} si quedan unidades pero en el mínimo o por debajo */
    public boolean estaBajoStock() {
        if (tieneVariantes()) {
            return !estaAgotado() && variantesActivas().stream().anyMatch(ProductVariant::estaBajoStock);
        }
        return getStock().signum() > 0 && getStock().compareTo(getStockMin()) <= 0;
    }

    /**
     * @param cantidad unidades requeridas
     * @return {@code true} si el stock cubre la cantidad
     */
    public boolean tieneStockPara(BigDecimal cantidad) {
        return getStock().compareTo(cantidad) >= 0;
    }

    /**
     * Incrementa el stock (entrada de mercancía).
     *
     * @param cantidad unidades a sumar, debe ser mayor que cero
     * @throws ValidationException si la cantidad no es positiva
     */
    public void aumentarStock(BigDecimal cantidad) {
        validarCantidadPositiva(cantidad);
        stock = QuantityUtil.normal(getStock().add(cantidad));
    }

    /**
     * Descuenta stock (salida o venta).
     *
     * @param cantidad unidades a descontar, debe ser mayor que cero
     * @throws ValidationException si la cantidad no es positiva o el stock es insuficiente
     */
    public void disminuirStock(BigDecimal cantidad) {
        validarCantidadPositiva(cantidad);
        verificarStockDisponible(cantidad);
        stock = QuantityUtil.normal(getStock().subtract(cantidad));
    }

    /**
     * @param cantidad unidades requeridas
     * @throws ValidationException si el stock no alcanza
     */
    public void verificarStockDisponible(BigDecimal cantidad) {
        if (!tieneStockPara(cantidad)) {
            throw new ValidationException("Stock insuficiente para " + getEtiqueta()
                    + ". Disponible: " + QuantityUtil.texto(getStock()));
        }
    }

    private static void validarCantidadPositiva(BigDecimal cantidad) {
        if (!QuantityUtil.esPositiva(cantidad)) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
    }

    // ── Tallas y colores (Experto) ──────────────────────────────────────

    /** @return {@code true} si el producto reparte su stock por tallas o colores */
    public boolean tieneVariantes() {
        return getManejo() == ManejoProducto.VARIANTES;
    }

    /** @return las variantes habilitadas */
    public List<ProductVariant> variantesActivas() {
        return getVariantes().stream().filter(ProductVariant::isActivo).toList();
    }

    /**
     * @param varianteId ID de la variante
     * @return la variante, si es de este producto
     */
    public Optional<ProductVariant> buscarVariante(String varianteId) {
        if (varianteId == null) {
            return Optional.empty();
        }
        return getVariantes().stream().filter(v -> varianteId.equals(v.getId())).findFirst();
    }

    /**
     * @param codigoLeido código escaneado o escrito
     * @return la variante que tiene ese código
     */
    public Optional<ProductVariant> buscarVariantePorCodigo(String codigoLeido) {
        if (TextUtil.esVacio(codigoLeido)) {
            return Optional.empty();
        }
        return variantesActivas().stream()
                .filter(v -> v.getCodigo() != null && v.getCodigo().equalsIgnoreCase(codigoLeido.trim()))
                .findFirst();
    }

    /** Deja en {@link #stock} y {@link #stockMin} la suma de las variantes habilitadas. */
    public void sincronizarStockConVariantes() {
        if (!tieneVariantes()) {
            return;
        }
        List<ProductVariant> activas = variantesActivas();
        stock = QuantityUtil.normal(activas.stream().map(ProductVariant::getStock).reduce(BigDecimal.ZERO, BigDecimal::add));
        stockMin = QuantityUtil.normal(activas.stream().map(ProductVariant::getStockMin).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * @param varianteId variante vendida, o {@code null}
     * @return el precio de venta de esa variante (o el del producto)
     */
    public double precioVentaDe(String varianteId) {
        return buscarVariante(varianteId).map(v -> v.precioVentaCon(precioVenta)).orElse(precioVenta);
    }

    // ── Utilidad (Experto) ──────────────────────────────────────────────

    /** @return {@code true} si se registró el precio de compra (necesario para estimar la utilidad) */
    public boolean tienePrecioCompra() {
        return precioCompra > 0;
    }

    /** @return ganancia por unidad: precio de venta menos precio de compra (negativa si se vende a pérdida) */
    public double getUtilidadUnitaria() {
        return precioVenta - precioCompra;
    }

    /** @return porcentaje de la venta que es ganancia, o 0 si el precio de venta es 0 */
    public double getMargen() {
        return precioVenta > 0 ? getUtilidadUnitaria() / precioVenta * 100 : 0;
    }

    /** @return utilidad que dejaría vender todo el stock actual a los precios registrados */
    public double getUtilidadEstimadaStock() {
        return getUtilidadUnitaria() * Math.max(getStock().doubleValue(), 0);
    }

    // ── Vencimiento (Experto) ───────────────────────────────────────────

    /** @return la fecha de vencimiento si está controlada y es válida */
    public Optional<LocalDate> getFechaVencimientoComoFecha() {
        return requiereVencimiento ? DateUtil.parsear(fechaVencimiento) : Optional.empty();
    }

    /**
     * @param hoy fecha de referencia
     * @return {@code true} si el producto controla vencimiento y la fecha ya pasó
     */
    public boolean estaVencido(LocalDate hoy) {
        return getFechaVencimientoComoFecha().map(f -> f.isBefore(hoy)).orElse(false);
    }

    /**
     * @param dias ventana de días a evaluar
     * @param hoy  fecha de referencia
     * @return {@code true} si vence desde hoy y antes de {@code hoy + dias}
     */
    public boolean venceDentroDe(int dias, LocalDate hoy) {
        LocalDate limite = hoy.plusDays(dias);
        return getFechaVencimientoComoFecha()
                .map(f -> !f.isBefore(hoy) && f.isBefore(limite))
                .orElse(false);
    }

    // ── Búsqueda (Experto) ──────────────────────────────────────────────

    /**
     * @param texto texto buscado en nombre, descripción, código o ID
     * @return {@code true} si hay coincidencia (ignorando mayúsculas)
     */
    public boolean coincideCon(String texto) {
        return TextUtil.contieneIgnorandoMayusculas(nombre, texto)
                || TextUtil.contieneIgnorandoMayusculas(descripcion, texto)
                || TextUtil.contieneIgnorandoMayusculas(codigo, texto)
                || getVariantes().stream().anyMatch(v -> TextUtil.contieneIgnorandoMayusculas(v.getCodigo(), texto))
                || TextUtil.contieneIgnorandoMayusculas(getId(), texto);
    }

    /**
     * @param nombreCategoria categoría a comparar
     * @return {@code true} si el producto pertenece a esa categoría
     */
    public boolean perteneceACategoria(String nombreCategoria) {
        return nombreCategoria != null && nombreCategoria.equals(categoria);
    }

    // ── Etiqueta QR (Experto) ───────────────────────────────────────────

    /** Primera línea de toda etiqueta QR de TBalance. */
    public static final String ENCABEZADO_QR = QrLabel.ENCABEZADO;

    /**
     * <b>GRASP Experto:</b> el producto conoce sus datos, por eso arma el texto
     * que se codifica en su etiqueta QR (formato en {@link QrLabel}). El texto
     * es corto a propósito: un QR con pocos datos tiene módulos más grandes y la
     * webcam lo lee mejor.
     *
     * @param perfilId negocio dueño del producto, para no confundir etiquetas de otro negocio
     * @return texto de la etiqueta: {@code TBalance}, el ID, el negocio y el precio de venta
     */
    public String contenidoEtiquetaQR(String perfilId) {
        return QrLabel.texto(this, perfilId);
    }

    /**
     * Operación inversa de {@link #contenidoEtiquetaQR(String)}.
     *
     * @param contenido texto leído del código QR
     * @return el ID, o vacío si el texto no es una etiqueta de TBalance
     */
    public static Optional<String> idDesdeEtiquetaQR(String contenido) {
        return QrLabel.leer(contenido).map(QrLabel::productoId);
    }

    /** @return precio de venta con separador de miles, por ejemplo {@code $ 2.500} */
    public String getPrecioVentaFormateado() {
        return "$ " + String.format(Locale.ROOT, "%,.0f", precioVenta).replace(',', '.');
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : super.getEtiqueta();
    }

    // ── Getters y setters (encapsulamiento) ─────────────────────────────

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(String proveedorId) {
        this.proveedorId = proveedorId;
    }

    /** @return costo oculto por unidad (0 si no tiene) */
    public double getCostoOculto() {
        return costoOculto;
    }

    /** @param costoOculto costo extra por unidad; no puede ser negativo */
    public void setCostoOculto(double costoOculto) {
        this.costoOculto = Math.max(0, costoOculto);
    }

    /**
     * @return ganancia por unidad después del costo de compra y del costo oculto
     */
    public double getGananciaRealUnidad() {
        return getPrecioVenta() - getPrecioCompra() - costoOculto;
    }

    public double getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(double precioCompra) {
        this.precioCompra = precioCompra;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    /** @return unidades disponibles (en un producto con tallas, la suma de todas) */
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

    /** @return cómo se controla el inventario ({@link ManejoProducto#SIMPLE} en datos viejos) */
    public ManejoProducto getManejo() {
        return manejo == null ? ManejoProducto.SIMPLE : manejo;
    }

    public void setManejo(ManejoProducto manejo) {
        this.manejo = manejo;
    }

    /** @return unidad en que se cuenta ({@link UnidadMedida#UNIDAD} en datos viejos) */
    public UnidadMedida getUnidadMedida() {
        return unidadMedida == null ? UnidadMedida.UNIDAD : unidadMedida;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    /** @return todas las variantes, también las deshabilitadas (vacía en un producto simple) */
    public List<ProductVariant> getVariantes() {
        if (variantes == null) {
            variantes = new ArrayList<>();
        }
        return variantes;
    }

    public void setVariantes(List<ProductVariant> variantes) {
        this.variantes = variantes == null ? new ArrayList<>() : new ArrayList<>(variantes);
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isRequiereVencimiento() {
        return requiereVencimiento;
    }

    public void setRequiereVencimiento(boolean requiereVencimiento) {
        this.requiereVencimiento = requiereVencimiento;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(String fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    @Override
    public boolean isActivo() {
        return !Boolean.FALSE.equals(activo);
    }

    @Override
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
