package sistemainventario.model;

import sistemainventario.exception.ValidacionException;
import sistemainventario.util.TextoUtil;

/**
 * Distribuidor o fabricante que abastece productos al negocio.
 */
public class Proveedor extends EntidadConId {

    private String nombre;
    private String contacto;
    private String telefono;
    private String email;
    private String direccion;
    private String categoria;
    private boolean activo = true;

    /** Constructor vacío requerido por Gson. */
    public Proveedor() {
        super();
    }

    public Proveedor(String id, String nombre, String contacto, String telefono,
                     String email, String direccion, String categoria) {
        super(id);
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.categoria = categoria;
    }

    /**
     * <b>GRASP Experto:</b> el proveedor valida sus propios datos.
     *
     * @throws ValidacionException si el nombre está vacío
     */
    public void validar() {
        if (TextoUtil.esVacio(nombre)) {
            throw new ValidacionException("El nombre del proveedor es obligatorio");
        }
    }

    /**
     * <b>GRASP Experto:</b> el proveedor sabe si coincide con un texto de búsqueda.
     *
     * @param texto texto buscado en nombre o contacto
     * @return {@code true} si hay coincidencia
     */
    public boolean coincideCon(String texto) {
        return TextoUtil.contieneIgnorandoMayusculas(nombre, texto)
                || TextoUtil.contieneIgnorandoMayusculas(contacto, texto);
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : super.getEtiqueta();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s", getId(), nombre, telefono, email);
    }
}
