package sistemainventario.service;

import sistemainventario.model.*;
import sistemainventario.repository.VentaRepository;
import sistemainventario.util.FechaUtil;

import java.nio.file.Path;
import java.util.*;

public class GestorVentas {

    private final VentaRepository    repo;
    private final GestorInventario   gestorInventario;
    private final GestorMovimientos  gestorMovimientos;

    public GestorVentas(GestorInventario gestorInventario,
                        GestorMovimientos gestorMovimientos,
                        Path dirBase) {
        this.gestorInventario  = gestorInventario;
        this.gestorMovimientos = gestorMovimientos;
        this.repo              = new VentaRepository(dirBase);
    }

    public Venta crear(Venta venta) {
        if (venta.getItems() == null || venta.getItems().isEmpty()) return null;

        for (ItemVenta item : venta.getItems()) {
            Producto p = gestorInventario.buscarPorId(item.getProductoId());
            if (p == null || p.getStock() < item.getCantidad()) return null;
        }

        for (ItemVenta item : venta.getItems()) {
            Producto p = gestorInventario.buscarPorId(item.getProductoId());
            p.setStock(p.getStock() - item.getCantidad());
            gestorInventario.actualizar(p);
        }

        if (venta.getFecha() == null || venta.getFecha().isBlank())
            venta.setFecha(FechaUtil.hoy());
        if (venta.getEstado() == null || venta.getEstado().isBlank())
            venta.setEstado("COMPLETADA");

        repo.agregar(venta);
        return venta;
    }

    public List<Venta> listarTodas() {
        List<Venta> copia = new ArrayList<>(repo.listarTodos());
        Collections.reverse(copia);
        return copia;
    }

    public double getTotalVentas() {
        return repo.listarTodos().stream()
                .filter(v -> "COMPLETADA".equals(v.getEstado()))
                .mapToDouble(Venta::getTotal).sum();
    }

    public int getTotalTransacciones() {
        return (int) repo.listarTodos().stream()
                .filter(v -> "COMPLETADA".equals(v.getEstado())).count();
    }
}
