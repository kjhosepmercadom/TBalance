package sistemainventario.model;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lo que maneja un negocio (v3.6.0): es lo que gobierna el polimorfismo.
 *
 * <p>El tipo de negocio solo pone los valores iniciales
 * ({@link BusinessType#capacidadesPorDefecto()}); el dueño los puede cambiar al
 * editar el negocio. Se guardan en la configuración del negocio
 * (PERFIL_CONFIGURACION en la base) con las claves {@code cap.*}, así que no
 * necesitan tabla propia.</p>
 *
 * <p>Las capacidades no deciden por sí solas cómo se vende cada producto: dicen
 * qué {@link ManejoProducto manejos} están permitidos. Cada producto elige el
 * suyo y la estrategia de ese manejo hace el trabajo.</p>
 *
 * @param variantes    maneja tallas y colores
 * @param vencimiento  cuánto controla las fechas de vencimiento
 * @param fraccion     vende por metro, kilo o litro (cantidades con decimales)
 * @param seriales     registra serial o IMEI y garantía (llega en la fase 3; hoy solo se guarda)
 * @param ventaRapida  escanear y cobrar: la venta agrega 1 unidad y deja el foco en el lector
 */
public record BusinessCapabilities(boolean variantes, ControlVencimiento vencimiento, boolean fraccion,
                                   boolean seriales, boolean ventaRapida) {

    public static final String CLAVE_VARIANTES = "cap.variantes";
    public static final String CLAVE_VENCIMIENTO = "cap.vencimiento";
    public static final String CLAVE_FRACCION = "cap.fraccion";
    public static final String CLAVE_SERIALES = "cap.seriales";
    public static final String CLAVE_VENTA_RAPIDA = "cap.ventaRapida";

    public BusinessCapabilities {
        vencimiento = vencimiento == null ? ControlVencimiento.OPCIONAL : vencimiento;
    }

    /** @return los manejos que pueden usar los productos de este negocio ({@code SIMPLE} siempre) */
    public Set<ManejoProducto> manejosPermitidos() {
        Set<ManejoProducto> permitidos = EnumSet.of(ManejoProducto.SIMPLE);
        if (variantes) {
            permitidos.add(ManejoProducto.VARIANTES);
        }
        return permitidos;
    }

    /** @return las unidades de medida que puede elegir un producto */
    public Set<UnidadMedida> unidadesPermitidas() {
        if (fraccion) {
            return EnumSet.allOf(UnidadMedida.class);
        }
        return EnumSet.of(UnidadMedida.UNIDAD, UnidadMedida.CAJA);
    }

    /**
     * @param config configuración guardada del negocio
     * @return {@code true} si ya tiene sus capacidades guardadas
     */
    public static boolean estanGuardadas(Map<String, String> config) {
        return config != null && config.containsKey(CLAVE_VARIANTES);
    }

    /**
     * Lee las capacidades de la configuración; lo que falte lo toma de los valores por defecto.
     *
     * @param config  configuración del negocio (claves {@code cap.*})
     * @param defecto valores del tipo de negocio
     * @return las capacidades
     */
    public static BusinessCapabilities desde(Map<String, String> config, BusinessCapabilities defecto) {
        if (config == null) {
            return defecto;
        }
        return new BusinessCapabilities(
                logico(config.get(CLAVE_VARIANTES), defecto.variantes()),
                ControlVencimiento.desdeTexto(config.get(CLAVE_VENCIMIENTO), defecto.vencimiento()),
                logico(config.get(CLAVE_FRACCION), defecto.fraccion()),
                logico(config.get(CLAVE_SERIALES), defecto.seriales()),
                logico(config.get(CLAVE_VENTA_RAPIDA), defecto.ventaRapida()));
    }

    /** @return las capacidades como claves {@code cap.*} para guardarlas */
    public Map<String, String> comoConfiguracion() {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put(CLAVE_VARIANTES, String.valueOf(variantes));
        mapa.put(CLAVE_VENCIMIENTO, vencimiento.name());
        mapa.put(CLAVE_FRACCION, String.valueOf(fraccion));
        mapa.put(CLAVE_SERIALES, String.valueOf(seriales));
        mapa.put(CLAVE_VENTA_RAPIDA, String.valueOf(ventaRapida));
        return mapa;
    }

    /**
     * @param vencimientoNuevo otro control de vencimiento
     * @return una copia con ese control
     */
    public BusinessCapabilities conVencimiento(ControlVencimiento vencimientoNuevo) {
        return new BusinessCapabilities(variantes, vencimientoNuevo, fraccion, seriales, ventaRapida);
    }

    private static boolean logico(String texto, boolean defecto) {
        return texto == null || texto.isBlank() ? defecto : Boolean.parseBoolean(texto.trim());
    }
}
