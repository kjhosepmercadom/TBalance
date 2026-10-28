package sistemainventario.model;

public class Usuario extends EntidadConId {

    private String  nombre;
    private String  usuario;
    private String  passwordHash;
    private String  rol;
    private boolean activo;

    public Usuario() { super(); activo = true; }

    public Usuario(String id, String nombre, String usuario,
                   String passwordHash, String rol) {
        super(id);
        this.nombre       = nombre;
        this.usuario      = usuario;
        this.passwordHash = passwordHash;
        this.rol          = rol;
        this.activo       = true;
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : usuario;
    }

    public String  getNombre()             { return nombre; }
    public void    setNombre(String n)     { this.nombre = n; }
    public String  getUsuario()            { return usuario; }
    public void    setUsuario(String u)    { this.usuario = u; }
    public String  getPasswordHash()       { return passwordHash; }
    public void    setPasswordHash(String h){ this.passwordHash = h; }
    public String  getRol()                { return rol; }
    public void    setRol(String r)        { this.rol = r; }
    public boolean isActivo()              { return activo; }
    public void    setActivo(boolean a)    { this.activo = a; }
}
