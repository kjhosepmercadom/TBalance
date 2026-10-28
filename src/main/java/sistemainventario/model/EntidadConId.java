package sistemainventario.model;

/**
 * Clase abstracta base que aplica ABSTRACCIÓN y HERENCIA sobre todas las
 * entidades persistibles del sistema. Garantiza que cualquier entidad
 * (Producto, Categoria, Proveedor, Entrada, Salida, Venta, Perfil) tenga
 * un identificador único y operaciones polimórficas comunes.
 *
 * El polimorfismo se aplica en {@link sistemainventario.repository.JsonRepository}
 * que trabaja con cualquier subclase de esta clase.
 */
public abstract class EntidadConId {

    protected String id;

    public EntidadConId() {}

    public EntidadConId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Etiqueta legible para mostrar en selectores y tablas.
     * Las subclases pueden sobreescribirla (polimorfismo).
     */
    public String getEtiqueta() {
        return id == null ? "" : id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntidadConId)) return false;
        EntidadConId that = (EntidadConId) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
