package sistemainventario.model;

import java.util.ArrayList;
import java.util.List;

public class Venta extends EntidadConId {

    private String          cliente;
    private String          metodoPago;
    private String          observacion;
    private String          fecha;
    private String          estado;
    private List<ItemVenta> items = new ArrayList<>();
    private double          total;

    public Venta() { super(); items = new ArrayList<>(); }

    public Venta(String id, String cliente, String metodoPago,
                 String observacion, String fecha) {
        super(id);
        this.cliente     = cliente;
        this.metodoPago  = metodoPago;
        this.observacion = observacion;
        this.fecha       = fecha;
        this.estado      = "COMPLETADA";
        this.items       = new ArrayList<>();
    }

    @Override
    public String getEtiqueta() {
        return id == null ? "" : id;
    }

    /**
     * Calcula el total como suma de (cantidad x precioUnitario) de cada item.
     * Actualiza el campo {@link #total} para que sea serializable por Gson.
     */
    public double getTotal() {
        if (items == null) {
            this.total = 0.0;
            return 0.0;
        }
        double t = 0.0;
        for (ItemVenta i : items) {
            t += i.getCantidad() * i.getPrecioUnitario();
        }
        this.total = t;
        return t;
    }

    public void setTotal(double total) { this.total = total; }

    /**
     * Recalcula el total a partir de los items. Util para asegurar
     * que el campo {@link #total} este sincronizado antes de serializar.
     */
    public void recalcularTotal() {
        getTotal();
    }

    public int getTotalUnidades() {
        return items.stream().mapToInt(ItemVenta::getCantidad).sum();
    }

    public String          getCliente()                 { return cliente; }
    public void            setCliente(String c)         { this.cliente = c; }
    public String          getMetodoPago()              { return metodoPago; }
    public void            setMetodoPago(String m)      { this.metodoPago = m; }
    public String          getObservacion()             { return observacion; }
    public void            setObservacion(String o)     { this.observacion = o; }
    public String          getFecha()                   { return fecha; }
    public void            setFecha(String f)           { this.fecha = f; }
    public String          getEstado()                  { return estado; }
    public void            setEstado(String e)          { this.estado = e; }
    public List<ItemVenta> getItems()                   { return items; }
    public void            setItems(List<ItemVenta> i)  { this.items = i; }
    public void            addItem(ItemVenta item)      { items.add(item); }
}
