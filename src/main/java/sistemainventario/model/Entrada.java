package sistemainventario.model;

public class Entrada extends EntidadConId {

    private String productoId;
    private String proveedorId;
    private int    cantidad;
    private double precioUnitario;
    private String fecha;
    private String observacion;

    public Entrada() { super(); }

    public Entrada(String id, String productoId, String proveedorId,
                   int cantidad, double precioUnitario,
                   String fecha, String observacion) {
        super(id);
        this.productoId     = productoId;
        this.proveedorId    = proveedorId;
        this.cantidad       = cantidad;
        this.precioUnitario = precioUnitario;
        this.fecha          = fecha;
        this.observacion    = observacion;
    }

    @Override
    public String getEtiqueta() {
        return id == null ? "" : id;
    }

    public String getProductoId()              { return productoId; }
    public void   setProductoId(String p)      { this.productoId = p; }
    public String getProveedorId()             { return proveedorId; }
    public void   setProveedorId(String p)     { this.proveedorId = p; }
    public int    getCantidad()                { return cantidad; }
    public void   setCantidad(int c)           { this.cantidad = c; }
    public double getPrecioUnitario()          { return precioUnitario; }
    public void   setPrecioUnitario(double p)  { this.precioUnitario = p; }
    public String getFecha()                   { return fecha; }
    public void   setFecha(String f)           { this.fecha = f; }
    public String getObservacion()             { return observacion; }
    public void   setObservacion(String o)     { this.observacion = o; }
}
