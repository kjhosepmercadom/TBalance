package sistemainventario.model;

public class Proveedor extends EntidadConId {

    private String  nombre;
    private String  contacto;
    private String  telefono;
    private String  email;
    private String  direccion;
    private String  categoria;
    private boolean activo;

    public Proveedor() { super(); activo = true; }

    public Proveedor(String id, String nombre, String contacto,
                     String telefono, String email, String direccion,
                     String categoria) {
        super(id);
        this.nombre     = nombre;
        this.contacto   = contacto;
        this.telefono   = telefono;
        this.email      = email;
        this.direccion  = direccion;
        this.categoria  = categoria;
        this.activo     = true;
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : id;
    }

    public String  getNombre()             { return nombre; }
    public void    setNombre(String n)     { this.nombre = n; }
    public String  getContacto()           { return contacto; }
    public void    setContacto(String c)   { this.contacto = c; }
    public String  getTelefono()           { return telefono; }
    public void    setTelefono(String t)   { this.telefono = t; }
    public String  getEmail()              { return email; }
    public void    setEmail(String e)      { this.email = e; }
    public String  getDireccion()          { return direccion; }
    public void    setDireccion(String d)  { this.direccion = d; }
    public String  getCategoria()          { return categoria; }
    public void    setCategoria(String c)  { this.categoria = c; }
    public boolean isActivo()              { return activo; }
    public void    setActivo(boolean a)    { this.activo = a; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s", id, nombre, telefono, email);
    }
}
