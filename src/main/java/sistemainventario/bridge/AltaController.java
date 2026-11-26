package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.PoliticaPrecio;
import sistemainventario.model.Profile;
import sistemainventario.model.RedondeoPrecio;
import sistemainventario.service.AltaAgilService;
import sistemainventario.service.ImportadorProductos;
import sistemainventario.service.ProfileServices;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>GRASP Controller</b> del registro ágil de productos (v3.8.0): precio sugerido,
 * reconocer un código, partir de un producto parecido y cargar una lista pegada.
 *
 * <p>Los números llegan como texto desde la página y se leen como se escriben en
 * Colombia ({@code 52.000}, {@code 89,5}).</p>
 */
public class AltaController {

    private final AppContext contexto;

    /** @param contexto contexto de la aplicación */
    public AltaController(AppContext contexto) {
        this.contexto = contexto;
    }

    /**
     * @param costo       precio de compra por unidad
     * @param costoOculto empaque, bolsa o flete por unidad (vacío = 0)
     * @return precio sugerido con el margen del negocio y las opciones de los botones
     */
    public String sugerirPrecio(String costo, String costoOculto) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(
                servicio().sugerirPrecio(numero(costo, "El costo"), numeroOCero(costoOculto, "El costo oculto"))));
    }

    /**
     * @param codigo código leído por el lector o escrito a mano
     * @return el producto que ya tiene ese código, o {@code null} si es nuevo
     */
    public String buscarProductoPorCodigo(String codigo) {
        return BridgeResponse.ejecutar(() -> servicio().buscarPorCodigo(codigo)
                .map(BridgeResponse::json).orElse("null"));
    }

    /**
     * @param idProducto producto del que se parte
     * @return un producto nuevo (sin ID, sin código, con el stock en cero) para llenar el formulario
     */
    public String plantillaDesdeProducto(String idProducto) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(servicio().plantillaDesde(idProducto)));
    }

    /**
     * @param texto lista pegada desde Excel
     * @return qué se cargaría, qué ya existe y qué tiene error; no guarda nada
     */
    public String revisarListaProductos(String texto) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(servicio().revisar(texto)));
    }

    /**
     * @param texto lista pegada desde Excel
     * @return lo que se cargó y lo que se saltó, fila por fila
     */
    public String importarListaProductos(String texto) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(servicio().importar(texto)));
    }

    /** @return margen, redondeo y opciones de redondeo del negocio activo */
    public String obtenerPoliticaPrecio() {
        return BridgeResponse.ejecutar(() -> {
            PoliticaPrecio politica = perfilActivo().getPoliticaPrecio();
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("margenPct", politica.margenPct());
            datos.put("redondeo", politica.redondeo().name());
            Map<String, String> opciones = new LinkedHashMap<>();
            for (RedondeoPrecio r : RedondeoPrecio.values()) {
                opciones.put(r.name(), r.getEtiqueta());
            }
            datos.put("opcionesRedondeo", opciones);
            datos.put("margenesRapidos", AltaAgilService.MARGENES_RAPIDOS);
            return BridgeResponse.json(datos);
        });
    }

    /**
     * @param margen   porcentaje de ganancia sobre el precio (1 a 90)
     * @param redondeo nombre de la regla de redondeo
     * @return OK o el error
     */
    public String guardarPoliticaPrecio(String margen, String redondeo) {
        return BridgeResponse.ejecutarOk(() -> {
            Double pct = ImportadorProductos.numero(margen);
            if (pct == null) {
                throw new ValidationException("El margen debe ser un número entre 1 y 90");
            }
            RedondeoPrecio regla = RedondeoPrecio.desde(redondeo, null);
            if (regla == null) {
                throw new ValidationException("Elija cómo terminan los precios");
            }
            contexto.getGestorPerfiles().guardarPoliticaPrecio(contexto.getPerfilActivoId(),
                    new PoliticaPrecio(pct, regla));
        });
    }

    private AltaAgilService servicio() {
        ProfileServices servicios = contexto.requerirServiciosActivos();
        return new AltaAgilService(servicios.inventario(), servicios.categorias(),
                () -> perfilActivo().getPoliticaPrecio());
    }

    private Profile perfilActivo() {
        return contexto.getGestorPerfiles().getPerfilActivo()
                .orElseThrow(() -> new ValidationException("Elija un negocio primero"));
    }

    private static double numero(String texto, String campo) {
        Double valor = ImportadorProductos.numero(texto);
        if (valor == null) {
            throw new ValidationException(campo + " debe ser un número");
        }
        return valor;
    }

    private static double numeroOCero(String texto, String campo) {
        return texto == null || texto.isBlank() ? 0 : numero(texto, campo);
    }
}
