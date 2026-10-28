package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Producto;
import sistemainventario.service.GestorEtiquetas;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>GRASP Controller</b> de los casos de uso de etiquetas con código QR.
 */
public class EtiquetaController {

    private final AppContext contexto;
    private final AbridorArchivos abridor;

    public EtiquetaController(AppContext contexto, AbridorArchivos abridor) {
        this.contexto = contexto;
        this.abridor = abridor;
    }

    /**
     * @param productoId ID del producto
     * @return JSON con la imagen de la etiqueta y el texto que contiene el QR
     */
    public String obtenerEtiquetaQR(String productoId) {
        return RespuestaBridge.ejecutar(() -> {
            Producto producto = producto(productoId);
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("imagen", etiquetas().vistaPrevia(producto));
            respuesta.put("contenido", producto.contenidoEtiquetaQR());
            return RespuestaBridge.json(respuesta);
        });
    }

    /**
     * Guarda la etiqueta como PNG y la abre con el visor de imágenes.
     *
     * @param productoId ID del producto
     * @return JSON con la ruta del archivo creado
     */
    public String guardarEtiquetaQR(String productoId) {
        return RespuestaBridge.ejecutar(() -> {
            Path archivo = etiquetas().guardarEtiqueta(producto(productoId));
            abridor.abrir(archivo);
            return RespuestaBridge.json(Map.of("ruta", archivo.toAbsolutePath().toString()));
        });
    }

    /**
     * Genera las hojas de etiquetas de todos los productos del perfil activo
     * y abre la carpeta donde quedaron.
     *
     * @return JSON con la cantidad de hojas y la carpeta
     */
    public String generarHojaEtiquetas() {
        return RespuestaBridge.ejecutar(() -> {
            List<Producto> productos = contexto.requerirServiciosActivos().inventario().listarTodos();
            List<Path> hojas = etiquetas().guardarHojas(productos);
            abridor.abrir(etiquetas().getCarpeta());
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("hojas", hojas.size());
            respuesta.put("etiquetas", productos.size());
            respuesta.put("carpeta", etiquetas().getCarpeta().toAbsolutePath().toString());
            return RespuestaBridge.json(respuesta);
        });
    }

    private Producto producto(String productoId) {
        return contexto.requerirServiciosActivos().inventario().obtener(productoId);
    }

    private GestorEtiquetas etiquetas() {
        return contexto.getGestorEtiquetas();
    }
}
