package sistemainventario.manejo;

import sistemainventario.model.ManejoProducto;

import java.util.List;
import java.util.Map;

/**
 * Qué muestra la ventana "Agregar / Editar producto" para un manejo.
 *
 * <p>La página no pregunta "¿es una boutique?": pinta el formulario a partir
 * de esta descripción, que arma cada estrategia. Así, un manejo nuevo trae su
 * propio formulario sin cambiar el JavaScript de las demás.</p>
 *
 * @param manejo                 manejo descrito
 * @param etiqueta               nombre para el selector "¿Cómo se controla este producto?"
 * @param stockUnico             muestra los campos Stock y Stock mínimo
 * @param matrizVariantes        muestra la tabla de tallas y colores
 * @param vencimientoVisible     muestra el control de fecha de vencimiento
 * @param vencimientoObligatorio la fecha es obligatoria (no se puede apagar)
 * @param unidadVisible          muestra el selector de unidad de medida
 * @param atajosTallas           grupos de tallas para llenar la tabla con un clic
 */
public record FormularioProducto(ManejoProducto manejo, String etiqueta, boolean stockUnico, boolean matrizVariantes,
                                 boolean vencimientoVisible, boolean vencimientoObligatorio, boolean unidadVisible,
                                 Map<String, List<String>> atajosTallas) {
}
