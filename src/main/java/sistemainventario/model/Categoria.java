package sistemainventario.model;

public class Categoria extends EntidadConId {

    private String nombre;
    private String descripcion;

    public Categoria() { super(); }

    public Categoria(String id, String nombre, String descripcion) {
        super(id);
        this.nombre      = nombre;
        this.descripcion = descripcion;
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : id;
    }

    public String getNombre()                 { return nombre; }
    public void   setNombre(String n)         { this.nombre = n; }
    public String getDescripcion()            { return descripcion; }
    public void   setDescripcion(String d)    { this.descripcion = d; }
}
