package sistemainventario.service;

import sistemainventario.model.ConfiguracionGlobal;
import sistemainventario.repository.ConfiguracionGlobalRepository;

/**
 * Servicio responsable de la configuración global de la aplicación
 * (lista de perfiles, perfil activo).
 *
 * SRP: solo lee/escribe {@link ConfiguracionGlobal}.
 * La gestión de perfiles individuales reside en {@link GestorPerfiles}.
 */
public class GestorConfiguracionGlobal {

    private final ConfiguracionGlobalRepository repo;
    private ConfiguracionGlobal config;

    public GestorConfiguracionGlobal(ConfiguracionGlobalRepository repo) {
        this.repo   = repo;
        this.config = repo.cargar();
        if (this.config == null) this.config = new ConfiguracionGlobal();
    }

    public boolean esPrimerArranque() {
        return !repo.existe() || repo.cargar() == null
                || repo.cargar().getPerfiles().isEmpty();
    }

    public ConfiguracionGlobal getConfig() {
        if (config == null) config = repo.cargar();
        return config;
    }

    public void guardar(ConfiguracionGlobal cfg) {
        this.config = cfg;
        repo.guardar(cfg);
    }
}
