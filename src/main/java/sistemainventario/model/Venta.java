package sistemainventario.model;

import sistemainventario.exception.ValidacionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transacción comercial compuesta por uno o varios {@link ItemVenta}.
 *
 * <p><b>GRASP Creator:</b> la venta contiene y registra sus ítems, por eso es
 * ella quien los crea en {@link #agregarItem(Producto, int, double)}.</p>
 *
 * <p><b>GRASP Experto:</b> conoce todos sus ítems, por lo que calcula el
 * total, las unidades y las cantidades agrupadas por producto.</p>
 */
public class Venta extends EntidadConId {

    public static final String PREFIJO_ID = "VTA";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";

    private String cliente;
    private String metodoPago;
    private String observacion;
    private String fecha;
    private String estado;
    private List<ItemVenta> items = new ArrayList<>();
    private double total;

    /** Constructor vacío requerido por Gson. */
    public Venta() {
        super();
    }

    public Venta(String id, String cliente, String metodoPago, String observacion, String fecha) {
        super(id);
        this.cliente = cliente;
        this.metodoPago = metodoPago;
        this.observacion = observacion;
        this.fecha = fecha;
        this.estado = ESTADO_COMPLETADA;
    }

    /**
     * <b>Creator:</b> crea y agrega un ítem a partir de un producto.
     *
     * @param producto       producto vendido
     * @param cantidad       unidades vendidas
     * @param precioUnitario precio aplicado en la venta
     * @return el ítem creado
     */
    public ItemVenta agregarItem(Producto producto, int cantidad, double precioUnitario) {
        ItemVenta item = new ItemVenta(producto.getId(), producto.getNombre(), cantidad, precioUnitario);
        item.validar();
        itemsSeguros().add(item);
        recalcularTotal();
        return item;
    }

    /**
     * @throws ValidacionException si la venta no tiene ítems o alguno es inválido
     */
    public void validar() {
        if (itemsSeguros().isEmpty()) {
            throw new ValidacionException("La venta debe tener al menos un producto");
        }
        itemsSeguros().forEach(ItemVenta::validar);
    }

    /**
     * Asigna ID, fecha y estado COMPLETADA, y sincroniza el total persistido.
     *
     * @param nuevoId        ID generado por el servicio
     * @param fechaRegistro  fecha de la venta
     */
    public void completar(String nuevoId, String fechaRegistro) {
        setId(nuevoId);
        this.fecha = fechaRegistro;
        this.estado = ESTADO_COMPLETADA;
        recalcularTotal();
    }

    /** @return suma de los subtotales de los ítems */
    public double calcularTotal() {
        return itemsSeguros().stream().mapToDouble(ItemVenta::getSubtotal).sum();
    }

    /** Sincroniza los atributos {@code total} y {@code subtotal} antes de serializar. */
    public void recalcularTotal() {
        itemsSeguros().forEach(ItemVenta::sincronizarSubtotal);
        this.total = calcularTotal();
    }

    /** @return total de la venta, siempre consistente con sus ítems */
    public double getTotal() {
        return calcularTotal();
    }

    /** @return número total de unidades vendidas */
    public int getTotalUnidades() {
        return itemsSeguros().stream().mapToInt(ItemVenta::getCantidad).sum();
    }

    /**
     * Agrupa las cantidades por producto; útil cuando el mismo producto
     * aparece en más de un ítem.
     *
     * @return mapa productoId → unidades totales
     */
    public Map<String, Integer> cantidadesPorProducto() {
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (ItemVenta item : itemsSeguros()) {
            cantidades.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    /** @return {@code true} si la venta está en estado COMPLETADA */
    public boolean estaCompletada() {
        return ESTADO_COMPLETADA.equals(estado);
    }

    private List<ItemVenta> itemsSeguros() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /** @return vista de solo lectura de los ítems */
    public List<ItemVenta> getItems() {
        return Collections.unmodifiableList(itemsSeguros());
    }

    public void setItems(List<ItemVenta> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
