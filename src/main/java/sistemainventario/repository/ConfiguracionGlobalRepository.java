package sistemainventario.repository;

import sistemainventario.model.ConfiguracionGlobal;
import sistemainventario.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Persiste la configuración global (lista de perfiles y perfil activo).
 * Es un repositorio de objeto único (no lista), por lo que no extiende
 * {@link JsonRepository} sino que implementa su propia serialización.
 */
public class ConfiguracionGlobalRepository {

    private static final String ARCHIVO = "configuracion-global.json";
    private final Path ruta;

    public ConfiguracionGlobalRepository(Path dirBase) {
        try { Files.createDirectories(dirBase); } catch (IOException ignored) {}
        this.ruta = dirBase.resolve(ARCHIVO);
    }

    public boolean existe() {
        return Files.exists(ruta);
    }

    public ConfiguracionGlobal cargar() {
        if (!existe()) return null;
        try {
            String json = Files.readString(ruta, StandardCharsets.UTF_8);
            return JsonUtil.fromJson(json, ConfiguracionGlobal.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void guardar(ConfiguracionGlobal cfg) {
        try {
            Files.writeString(ruta, JsonUtil.toJson(cfg), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error guardando configuracion-global.json: " + e.getMessage());
        }
    }
}
