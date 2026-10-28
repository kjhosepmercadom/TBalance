package sistemainventario.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tipos de negocio permitidos en el sistema.
 *
 * Cada tipo declara:
 * <ul>
 *   <li>Sus categorías predefinidas</li>
 *   <li>Si requiere control de fechas de vencimiento
 *       (solo Farmacia, Supermercado, Tienda de barrio)</li>
 *   <li>Sugerencias de placeholders para los formularios</li>
 * </ul>
 *
 * Elegir un tipo es OPCIONAL (NINGUNO = sin categorías predefinidas).
 */
public enum PerfilNegocio {

    BOUTIQUE("Boutique",
            new String[]{"Camisas", "Vestidos", "Pantalones", "Zapatos", "Accesorios"},
            true,
            new String[]{"Blusa Casual Mujer", "Ropa", "Moda Fashion"}),

    FARMACIA("Farmacia",
            new String[]{"Medicamentos", "Vitaminas", "Higiene", "Cosmeticos", "Primeros Auxilios"},
            true,
            new String[]{"Acetaminofen 500 mg", "Medicamentos", "Laboratorios XYZ"}),

    FERRETERIA("Ferreteria",
            new String[]{"Herramientas", "Pinturas", "Electricidad", "Plomeria", "Tornilleria"},
            true,
            new String[]{"Martillo de acero", "Herramientas", "FerreDistribuciones"}),

    SUPERMERCADO("Supermercado",
            new String[]{"Frutas", "Verduras", "Carnes", "Lacteos", "Bebidas", "Aseo"},
            true,
            new String[]{"Leche Entera 1L", "Lacteos", "Alimentos Nacionales"}),

    TIENDA_BARRIO("Tienda de barrio",
            new String[]{"Bebidas", "Lacteos", "Snacks", "Dulces", "Aseo"},
            true,
            new String[]{"Arroz Diana 500g", "Abarrotes", "Distribuidora Local"}),

    TECNOLOGIA("Tecnologia",
            new String[]{"Computadores", "Celulares", "Accesorios", "Audio", "Gaming"},
            true,
            new String[]{"Portatil HP 15", "Laptops", "Distribuidora Tecnologica"}),

    NINGUNO("Personalizado (sin categorías predefinidas)",
            new String[0],
            true,
            new String[]{"", "", ""});

    private final String etiqueta;
    private final List<String> categoriasPredefinidas;
    private final boolean requiereVencimiento;
    private final String[] sugerencias;

    PerfilNegocio(String etiqueta, String[] categorias, boolean requiereVencimiento, String[] sugerencias) {
        this.etiqueta = etiqueta;
        this.categoriasPredefinidas = Collections.unmodifiableList(Arrays.asList(categorias));
        this.requiereVencimiento    = requiereVencimiento;
        this.sugerencias            = sugerencias;
    }

    public String getEtiqueta() { return etiqueta; }

    public List<String> getCategoriasPredefinidas() {
        return categoriasPredefinidas;
    }

    public boolean tieneCategorias() {
        return !categoriasPredefinidas.isEmpty();
    }

    /**
     * Indica si para este tipo de negocio el sistema debe gestionar
     * fechas de vencimiento en los productos.
     */
    public boolean requiereControlVencimiento() {
        return requiereVencimiento;
    }

    /**
     * Devuelve un mapa con sugerencias de placeholders para los formularios
     * según el tipo de negocio. Las claves son: producto, categoria, proveedor.
     */
    public Map<String, String> getSugerencias() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("producto",  sugerencias.length > 0 ? sugerencias[0] : "");
        map.put("categoria", sugerencias.length > 1 ? sugerencias[1] : "");
        map.put("proveedor", sugerencias.length > 2 ? sugerencias[2] : "");
        return map;
    }

    public static PerfilNegocio desdeClave(String clave) {
        if (clave == null || clave.isBlank()) return NINGUNO;
        for (PerfilNegocio p : values()) {
            if (p.name().equalsIgnoreCase(clave) || p.etiqueta.equalsIgnoreCase(clave))
                return p;
        }
        return NINGUNO;
    }
}
