package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Perfil;
import sistemainventario.model.PerfilNegocio;
import sistemainventario.service.GestorPerfiles;
import sistemainventario.util.JsonUtil;
import sistemainventario.util.TextoUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>GRASP Controller</b> de los casos de uso de perfiles: recibe los
 * eventos de la vista, delega en {@link GestorPerfiles} y avisa a la vista
 * cuando cambia el estado.
 */
public class PerfilController {

    private final AppContext contexto;
    private final NotificadorVista notificador;

    public PerfilController(AppContext contexto, NotificadorVista notificador) {
        this.contexto = contexto;
        this.notificador = notificador;
    }

    public String listarPerfiles() {
        return RespuestaBridge.json(perfiles().listarTodos());
    }

    public String obtenerPerfilActivo() {
        return perfiles().getPerfilActivo().map(RespuestaBridge::json).orElse("{}");
    }

    public String obtenerSugerencias() {
        return RespuestaBridge.json(tipoNegocioActivo().getSugerencias());
    }

    public boolean requiereControlVencimiento() {
        return perfiles().getPerfilActivo().map(Perfil::requiereControlVencimiento).orElse(false);
    }

    public String obtenerTiposNegocio() {
        List<Map<String, Object>> tipos = Arrays.stream(PerfilNegocio.values())
                .map(PerfilNegocio::describir)
                .collect(Collectors.toList());
        return RespuestaBridge.json(tipos);
    }

    public String crearPerfil(String jsonPerfil) {
        return RespuestaBridge.ejecutar(() -> {
            Perfil creado = perfiles().crear(JsonUtil.fromJson(jsonPerfil, Perfil.class));
            notificador.ejecutarScript("onPerfilesActualizados()");
            return RespuestaBridge.json(creado);
        });
    }

    public String editarPerfil(String jsonPerfil) {
        return RespuestaBridge.ejecutarOk(() -> {
            perfiles().actualizar(JsonUtil.fromJson(jsonPerfil, Perfil.class));
            notificador.ejecutarScript("onPerfilesActualizados()");
        });
    }

    public String eliminarPerfil(String id) {
        return RespuestaBridge.ejecutarOk(() -> {
            contexto.eliminarPerfil(id);
            notificador.ejecutarScript("onPerfilesActualizados()");
        });
    }

    public String seleccionarPerfil(String id) {
        String perfilId = "null".equals(id) || TextoUtil.esVacio(id) ? null : id;
        return RespuestaBridge.ejecutarOk(() -> {
            contexto.activarPerfil(perfilId);
            notificador.ejecutarScript("onPerfilSeleccionado(" + RespuestaBridge.literalJs(perfilId) + ")");
        });
    }

    /** @return el tipo de negocio del perfil activo, o {@code NINGUNO} */
    private PerfilNegocio tipoNegocioActivo() {
        return perfiles().getPerfilActivo().map(Perfil::getPerfilNegocio).orElse(PerfilNegocio.NINGUNO);
    }

    private GestorPerfiles perfiles() {
        return contexto.getGestorPerfiles();
    }
}
