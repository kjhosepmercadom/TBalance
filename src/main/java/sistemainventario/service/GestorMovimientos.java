package sistemainventario.service;

import sistemainventario.exception.ValidacionException;
import sistemainventario.model.Entrada;
import sistemainventario.model.MovimientoInventario;
import sistemainventario.model.Producto;
import sistemainventario.model.Salida;
import sistemainventario.repository.Repository;
import sistemainventario.util.FechaUtil;
import sistemainventario.util.IdGenerator;

import java.util.List;
import java.util.Objects;

/**
 * Registro de entradas y salidas de inventario.
 *
 * <p><b>Bajo acoplamiento:</b> en P2 esta clase creaba sus propios
 * repositorios con {@code new EntradaRepository(dir)}, duplicando las cachés
 * que ya existían en {@code AppContext}. Ahora los recibe por constructor
 * (inyección de dependencias) como interfaces {@link Repository}.</p>
 *
 * <p><b>Polimorfismo:</b> {@link #registrar} trabaja con
 * {@link MovimientoInventario}; cada subclase decide si suma o resta stock.</p>
 */
public class GestorMovimientos {

    private final GestorInventario inventario;
    private final Repository<Entrada> entradas;
    private final Repository<Salida> salidas;

    public GestorMovimientos(GestorInventario inventario,
                             Repository<Entrada> entradas,
                             Repository<Salida> salidas) {
        this.inventario = Objects.requireNonNull(inventario);
        this.entradas = Objects.requireNonNull(entradas);
        this.salidas = Objects.requireNonNull(salidas);
    }

    public List<Entrada> listarEntradas() {
        return entradas.listarTodos();
    }

    public List<Salida> listarSalidas() {
        return salidas.listarTodos();
    }

    /**
     * @param entrada datos de la entrada
     * @return la entrada registrada
     * @throws ValidacionException si el producto no existe o la cantidad no es válida
     */
    public Entrada registrarEntrada(Entrada entrada) {
        return registrar(entrada, entradas);
    }

    /**
     * @param salida datos de la salida
     * @return la salida registrada
     * @throws ValidacionException si el stock es insuficiente o los datos no son válidos
     */
    public Salida registrarSalida(Salida salida) {
        return registrar(salida, salidas);
    }

    /**
     * Algoritmo común para cualquier movimiento: validar, aplicar al
     * producto, persistir el producto y guardar el movimiento.
     */
    private <M extends MovimientoInventario> M registrar(M movimiento, Repository<M> repositorio) {
        if (movimiento == null) {
            throw new ValidacionException("No se recibieron datos del movimiento");
        }
        movimiento.validar();
        Producto producto = inventario.obtener(movimiento.getProductoId());
        movimiento.aplicarA(producto);
        inventario.registrarCambioDeStock(producto);
        movimiento.registrar(IdGenerator.conPrefijo(movimiento.getPrefijoId()), FechaUtil.hoy());
        repositorio.agregar(movimiento);
        return movimiento;
    }
}
