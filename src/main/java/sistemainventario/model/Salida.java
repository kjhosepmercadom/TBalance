package sistemainventario.model;

public class Salida extends EntidadConId {

    private String productoId;
    private int    cantidad;
    private String motivo;
    private String fecha;
    private String tipo;

    public Salida() { super(); }

    public Salida(String id, String productoId, int cantidad,
                  String motivo, String fecha, String tipo) {
        super(id);
        this.productoId  = productoId;
        this.cantidad    = cantidad;
        this.motivo      = motivo;
        this.fecha       = fecha;
        this.tipo        = tipo;
    }

    @Override
    public String getEtiqueta() {
        return id == null ? "" : id;
    }

    public String getProductoId()             { return productoId; }
    public void   setProductoId(String p)     { this.productoId = p; }
    public int    getCantidad()               { return cantidad; }
    public void   setCantidad(int c)          { this.cantidad = c; }
    public String getMotivo()                 { return motivo; }
    public void   setMotivo(String m)         { this.motivo = m; }
    public String getFecha()                  { return fecha; }
    public void   setFecha(String f)          { this.fecha = f; }
    public String getTipo()                   { return tipo; }
    public void   setTipo(String t)           { this.tipo = t; }
}
