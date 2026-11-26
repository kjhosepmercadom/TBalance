package sistemainventario.manejo;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.BusinessCapabilities;
import sistemainventario.model.ControlVencimiento;
import sistemainventario.model.ManejoProducto;
import sistemainventario.model.Product;
import sistemainventario.model.ProductVariant;
import sistemainventario.util.IdGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejo {@link ManejoProducto#VARIANTES}: stock por talla y color (Boutique).
 *
 * <p>El stock del producto es la suma de sus variantes activas; por eso cada
 * cambio pasa por {@link Product#sincronizarStockConVariantes()}.</p>
 */
public class VariantHandling implements InventoryHandling {

    private static final Map<String, List<String>> ATAJOS_TALLAS = Map.of(
            "Ropa", List.of("XS", "S", "M", "L", "XL", "XXL"),
            "Calzado", List.of("36", "37", "38", "39", "40", "41", "42"));

    @Override
    public ManejoProducto tipo() {
        return ManejoProducto.VARIANTES;
    }

    @Override
    public FormularioProducto formulario(BusinessCapabilities capacidades) {
        return new FormularioProducto(tipo(), "Con tallas y colores", false, true,
                capacidades.vencimiento() != ControlVencimiento.NINGUNO, capacidades.vencimiento() == ControlVencimiento.OBLIGATORIO,
                capacidades.fraccion(), new LinkedHashMap<>(ATAJOS_TALLAS));
    }

    @Override
    public void validar(Product producto, BusinessCapabilities capacidades) {
        if (!capacidades.fraccion() && producto.getUnidadMedida() != null
                && producto.getUnidadMedida().isFraccionable()) {
            throw new ValidationException("Este negocio no vende fracciones. Elija la unidad UND o CAJA.");
        }
        if (!capacidades.variantes()) {
            throw new ValidationException("Este negocio no maneja tallas ni colores. Actívelo en Editar negocio.");
        }
        for (ProductVariant variante : producto.getVariantes()) {
            if (variante.getId() == null || variante.getId().isBlank()) {
                variante.setId(IdGenerator.conPrefijo("VAR"));
            }
            if (variante.getTalla().isEmpty() && variante.getColor().isEmpty()) {
                throw new ValidationException("Cada fila necesita una talla o un color.");
            }
        }
        List<ProductVariant> activas = producto.variantesActivas();
        if (activas.isEmpty()) {
            throw new ValidationException("Agregue al menos una talla o color con su stock.");
        }
        for (int i = 0; i < activas.size(); i++) {
            for (int j = i + 1; j < activas.size(); j++) {
                if (activas.get(i).mismaCombinacion(activas.get(j))) {
                    throw new ValidationException("La talla y color \"" + activas.get(i).getDescripcion()
                            + "\" está repetida.");
                }
            }
        }
        producto.sincronizarStockConVariantes();
    }

    @Override
    public List<StockLine> reservar(Product producto, String varianteId, BigDecimal cantidad) {
        if (varianteId == null || varianteId.isBlank()) {
            throw new ValidationException("Elija la talla o color de " + producto.getNombre() + ".");
        }
        ProductVariant variante = producto.buscarVariante(varianteId)
                .filter(ProductVariant::isActivo)
                .orElseThrow(() -> new ValidationException("La talla o color elegido ya no existe en "
                        + producto.getNombre() + "."));
        if (!variante.tieneStockPara(cantidad)) {
            throw new ValidationException("Stock insuficiente para " + producto.getNombre()
                    + " (" + variante.getDescripcion() + ").");
        }
        return List.of(StockLine.de(producto.getId(), variante.getId(), cantidad));
    }

    @Override
    public void descontar(Product producto, List<StockLine> lineas) {
        for (StockLine linea : lineas) {
            ProductVariant variante = varianteDe(producto, linea);
            variante.disminuirStock(linea.cantidad(), producto.getNombre());
        }
        producto.sincronizarStockConVariantes();
    }

    @Override
    public void reponer(Product producto, StockLine linea) {
        varianteDe(producto, linea).aumentarStock(linea.cantidad());
        producto.sincronizarStockConVariantes();
    }

    private static ProductVariant varianteDe(Product producto, StockLine linea) {
        if (linea.varianteId() == null) {
            throw new ValidationException("Elija la talla o color de " + producto.getNombre() + ".");
        }
        return producto.buscarVariante(linea.varianteId()).orElseThrow(() ->
                new ValidationException("No se encontró la talla o color de " + producto.getNombre() + "."));
    }
}
