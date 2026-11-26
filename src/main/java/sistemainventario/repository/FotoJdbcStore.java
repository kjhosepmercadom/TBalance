package sistemainventario.repository;

import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Fotos en la tabla PRODUCTO_FOTO de PostgreSQL (cambio v11). */
public class FotoJdbcStore implements FotoStore {

    private final Database bd;
    private final String perfilId;

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public FotoJdbcStore(Database bd, String perfilId) {
        this.bd = bd;
        this.perfilId = perfilId;
    }

    @Override
    public Set<String> idsConFoto() {
        return new HashSet<>(bd.consultar("SELECT PRODUCTO_ID FROM PRODUCTO_FOTO WHERE PERFIL_ID = ?",
                fila -> fila.getString(1), perfilId));
    }

    @Override
    public Optional<Foto> obtener(String productoId) {
        List<Foto> filas = bd.consultar(
                "SELECT TIPO, DATOS FROM PRODUCTO_FOTO WHERE PERFIL_ID = ? AND PRODUCTO_ID = ?",
                fila -> new Foto(fila.getString(1), fila.getBytes(2)), perfilId, productoId);
        return filas.stream().findFirst();
    }

    @Override
    public void guardar(String productoId, Foto foto) {
        bd.transaccion(c -> {
            Database.actualizar(c, "DELETE FROM PRODUCTO_FOTO WHERE PERFIL_ID = ? AND PRODUCTO_ID = ?",
                    perfilId, productoId);
            return Database.actualizar(c,
                    "INSERT INTO PRODUCTO_FOTO (PERFIL_ID, PRODUCTO_ID, TIPO, DATOS, BYTES) VALUES (?, ?, ?, ?, ?)",
                    perfilId, productoId, foto.tipo(), new Database.Binario(foto.datos()), foto.datos().length);
        });
    }

    @Override
    public boolean eliminar(String productoId) {
        return bd.transaccion(c -> Database.actualizar(c,
                "DELETE FROM PRODUCTO_FOTO WHERE PERFIL_ID = ? AND PRODUCTO_ID = ?", perfilId, productoId)) > 0;
    }

    @Override
    public long bytesUsados() {
        List<Long> filas = bd.consultar("SELECT COALESCE(SUM(BYTES), 0) FROM PRODUCTO_FOTO WHERE PERFIL_ID = ?",
                fila -> fila.getLong(1), perfilId);
        return filas.isEmpty() ? 0 : filas.get(0);
    }
}
