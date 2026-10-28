package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.service.GestorRespaldo;

/**
 * <b>GRASP Controller</b> de los casos de uso de copias de seguridad.
 *
 * <p>Las copias no dependen del perfil activo: incluyen todos los perfiles.</p>
 */
public class RespaldoController {

    private final AppContext contexto;
    private final NotificadorVista notificador;

    public RespaldoController(AppContext contexto, NotificadorVista notificador) {
        this.contexto = contexto;
        this.notificador = notificador;
    }

    public String listarRespaldos() {
        return RespuestaBridge.ejecutar(() -> RespuestaBridge.json(respaldos().listar()));
    }

    public String crearRespaldo() {
        return RespuestaBridge.ejecutar(() -> RespuestaBridge.json(respaldos().crear()));
    }

    /**
     * Restaura un respaldo y pide a la vista que recargue todos los datos.
     *
     * @param nombre archivo .zip
     * @return JSON con la copia automática creada antes de restaurar, o un error
     */
    public String restaurarRespaldo(String nombre) {
        return RespuestaBridge.ejecutar(() -> {
            String copiaPrevia = RespuestaBridge.json(contexto.restaurarRespaldo(nombre));
            notificador.ejecutarScript("onPerfilesActualizados()");
            return copiaPrevia;
        });
    }

    public String eliminarRespaldo(String nombre) {
        return RespuestaBridge.ejecutarOk(() -> respaldos().eliminar(nombre));
    }

    public String obtenerCarpetaRespaldos() {
        return respaldos().getCarpetaRespaldos().toAbsolutePath().toString();
    }

    private GestorRespaldo respaldos() {
        return contexto.getGestorRespaldo();
    }
}
