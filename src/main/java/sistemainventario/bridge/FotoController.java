package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.service.FotoService;
import sistemainventario.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * <b>GRASP Controller</b> de las fotos de producto (v3.9.0). La página manda la foto ya
 * reducida; las reglas (tipo real, peso, cuota) las aplica {@link FotoService}.
 *
 * <p>Las fotos no viajan con la lista de productos: la página pide aquí, en una sola
 * llamada, <i>cuáles</i> productos tienen foto, y trae cada imagen solo cuando la va a mostrar.</p>
 */
public class FotoController {

    private final AppContext contexto;

    /** @param contexto contexto de la aplicación */
    public FotoController(AppContext contexto) {
        this.contexto = contexto;
    }

    /**
     * @return si el negocio usa fotos, qué productos tienen y cuánto espacio ocupan
     */
    public String obtenerEstadoFotos() {
        return BridgeResponse.ejecutar(() -> {
            Map<String, Object> estado = new LinkedHashMap<>();
            FotoService fotos = contexto.getFotos();
            boolean usa = fotos.estanHabilitadas();
            estado.put("habilitadas", usa);
            estado.put("ids", usa ? new TreeSet<>(fotos.idsConFoto()) : java.util.List.of());
            estado.put("bytes", usa ? fotos.bytesUsados() : 0);
            estado.put("cuotaBytes", FotoService.CUOTA_BYTES);
            estado.put("maximoBytes", FotoService.BYTES_MAXIMOS);
            return BridgeResponse.json(estado);
        });
    }

    /**
     * @param productoId producto
     * @return la foto como "data:image/...;base64,..." o texto vacío si no tiene
     */
    public String obtenerFotoProducto(String productoId) {
        return BridgeResponse.ejecutar(() -> contexto.getFotos().obtener(productoId).orElse(""));
    }

    /**
     * @param productoId producto
     * @param dataUrl    foto reducida por la página
     * @return OK o el error
     */
    public String guardarFotoProducto(String productoId, String dataUrl) {
        return BridgeResponse.ejecutarOk(() -> contexto.getFotos().guardar(productoId, dataUrl));
    }

    /** Texto con que empieza la respuesta cuando el producto se guardó pero la foto no. */
    public static final String PRODUCTO_SIN_FOTO = "OK_SIN_FOTO: ";

    /**
     * Guarda el producto y, en el mismo paso, su foto. Así un producto nuevo recibe la foto
     * con el ID que acaba de tener, sin que la página tenga que adivinarlo.
     *
     * @param jsonProducto producto en JSON (igual que {@code guardarProducto})
     * @param foto         vacío = no tocar la foto; {@code QUITAR} = quitarla; o la foto reducida como data URL
     * @return {@code OK}; {@code OK_SIN_FOTO: motivo} si el producto se guardó pero la foto no; o el error
     */
    public String guardarProductoConFoto(String jsonProducto, String foto) {
        return BridgeResponse.ejecutar(() -> {
            Product guardado = contexto.requerirServiciosActivos().inventario()
                    .guardar(JsonUtil.fromJson(jsonProducto, Product.class));
            if (foto == null || foto.isBlank() || "null".equals(foto)) {
                return BridgeResponse.OK;
            }
            try {
                FotoService servicio = contexto.getFotos();
                if ("QUITAR".equals(foto)) {
                    servicio.eliminar(guardado.getId());
                } else {
                    servicio.guardar(guardado.getId(), foto);
                }
            } catch (ValidationException e) {
                return PRODUCTO_SIN_FOTO + e.getMessage();
            }
            return BridgeResponse.OK;
        });
    }

    /**
     * @param productoId producto
     * @return OK o el error
     */
    public String quitarFotoProducto(String productoId) {
        return BridgeResponse.ejecutarOk(() -> contexto.getFotos().eliminar(productoId));
    }

    /**
     * Enciende o apaga las fotos del negocio abierto. Apagarlas no borra las que ya hay.
     *
     * @param usar {@code "true"} para usar fotos
     * @return OK o el error
     */
    public String usarFotosProducto(String usar) {
        return BridgeResponse.ejecutarOk(() -> {
            String perfilId = contexto.getPerfilActivoId();
            if (perfilId == null) {
                throw new ValidationException("Elija un negocio primero");
            }
            contexto.getGestorPerfiles().guardarUsoDeFotos(perfilId, Boolean.parseBoolean(usar));
        });
    }
}
