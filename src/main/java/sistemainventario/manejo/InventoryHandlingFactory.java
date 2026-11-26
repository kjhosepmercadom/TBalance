package sistemainventario.manejo;

import sistemainventario.model.ManejoProducto;
import sistemainventario.model.Product;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Entrega la estrategia de cada manejo. Para agregar uno nuevo (lotes, seriales)
 * se registra aquí; el resto del sistema no cambia.
 */
public final class InventoryHandlingFactory {

    private static final Map<ManejoProducto, InventoryHandling> ESTRATEGIAS = new EnumMap<>(ManejoProducto.class);

    static {
        for (InventoryHandling estrategia : List.of(new SimpleHandling(), new VariantHandling())) {
            ESTRATEGIAS.put(estrategia.tipo(), estrategia);
        }
    }

    private InventoryHandlingFactory() {
    }

    /**
     * @param manejo cómo se controla el producto
     * @return su estrategia (la simple si no hay una registrada)
     */
    public static InventoryHandling para(ManejoProducto manejo) {
        return ESTRATEGIAS.getOrDefault(manejo == null ? ManejoProducto.SIMPLE : manejo, ESTRATEGIAS.get(ManejoProducto.SIMPLE));
    }

    /**
     * @param producto producto
     * @return la estrategia que lo maneja
     */
    public static InventoryHandling para(Product producto) {
        return para(producto.getManejo());
    }
}
