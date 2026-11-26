package sistemainventario.repository;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import sistemainventario.model.Sale;

/** Ventas del negocio, en {@code ventas.json}. */
public class SaleRepository extends JsonRepository<Sale> {

    /** Nombre del archivo dentro de la carpeta del negocio. */
    public static final String ARCHIVO = "ventas.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public SaleRepository(Path directorio) {
        super(directorio, ARCHIVO, Sale.class);
        numerarViejas();
    }

    /** La venta nueva recibe el número siguiente (un solo equipo usa los archivos). */
    @Override
    public void agregar(Sale venta) {
        if (venta != null && venta.getNumero() <= 0) {
            venta.setNumero(listarTodos().stream().mapToLong(Sale::getNumero).max().orElse(0) + 1);
        }
        super.agregar(venta);
    }

    /**
     * Las ventas de versiones anteriores no tienen número: se numeran por fecha, de una en una.
     * Queda guardado la próxima vez que se escriba el archivo.
     */
    private void numerarViejas() {
        List<Sale> todas = listarTodos();
        if (todas.stream().noneMatch(v -> v.getNumero() <= 0)) {
            return;
        }
        long siguiente = todas.stream().mapToLong(Sale::getNumero).max().orElse(0) + 1;
        List<Sale> sinNumero = todas.stream().filter(v -> v.getNumero() <= 0)
                .sorted(Comparator.comparing((Sale v) -> v.getFecha() == null ? "" : v.getFecha())
                        .thenComparing(v -> v.getId() == null ? 0 : v.getId().length())
                        .thenComparing(v -> v.getId() == null ? "" : v.getId()))
                .toList();
        for (Sale venta : sinNumero) {
            venta.setNumero(siguiente++);
        }
    }
}
