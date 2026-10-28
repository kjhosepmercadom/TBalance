package sistemainventario.service;

import sistemainventario.model.*;
import sistemainventario.repository.*;
import sistemainventario.util.FechaUtil;

import java.nio.file.Path;
import java.util.*;

public class GestorMovimientos {

    private final GestorInventario  gestorInventario;
    private final EntradaRepository entradaRepo;
    private final SalidaRepository  salidaRepo;

    public GestorMovimientos(GestorInventario gestorInventario, Path dirBase) {
        this.gestorInventario = gestorInventario;
        this.entradaRepo      = new EntradaRepository(dirBase);
        this.salidaRepo       = new SalidaRepository(dirBase);
    }

    public List<Entrada> listarEntradas() {
        return entradaRepo.listarTodos();
    }

    public List<Salida> listarSalidas() {
        return salidaRepo.listarTodos();
    }

    public boolean registrarEntrada(Entrada e) {
        if (e == null || e.getProductoId() == null) return false;
        Producto p = gestorInventario.buscarPorId(e.getProductoId());
        if (p == null || e.getCantidad() <= 0) return false;
        if (e.getFecha() == null || e.getFecha().isBlank()) e.setFecha(FechaUtil.hoy());
        p.setStock(p.getStock() + e.getCantidad());
        gestorInventario.actualizar(p);
        entradaRepo.agregar(e);
        return true;
    }

    public boolean registrarSalida(Salida s) {
        if (s == null || s.getProductoId() == null) return false;
        Producto p = gestorInventario.buscarPorId(s.getProductoId());
        if (p == null || s.getCantidad() <= 0 || p.getStock() < s.getCantidad()) return false;
        if (s.getFecha() == null || s.getFecha().isBlank()) s.setFecha(FechaUtil.hoy());
        p.setStock(p.getStock() - s.getCantidad());
        gestorInventario.actualizar(p);
        salidaRepo.agregar(s);
        return true;
    }
}
