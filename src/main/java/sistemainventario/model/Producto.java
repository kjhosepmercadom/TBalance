package sistemainventario.model;

public class Producto extends EntidadConId {

    private String nombre;
    private String codigo;
    private String descripcion;
    private String categoria;
    private String categoriaId;
    private String proveedorId;
    private double precioCompra;
    private double precioVenta;
    private int    stock;
    private int    stockMin;
    private String tipo;
    private boolean requiereVencimiento;
    private String  fechaVencimiento;

    public Producto() { super(); }

    public Producto(String id, String nombre, String descripcion,
                    String categoria, String categoriaId, String proveedorId,
                    double precioCompra, double precioVenta,
                    int stock, int stockMin, String tipo) {
        super(id);
        this.nombre       = nombre;
        this.descripcion  = descripcion;
        this.categoria    = categoria;
        this.categoriaId  = categoriaId;
        this.proveedorId  = proveedorId;
        this.precioCompra = precioCompra;
        this.precioVenta  = precioVenta;
        this.stock        = stock;
        this.stockMin     = stockMin;
        this.tipo         = tipo;
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : id;
    }

    public String getEstado() {
        if (stock == 0) return "Agotado";
        if (stock <= stockMin) return "Bajo Stock";
        return "Disponible";
    }

    public String getNombre()                { return nombre; }
    public void   setNombre(String n)        { this.nombre = n; }
    public String getCodigo()                { return codigo; }
    public void   setCodigo(String c)        { this.codigo = c; }
    public String getDescripcion()           { return descripcion; }
    public void   setDescripcion(String d)   { this.descripcion = d; }
    public String getCategoria()             { return categoria; }
    public void   setCategoria(String c)     { this.categoria = c; }
    public String getCategoriaId()           { return categoriaId; }
    public void   setCategoriaId(String id)  { this.categoriaId = id; }
    public String getProveedorId()           { return proveedorId; }
    public void   setProveedorId(String id)  { this.proveedorId = id; }
    public double getPrecioCompra()          { return precioCompra; }
    public void   setPrecioCompra(double p)  { this.precioCompra = p; }
    public double getPrecioVenta()           { return precioVenta; }
    public void   setPrecioVenta(double p)   { this.precioVenta = p; }
    public int    getStock()                 { return stock; }
    public void   setStock(int s)            { this.stock = s; }
    public int    getStockMin()              { return stockMin; }
    public void   setStockMin(int m)         { this.stockMin = m; }
    public String getTipo()                  { return tipo; }
    public void   setTipo(String t)          { this.tipo = t; }

    public boolean isRequiereVencimiento()                 { return requiereVencimiento; }
    public void    setRequiereVencimiento(boolean r)       { this.requiereVencimiento = r; }
    public String  getFechaVencimiento()                   { return fechaVencimiento; }
    public void    setFechaVencimiento(String f)           { this.fechaVencimiento = f; }
}
