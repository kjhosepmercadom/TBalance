package sistemainventario.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración global de la aplicación. Almacena la lista de perfiles
 * existentes y el identificador del perfil activo durante la sesión.
 *
 * Esta clase NO contiene datos de negocio (productos, ventas, etc.);
 * solo persiste el registro de perfiles y el puntero al activo.
 */
public class ConfiguracionGlobal {

    private List<Perfil> perfiles = new ArrayList<>();
    private String       perfilActivoId;
    private String       version;

    public ConfiguracionGlobal() {
        this.version = "2.1.0";
    }

    public List<Perfil> getPerfiles()                  { return perfiles; }
    public void         setPerfiles(List<Perfil> p)    { this.perfiles = p; }
    public String       getPerfilActivoId()            { return perfilActivoId; }
    public void         setPerfilActivoId(String id)   { this.perfilActivoId = id; }
    public String       getVersion()                   { return version; }
    public void         setVersion(String v)           { this.version = v; }

    public Perfil getPerfilActivo() {
        if (perfilActivoId == null) return null;
        return perfiles.stream()
                .filter(p -> perfilActivoId.equals(p.getId()))
                .findFirst().orElse(null);
    }
}
