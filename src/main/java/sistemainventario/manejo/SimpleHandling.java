package sistemainventario.manejo;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.BusinessCapabilities;
import sistemainventario.model.ControlVencimiento;
import sistemainventario.model.ManejoProducto;
import sistemainventario.model.Product;
import sistemainventario.util.QuantityUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manejo {@link ManejoProducto#SIMPLE}: un solo stock para todo el producto.
 */
public class SimpleHandling implements InventoryHandling {

    @Override
    public ManejoProducto tipo() {
        return ManejoProducto.SIMPLE;
    }

    @Override
    public FormularioProducto formulario(BusinessCapabilities capacidades) {
        return new FormularioProducto(tipo(), "Simple (un solo stock)", true, false,
                capacidades.vencimiento() != ControlVencimiento.NINGUNO, capacidades.vencimiento() == ControlVencimiento.OBLIGATORIO,
                capacidades.fraccion(), Map.of());
    }

    @Override
    public void validar(Product producto, BusinessCapabilities capacidades) {
        if (!capacidades.fraccion() && producto.getUnidadMedida() != null
                && producto.getUnidadMedida().isFraccionable()) {
            throw new ValidationException("Este negocio no vende fracciones. Elija la unidad UND o CAJA.");
        }
        producto.setVariantes(new ArrayList<>());
        if (QuantityUtil.esNegativa(producto.getStock()) || QuantityUtil.esNegativa(producto.getStockMin())) {
            throw new ValidationException("El stock no puede ser negativo");
        }
    }

    @Override
    public List<StockLine> reservar(Product producto, String varianteId, BigDecimal cantidad) {
        producto.verificarStockDisponible(cantidad);
        return List.of(StockLine.de(producto.getId(), null, cantidad));
    }

    @Override
    public void descontar(Product producto, List<StockLine> lineas) {
        for (StockLine linea : lineas) {
            producto.disminuirStock(linea.cantidad());
        }
    }

    @Override
    public void reponer(Product producto, StockLine linea) {
        producto.aumentarStock(linea.cantidad());
    }
}
