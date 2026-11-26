package sistemainventario.manejo;

import sistemainventario.model.BusinessCapabilities;
import sistemainventario.model.ManejoProducto;
import sistemainventario.model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * <b>Patrón Strategy</b> (v3.6.0): las reglas de inventario que cambian según
 * cómo se controla un producto ({@link ManejoProducto}).
 *
 * <p>Los servicios de ventas, entradas y salidas no preguntan si el producto
 * tiene tallas: le piden a su estrategia que reserve, descuente o reponga.
 * Agregar un manejo nuevo (lotes con FEFO, seriales) es escribir otra clase que
 * implemente esta interfaz y registrarla en {@link InventoryHandlingFactory};
 * ni la venta ni las entradas cambian (<b>OCP</b>).</p>
 */
public interface InventoryHandling {

    /** @return el manejo que implementa esta estrategia */
    ManejoProducto tipo();

    /**
     * @param capacidades lo que maneja el negocio
     * @return qué campos muestra el formulario del producto
     */
    FormularioProducto formulario(BusinessCapabilities capacidades);

    /**
     * Revisa y completa el producto antes de guardarlo.
     *
     * @param producto    producto que llega de la pantalla
     * @param capacidades lo que maneja el negocio
     * @throws sistemainventario.exception.ValidationException si algo no cuadra
     */
    void validar(Product producto, BusinessCapabilities capacidades);

    /**
     * Decide de dónde sale una cantidad, sin cambiar nada todavía. Si no
     * alcanza, lanza el error: así una venta revisa todos sus productos antes
     * de descontar el primero.
     *
     * @param producto   producto vendido
     * @param varianteId talla/color elegido, o {@code null}
     * @param cantidad   cuánto
     * @return las líneas que se van a descontar
     */
    List<StockLine> reservar(Product producto, String varianteId, BigDecimal cantidad);

    /**
     * Descuenta lo reservado (venta, salida o anulación de una entrada).
     *
     * @param producto producto
     * @param lineas   lo que devolvió {@link #reservar}
     */
    void descontar(Product producto, List<StockLine> lineas);

    /**
     * Devuelve unidades al inventario (entrada, anulación de una venta o de una salida).
     *
     * @param producto producto
     * @param linea    a dónde entran
     */
    void reponer(Product producto, StockLine linea);
}
