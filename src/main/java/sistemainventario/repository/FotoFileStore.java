package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fotos en archivos: {@code perfiles/<negocio>/fotos/<producto>.webp|jpg|png}. La extensión
 * dice el tipo de la imagen, así no hace falta un índice aparte.
 */
public class FotoFileStore implements FotoStore {

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/webp", "webp", "image/jpeg", "jpg", "image/png", "png");

    private final Path carpeta;

    /** @param carpeta carpeta {@code fotos} del negocio (se crea al guardar la primera) */
    public FotoFileStore(Path carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public Set<String> idsConFoto() {
        if (!Files.isDirectory(carpeta)) {
            return Set.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.map(a -> a.getFileName().toString())
                    .filter(n -> n.lastIndexOf('.') > 0)
                    .map(n -> n.substring(0, n.lastIndexOf('.')))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new PersistenceException("No se pudieron leer las fotos", e);
        }
    }

    @Override
    public Optional<Foto> obtener(String productoId) {
        for (Map.Entry<String, String> e : EXTENSIONES.entrySet()) {
            Path ruta = archivo(productoId, e.getValue());
            if (Files.isRegularFile(ruta)) {
                try {
                    return Optional.of(new Foto(e.getKey(), Files.readAllBytes(ruta)));
                } catch (IOException ex) {
                    throw new PersistenceException("No se pudo leer la foto", ex);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void guardar(String productoId, Foto foto) {
        String extension = EXTENSIONES.get(foto.tipo());
        if (extension == null) {
            throw new PersistenceException("Tipo de imagen no permitido: " + foto.tipo(), null);
        }
        try {
            Files.createDirectories(carpeta);
            eliminar(productoId);
            Files.write(archivo(productoId, extension), foto.datos());
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar la foto", e);
        }
    }

    @Override
    public boolean eliminar(String productoId) {
        boolean habia = false;
        try {
            for (String extension : EXTENSIONES.values()) {
                habia |= Files.deleteIfExists(archivo(productoId, extension));
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo quitar la foto", e);
        }
        return habia;
    }

    @Override
    public long bytesUsados() {
        if (!Files.isDirectory(carpeta)) {
            return 0;
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.mapToLong(a -> a.toFile().length()).sum();
        } catch (IOException e) {
            throw new PersistenceException("No se pudieron leer las fotos", e);
        }
    }

    /** El ID viene de la base y de la página: se limpia para que nunca salga de la carpeta. */
    private Path archivo(String productoId, String extension) {
        String seguro = productoId.replaceAll("[^A-Za-z0-9_-]", "_");
        return carpeta.resolve(seguro + "." + extension);
    }
}
