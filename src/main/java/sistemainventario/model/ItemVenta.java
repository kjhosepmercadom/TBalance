package sistemainventario.model;

public class ItemVenta {

    private String productoId;
    private String nombreProducto;
    private int    cantidad;
    private double precioUnitario;
    private double subtotal;

    public ItemVenta() {}

    public ItemVenta(String productoId, String nombreProducto,
                     int cantidad, double precioUnitario) {
        this.productoId      = productoId;
        this.nombreProducto  = nombreProducto;
        this.cantidad        = cantidad;
        this.precioUnitario  = precioUnitario;
        this.subtotal        = calcularSubtotal();
    }

    private double calcularSubtotal() {
        return cantidad * precioUnitario;
    }

    /**
     * Devuelve el subtotal siempre calculado como cantidad × precio.
     * Si por deserialización el subtotal almacenado es 0, se recalcula
     * para garantizar la consistencia del total de la venta.
     */
    public double getSubtotal() {
        if (subtotal > 0) return subtotal;
        return calcularSubtotal();
    }

    public void setCantidad(int c) {
        this.cantidad = c;
        this.subtotal = calcularSubtotal();
    }

    public void setPrecioUnitario(double p) {
        this.precioUnitario = p;
        this.subtotal = calcularSubtotal();
    }

    public void setSubtotal(double s) { this.subtotal = s; }

    public String getProductoId()             { return productoId; }
    public void   setProductoId(String p)     { this.productoId = p; }
    public String getNombreProducto()         { return nombreProducto; }
    public void   setNombreProducto(String n) { this.nombreProducto = n; }
    public int    getCantidad()               { return cantidad; }
    public double getPrecioUnitario()         { return precioUnitario; }
}
