package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.repository.FotoStore;
import sistemainventario.repository.FotoStore.Foto;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Reglas de las fotos de producto (v3.9.0). La página ya reduce la imagen a unos 400 px;
 * aquí no se confía en eso: se revisa que de verdad sea una imagen, que pese poco y que
 * el negocio no pase de su cuota, para que las fotos nunca llenen la base.
 */
public class FotoService {

    /** Lo máximo que puede pesar una foto (una de 400 px en WebP pesa unos 30 KB). */
    public static final int BYTES_MAXIMOS = 60 * 1024;
    /** Lo máximo que pueden ocupar todas las fotos de un negocio (unas 5.000 fotos). */
    public static final long CUOTA_BYTES = 150L * 1024 * 1024;

    private final FotoStore almacen;
    private final Consumer<String> exigirProducto;
    private final BooleanSupplier habilitadas;

    /**
     * @param almacen        dónde se guardan las fotos del negocio
     * @param exigirProducto lanza {@link ValidationException} si el producto no existe o está deshabilitado
     * @param habilitadas    si el negocio usa fotos (ver {@code Profile.usaFotos})
     */
    public FotoService(FotoStore almacen, Consumer<String> exigirProducto, BooleanSupplier habilitadas) {
        this.almacen = almacen;
        this.exigirProducto = exigirProducto;
        this.habilitadas = habilitadas;
    }

    /** @return si el negocio usa fotos */
    public boolean estanHabilitadas() {
        return habilitadas.getAsBoolean();
    }

    /** @return productos con foto; vacío si el negocio no usa fotos */
    public Set<String> idsConFoto() {
        return estanHabilitadas() ? almacen.idsConFoto() : Set.of();
    }

    /**
     * @param productoId producto
     * @return su foto como "data:image/...;base64,...", si tiene
     */
    public Optional<String> obtener(String productoId) {
        if (!estanHabilitadas() || productoId == null || productoId.isBlank()) {
            return Optional.empty();
        }
        return almacen.obtener(productoId)
                .map(f -> "data:" + f.tipo() + ";base64," + Base64.getEncoder().encodeToString(f.datos()));
    }

    /**
     * Guarda (o reemplaza) la foto de un producto.
     *
     * @param productoId producto
     * @param dataUrl    imagen como "data:image/webp;base64,..."
     * @throws ValidationException si no es una imagen válida, pesa demasiado o pasa la cuota
     */
    public void guardar(String productoId, String dataUrl) {
        if (!estanHabilitadas()) {
            throw new ValidationException("Este negocio no usa fotos de producto");
        }
        exigirProducto.accept(productoId);
        Foto foto = leer(dataUrl);
        long anterior = almacen.obtener(productoId).map(f -> (long) f.datos().length).orElse(0L);
        if (almacen.bytesUsados() - anterior + foto.datos().length > CUOTA_BYTES) {
            throw new ValidationException("Las fotos de este negocio ya ocupan el espacio permitido ("
                    + CUOTA_BYTES / (1024 * 1024) + " MB). Quite fotos de productos que ya no vende.");
        }
        almacen.guardar(productoId, foto);
    }

    /**
     * @param productoId producto
     * @return {@code true} si tenía foto y se quitó
     */
    public boolean eliminar(String productoId) {
        return productoId != null && !productoId.isBlank() && almacen.eliminar(productoId);
    }

    /** @return bytes que ocupan las fotos del negocio */
    public long bytesUsados() {
        return almacen.bytesUsados();
    }

    /**
     * Decodifica y revisa la imagen. El tipo se confirma con los primeros bytes: lo que diga
     * el texto no cuenta.
     *
     * @param dataUrl imagen como data URL en base64
     * @return la foto lista para guardar
     * @throws ValidationException si no es WebP, JPG o PNG, está dañada o pesa más de 60 KB
     */
    public static Foto leer(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) {
            throw new ValidationException("Elija una foto en JPG, PNG o WebP");
        }
        int coma = dataUrl.indexOf(',');
        if (coma < 0 || !dataUrl.substring(0, coma).endsWith(";base64")) {
            throw new ValidationException("La foto está dañada");
        }
        byte[] datos;
        try {
            datos = Base64.getDecoder().decode(dataUrl.substring(coma + 1).trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("La foto está dañada");
        }
        if (datos.length == 0) {
            throw new ValidationException("La foto está vacía");
        }
        if (datos.length > BYTES_MAXIMOS) {
            throw new ValidationException("La foto pesa " + datos.length / 1024
                    + " KB y el máximo es " + BYTES_MAXIMOS / 1024 + " KB. Pruebe con otra foto.");
        }
        String tipo = tipoReal(datos);
        if (tipo == null) {
            throw new ValidationException("Elija una foto en JPG, PNG o WebP");
        }
        return new Foto(tipo, datos);
    }

    private static String tipoReal(byte[] b) {
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        return null;
    }
}
