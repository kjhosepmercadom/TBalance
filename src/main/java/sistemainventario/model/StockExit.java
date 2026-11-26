package sistemainventario.model;

import sistemainventario.manejo.InventoryHandling;
import sistemainventario.manejo.InventoryHandlingFactory;
import sistemainventario.manejo.StockLine;
import java.math.BigDecimal;

/**
 * Retiro de mercancía que no es una venta (merma, daño, uso interno):
 * reduce el stock del producto.
 */
public class StockExit extends InventoryMovement {

    public static final String PREFIJO_ID = "SAL";

    private String motivo;
    private String tipo;

    /** Constructor vacío requerido por Gson. */
    public StockExit() {
        super();
    }

    public StockExit(String id, String productoId, BigDecimal cantidad,
                  String motivo, String fecha, String tipo) {
        super(id, productoId, cantidad, fecha);
        this.motivo = motivo;
        this.tipo = tipo;
    }

    /** Polimorfismo: una salida descuenta unidades y valida el stock disponible. */
    @Override
    public void aplicarA(Product producto) {
        InventoryHandling estrategia = InventoryHandlingFactory.para(producto);
        estrategia.descontar(producto, estrategia.reservar(producto, getVarianteId(), getCantidad()));
    }

    @Override
    public String getPrefijoId() {
        return PREFIJO_ID;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
