package sistemainventario.service;

import sistemainventario.model.ConfiguracionGlobal;
import sistemainventario.model.Perfil;
import sistemainventario.repository.ConfiguracionGlobalRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lectura y actualización de la {@link ConfiguracionGlobal}.
 *
 * <p><b>SRP:</b> solo se ocupa del puntero al perfil activo y de mantener
 * sincronizada la lista de perfiles en el archivo de configuración. En P2,
 * {@code GestorPerfiles} también escribía directamente este archivo.</p>
 */
public class GestorConfiguracionGlobal {

    private final ConfiguracionGlobalRepository repositorio;

    public GestorConfiguracionGlobal(ConfiguracionGlobalRepository repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    /** @return {@code true} si aún no se ha creado ningún perfil */
    public boolean esPrimerArranque() {
        return !repositorio.existe() || !repositorio.cargar().tienePerfiles();
    }

    /** @return ID del perfil activo, si hay uno */
    public Optional<String> getPerfilActivoId() {
        ConfiguracionGlobal configuracion = repositorio.cargar();
        return configuracion.tienePerfilActivo()
                ? Optional.of(configuracion.getPerfilActivoId())
                : Optional.empty();
    }

    /**
     * @param perfilId ID del nuevo perfil activo, o {@code null} para ninguno
     */
    public void establecerPerfilActivo(String perfilId) {
        ConfiguracionGlobal configuracion = repositorio.cargar();
        configuracion.setPerfilActivoId(perfilId);
        repositorio.guardar(configuracion);
    }

    /**
     * Copia la lista de perfiles al archivo de configuración (estructura de P2).
     *
     * @param perfiles lista actual de perfiles
     */
    public void sincronizarPerfiles(List<Perfil> perfiles) {
        ConfiguracionGlobal configuracion = repositorio.cargar();
        configuracion.setPerfiles(perfiles);
        repositorio.guardar(configuracion);
    }
}
