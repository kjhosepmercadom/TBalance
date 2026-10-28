package sistemainventario.service;

import sistemainventario.model.Producto;
import sistemainventario.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Casos de uso del catálogo de productos: CRUD, búsqueda, filtros y alertas
 * de stock y vencimiento.
 *
 * <p><b>Alta cohesión:</b> todas sus operaciones giran alrededor de los
 * productos. Las preguntas sobre un producto concreto (¿está vencido?, ¿está
 * en bajo stock?) se delegan al propio {@link Producto} (<b>Experto</b>).</p>
 */
public class GestorInventario extends GestorEntidad<Producto> {

    public GestorInventario(Repository<Producto> repositorio) {
        super(repositorio);
    }

    @Override
    protected void validar(Producto producto) {
        producto.validar(LocalDate.now());
    }

    @Override
    protected String generarId() {
        return repositorio.siguienteIdNumerico();
    }

    @Override
    protected String nombreEntidad() {
        return "Producto";
    }

    /**
     * Persiste un cambio de stock sin volver a validar la ficha completa;
     * así un producto ya vencido puede seguir recibiendo movimientos.
     *
     * @param producto producto con el stock modificado
     */
    public void registrarCambioDeStock(Producto producto) {
        repositorio.actualizar(producto);
    }

    /**
     * @param texto texto a buscar; si está vacío se devuelven todos
     * @return productos que coinciden por nombre, descripción, código o ID
     */
    public List<Producto> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        return repositorio.filtrar(producto -> producto.coincideCon(texto));
    }

    /**
     * @param categoria nombre de la categoría; {@code "Todas"} o vacío devuelve todo
     * @return productos de la categoría
     */
    public List<Producto> filtrarPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank() || "Todas".equals(categoria)) {
            return listarTodos();
        }
        return repositorio.filtrar(producto -> producto.perteneceACategoria(categoria));
    }

    /** @return productos con stock en el mínimo o por debajo (sin estar agotados) */
    public List<Producto> getBajoStock() {
        return repositorio.filtrar(Producto::estaBajoStock);
    }

    /** @return productos sin unidades */
    public List<Producto> getAgotados() {
        return repositorio.filtrar(Producto::estaAgotado);
    }

    /** @return productos cuya fecha de vencimiento ya pasó */
    public List<Producto> getVencidos() {
        LocalDate hoy = LocalDate.now();
        return repositorio.filtrar(producto -> producto.estaVencido(hoy));
    }

    /**
     * @param dias ventana de días
     * @return productos que vencen dentro de los próximos {@code dias} días
     */
    public List<Producto> getPorVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        return repositorio.filtrar(producto -> producto.venceDentroDe(dias, hoy));
    }

    /** @return nombres de categorías usadas por los productos, ordenados */
    public Set<String> getCategorias() {
        return listarTodos().stream()
                .map(Producto::getCategoria)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
