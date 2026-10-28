package sistemainventario.service;

import sistemainventario.exception.ValidacionException;
import sistemainventario.model.Producto;
import sistemainventario.model.Venta;
import sistemainventario.repository.Repository;
import sistemainventario.util.FechaUtil;
import sistemainventario.util.IdGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registro y consulta de ventas.
 *
 * <p>Coordina el caso de uso "registrar venta": valida la venta, comprueba
 * el stock de todos los productos <i>antes</i> de modificar alguno (para no
 * dejar el inventario a medias) y luego descuenta y guarda.</p>
 */
public class GestorVentas {

    private final GestorInventario inventario;
    private final Repository<Venta> ventas;

    public GestorVentas(GestorInventario inventario, Repository<Venta> ventas) {
        this.inventario = Objects.requireNonNull(inventario);
        this.ventas = Objects.requireNonNull(ventas);
    }

    /**
     * @param venta venta recibida desde la vista
     * @return la venta registrada con ID, fecha, estado y total
     * @throws ValidacionException si la venta no es válida o falta stock
     */
    public Venta registrar(Venta venta) {
        if (venta == null) {
            throw new ValidacionException("No se recibieron datos de la venta");
        }
        venta.validar();
        Map<String, Integer> cantidades = venta.cantidadesPorProducto();
        verificarStock(cantidades);
        descontarStock(cantidades);
        venta.completar(IdGenerator.conPrefijo(Venta.PREFIJO_ID), FechaUtil.hoy());
        ventas.agregar(venta);
        return venta;
    }

    private void verificarStock(Map<String, Integer> cantidades) {
        cantidades.forEach((productoId, cantidad) ->
                inventario.obtener(productoId).verificarStockDisponible(cantidad));
    }

    private void descontarStock(Map<String, Integer> cantidades) {
        cantidades.forEach((productoId, cantidad) -> {
            Producto producto = inventario.obtener(productoId);
            producto.disminuirStock(cantidad);
            inventario.registrarCambioDeStock(producto);
        });
    }

    /** @return ventas de la más reciente a la más antigua, con el total sincronizado */
    public List<Venta> listarTodas() {
        List<Venta> lista = new ArrayList<>(ventas.listarTodos());
        lista.forEach(Venta::recalcularTotal);
        Collections.reverse(lista);
        return lista;
    }

    /** @return suma de los totales de las ventas completadas */
    public double getTotalVentas() {
        return ventas.filtrar(Venta::estaCompletada).stream().mapToDouble(Venta::getTotal).sum();
    }

    /** @return cantidad de ventas completadas */
    public int getTotalTransacciones() {
        return ventas.filtrar(Venta::estaCompletada).size();
    }
}
