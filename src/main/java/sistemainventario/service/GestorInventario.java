package sistemainventario.service;

import sistemainventario.model.Producto;
import sistemainventario.repository.ProductoRepository;
import sistemainventario.util.IdGenerator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class GestorInventario {

    private final ProductoRepository repo;

    public GestorInventario(ProductoRepository repo) {
        this.repo = repo;
    }

    public List<Producto> listarTodos() {
        return repo.listarTodos();
    }

    public Producto buscarPorId(String id) {
        return repo.buscarPorId(id);
    }

    public boolean agregar(Producto p) {
        if (p == null || repo.idExiste(p.getId())) return false;
        if (p.getId() == null || p.getId().isBlank())
            p.setId(IdGenerator.siguienteNumerico(
                    repo.listarTodos().stream().map(Producto::getId).collect(Collectors.toList())));
        repo.agregar(p);
        return true;
    }

    public boolean actualizar(Producto p) {
        return repo.actualizar(x -> x.getId().equals(p.getId()), p);
    }

    public boolean eliminar(String id) {
        return repo.eliminar(p -> p.getId().equals(id));
    }

    public List<Producto> buscarPorTexto(String texto) {
        String q = texto.toLowerCase().trim();
        return repo.filtrar(p ->
                p.getNombre().toLowerCase().contains(q) ||
                p.getDescripcion().toLowerCase().contains(q) ||
                p.getId().contains(q));
    }

    public List<Producto> filtrarPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank() || "Todas".equals(categoria))
            return listarTodos();
        return repo.filtrar(p -> categoria.equals(p.getCategoria()));
    }

    public List<Producto> getBajoStock() {
        return repo.filtrar(p -> p.getStock() > 0 && p.getStock() <= p.getStockMin());
    }

    public List<Producto> getAgotados() {
        return repo.filtrar(p -> p.getStock() == 0);
    }

    public Set<String> getCategorias() {
        return repo.listarTodos().stream()
                .map(Producto::getCategoria)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public String generarNuevoId() {
        return IdGenerator.siguienteNumerico(
                repo.listarTodos().stream().map(Producto::getId).collect(Collectors.toList()));
    }

    // ── CONTROL DE VENCIMIENTO ──────────────────────────────────────

    /**
     * Devuelve la fecha de hoy en formato ISO (yyyy-MM-dd).
     */
    public static String hoy() {
        return LocalDate.now().toString();
    }

    /**
     * Valida que la fecha de vencimiento sea correcta:
     * - Si requiere vencimiento, la fecha es obligatoria.
     * - La fecha no puede ser anterior a hoy.
     */
    public static String validarFechaVencimiento(boolean requiere, String fecha) {
        if (!requiere) return "OK";
        if (fecha == null || fecha.isBlank()) return "La fecha de vencimiento es obligatoria";
        try {
            LocalDate fv = LocalDate.parse(fecha);
            LocalDate hoy = LocalDate.now();
            if (fv.isBefore(hoy)) return "La fecha de vencimiento no puede ser anterior a hoy";
            return "OK";
        } catch (Exception e) {
            return "Formato de fecha invalido (use yyyy-MM-dd)";
        }
    }

    public List<Producto> getVencidos() {
        LocalDate hoy = LocalDate.now();
        return repo.filtrar(p -> p.isRequiereVencimiento()
                && p.getFechaVencimiento() != null
                && !p.getFechaVencimiento().isBlank()
                && parseFecha(p.getFechaVencimiento()) != null
                && parseFecha(p.getFechaVencimiento()).isBefore(hoy));
    }

    public List<Producto> getPorVencer(int dias) {
        LocalDate hoy    = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        return repo.filtrar(p -> {
            if (!p.isRequiereVencimiento()) return false;
            if (p.getFechaVencimiento() == null || p.getFechaVencimiento().isBlank()) return false;
            LocalDate fv = parseFecha(p.getFechaVencimiento());
            if (fv == null) return false;
            return !fv.isBefore(hoy) && fv.isBefore(limite);
        });
    }

    /**
     * Devuelve los días restantes hasta el vencimiento. Negativo si ya venció.
     */
    public static long diasParaVencer(String fecha) {
        if (fecha == null || fecha.isBlank()) return Long.MAX_VALUE;
        LocalDate fv = parseFecha(fecha);
        if (fv == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), fv);
    }

    private static LocalDate parseFecha(String f) {
        try { return LocalDate.parse(f); }
        catch (Exception e) { return null; }
    }
}
