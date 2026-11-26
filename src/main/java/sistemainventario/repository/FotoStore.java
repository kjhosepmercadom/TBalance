package sistemainventario.repository;

import java.util.Set;
import java.util.Optional;

/**
 * Fotos de los productos de un negocio (v3.9.0). Van aparte de los productos a propósito:
 * las listas se leen todo el tiempo y no deben arrastrar imágenes; la foto de un producto
 * se pide solo cuando se va a mostrar.
 */
public interface FotoStore {

    /**
     * Una foto ya reducida por la página.
     *
     * @param tipo  {@code image/webp}, {@code image/jpeg} o {@code image/png}
     * @param datos bytes de la imagen
     */
    record Foto(String tipo, byte[] datos) {
    }

    /** @return los productos que tienen foto (una sola consulta, sin leer las imágenes) */
    Set<String> idsConFoto();

    /**
     * @param productoId producto
     * @return su foto, si tiene
     */
    Optional<Foto> obtener(String productoId);

    /**
     * Guarda la foto del producto; si ya tenía una, la reemplaza.
     *
     * @param productoId producto
     * @param foto       imagen ya validada
     */
    void guardar(String productoId, Foto foto);

    /**
     * @param productoId producto
     * @return {@code true} si tenía foto y se quitó
     */
    boolean eliminar(String productoId);

    /** @return bytes que ocupan todas las fotos del negocio */
    long bytesUsados();

    /** Almacenamiento sin soporte de fotos (por ejemplo Oracle): todo vacío y no guarda. */
    FotoStore SIN_SOPORTE = new FotoStore() {
        @Override
        public Set<String> idsConFoto() {
            return Set.of();
        }

        @Override
        public Optional<Foto> obtener(String productoId) {
            return Optional.empty();
        }

        @Override
        public void guardar(String productoId, Foto foto) {
            throw new sistemainventario.exception.ValidationException(
                    "Este almacenamiento no guarda fotos. Use PostgreSQL o los archivos JSON.");
        }

        @Override
        public boolean eliminar(String productoId) {
            return false;
        }

        @Override
        public long bytesUsados() {
            return 0;
        }
    };
}
