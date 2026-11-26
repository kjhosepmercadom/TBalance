package sistemainventario.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tipos de negocio que puede tener un {@link Profile}.
 *
 * <p>Cada tipo declara sus categorías predefinidas, sus capacidades iniciales
 * ({@link BusinessCapabilities}: tallas, vencimiento, fracción...) y textos de
 * ejemplo para los formularios.</p>
 *
 * <p><b>OCP:</b> agregar un tipo de negocio nuevo consiste en declarar una
 * constante más; ni los servicios ni la vista necesitan cambiar porque
 * consumen la información a través de {@link #describir()}.</p>
 */
public enum BusinessType {

    BOUTIQUE("Boutique",
            List.of("Camisas", "Vestidos", "Pantalones", "Zapatos", "Accesorios"),
            new BusinessCapabilities(true, ControlVencimiento.NINGUNO, false, false, false),
            new Suggestions("Blusa Casual Mujer", "Ropa", "Moda Fashion")),

    FARMACIA("Farmacia",
            List.of("Medicamentos", "Vitaminas", "Higiene", "Cosmeticos", "Primeros Auxilios"),
            new BusinessCapabilities(false, ControlVencimiento.OBLIGATORIO, false, false, false),
            new Suggestions("Acetaminofen 500 mg", "Medicamentos", "Laboratorios XYZ")),

    FERRETERIA("Ferreteria",
            List.of("Herramientas", "Pinturas", "Electricidad", "Plomeria", "Tornilleria"),
            new BusinessCapabilities(false, ControlVencimiento.NINGUNO, true, false, false),
            new Suggestions("Martillo de acero", "Herramientas", "FerreDistribuciones")),

    SUPERMERCADO("Supermercado",
            List.of("Frutas", "Verduras", "Carnes", "Lacteos", "Bebidas", "Aseo"),
            new BusinessCapabilities(false, ControlVencimiento.OPCIONAL, true, false, true),
            new Suggestions("Leche Entera 1L", "Lacteos", "Alimentos Nacionales")),

    TIENDA_BARRIO("Tienda de barrio",
            List.of("Bebidas", "Lacteos", "Snacks", "Dulces", "Aseo"),
            new BusinessCapabilities(false, ControlVencimiento.OPCIONAL, true, false, true),
            new Suggestions("Arroz Diana 500g", "Abarrotes", "Distribuidora Local")),

    TECNOLOGIA("Tecnologia",
            List.of("Computadores", "Celulares", "Accesorios", "Audio", "Gaming"),
            new BusinessCapabilities(false, ControlVencimiento.NINGUNO, false, true, false),
            new Suggestions("Portatil HP 15", "Laptops", "Distribuidora Tecnologica")),

    NINGUNO("Personalizado (sin categorías predefinidas)",
            List.of(),
            new BusinessCapabilities(false, ControlVencimiento.OPCIONAL, false, false, false),
            new Suggestions("", "", ""));

    private final String etiqueta;
    private final List<String> categoriasPredefinidas;
    private final BusinessCapabilities capacidades;
    private final Suggestions sugerencias;

    BusinessType(String etiqueta, List<String> categorias, BusinessCapabilities capacidades, Suggestions sugerencias) {
        this.etiqueta = etiqueta;
        this.categoriasPredefinidas = Collections.unmodifiableList(categorias);
        this.capacidades = capacidades;
        this.sugerencias = sugerencias;
    }

    /**
     * Busca un tipo por su nombre de constante o por su etiqueta.
     *
     * @param clave por ejemplo {@code "FARMACIA"} o {@code "Farmacia"}
     * @return el tipo encontrado, o {@link #NINGUNO} si no existe
     */
    public static BusinessType desdeClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return NINGUNO;
        }
        return Arrays.stream(values())
                .filter(tipo -> tipo.name().equalsIgnoreCase(clave) || tipo.etiqueta.equalsIgnoreCase(clave))
                .findFirst()
                .orElse(NINGUNO);
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public List<String> getCategoriasPredefinidas() {
        return categoriasPredefinidas;
    }

    public boolean tieneCategorias() {
        return !categoriasPredefinidas.isEmpty();
    }

    /**
     * <b>v3.6.0:</b> lo que maneja un negocio de este tipo cuando se crea
     * (tallas en Boutique, fracción en Ferretería, vencimiento obligatorio en
     * Farmacia...). El dueño lo puede cambiar al editar el negocio.
     *
     * @return las capacidades iniciales
     */
    public BusinessCapabilities capacidadesPorDefecto() {
        return capacidades;
    }

    /** @return mapa con las claves {@code producto}, {@code categoria} y {@code proveedor} */
    public Map<String, String> getSugerencias() {
        return sugerencias.comoMapa();
    }

    /**
     * <b>GRASP Experto:</b> el tipo de negocio sabe describirse para la vista.
     *
     * @return mapa con clave, etiqueta, categorías y si exige vencimiento
     */
    public Map<String, Object> describir() {
        Map<String, Object> descripcion = new LinkedHashMap<>();
        descripcion.put("clave", name());
        descripcion.put("etiqueta", etiqueta);
        descripcion.put("categorias", categoriasPredefinidas);
        descripcion.put("vencimiento", capacidades.vencimiento() != ControlVencimiento.NINGUNO);
        descripcion.put("capacidades", capacidades);
        return descripcion;
    }

    /** Textos de ejemplo (placeholders) para los formularios de la vista. */
    private record Suggestions(String producto, String categoria, String proveedor) {

        Map<String, String> comoMapa() {
            Map<String, String> mapa = new LinkedHashMap<>();
            mapa.put("producto", producto);
            mapa.put("categoria", categoria);
            mapa.put("proveedor", proveedor);
            return mapa;
        }
    }
}
