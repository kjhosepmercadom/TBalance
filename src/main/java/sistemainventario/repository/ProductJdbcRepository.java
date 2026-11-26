package sistemainventario.repository;

import sistemainventario.model.ManejoProducto;
import sistemainventario.model.Product;
import sistemainventario.model.ProductVariant;
import sistemainventario.model.UnidadMedida;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Productos de un negocio en la tabla PRODUCTO.
 *
 * <p>Al guardar, los disparadores TRG_PRODUCTO_VALIDAR y
 * TRG_PRODUCTO_HISTORIAL_PRECIO de la base revisan el producto y registran los
 * cambios de precio.</p>
 *
 * <p>v3.6.0: las tallas y colores van en la tabla hija PRODUCTO_VARIANTE y se
 * guardan en la misma transacción que el producto, así PRODUCTO.STOCK (la suma)
 * nunca queda distinto de sus variantes.</p>
 */
public class ProductJdbcRepository extends JdbcRepository<Product> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public ProductJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "PRODUCTO";
    }

    @Override
    protected List<String> columnas() {
        return List.of("NOMBRE", "CODIGO", "DESCRIPCION", "CATEGORIA", "CATEGORIA_ID", "PROVEEDOR_ID",
                "PRECIO_COMPRA", "PRECIO_VENTA", "STOCK", "STOCK_MIN", "TIPO", "REQUIERE_VENCIMIENTO",
                "FECHA_VENCIMIENTO", "ACTIVO", "MANEJO", "UNIDAD_MEDIDA");
    }

    @Override
    protected Object[] valores(Product p) {
        return new Object[]{p.getNombre(), texto(p.getCodigo()), texto(p.getDescripcion()), texto(p.getCategoria()),
                referenciaValida("CATEGORIA", p.getCategoriaId()), referenciaValida("PROVEEDOR", p.getProveedorId()),
                p.getPrecioCompra(), p.getPrecioVenta(), p.getStock(), p.getStockMin(), texto(p.getTipo()),
                p.isRequiereVencimiento(), dia(p.getFechaVencimiento()), p.isActivo(),
                p.getManejo().name(), p.getUnidadMedida().getCodigo()};
    }

    @Override
    protected Product leer(ResultSet fila) throws SQLException {
        Product p = new Product();
        p.setId(fila.getString("ID"));
        p.setNombre(fila.getString("NOMBRE"));
        p.setCodigo(fila.getString("CODIGO"));
        p.setDescripcion(fila.getString("DESCRIPCION"));
        p.setCategoria(fila.getString("CATEGORIA"));
        p.setCategoriaId(fila.getString("CATEGORIA_ID"));
        p.setProveedorId(fila.getString("PROVEEDOR_ID"));
        p.setPrecioCompra(leerNumero(fila, "PRECIO_COMPRA"));
        p.setPrecioVenta(leerNumero(fila, "PRECIO_VENTA"));
        p.setStock(leerCantidad(fila, "STOCK"));
        p.setStockMin(leerCantidad(fila, "STOCK_MIN"));
        p.setTipo(fila.getString("TIPO"));
        p.setRequiereVencimiento(fila.getInt("REQUIERE_VENCIMIENTO") == 1);
        p.setFechaVencimiento(leerDia(fila, "FECHA_VENCIMIENTO"));
        p.setActivo(fila.getInt("ACTIVO") == 1);
        p.setManejo(ManejoProducto.desdeCodigo(fila.getString("MANEJO")));
        p.setUnidadMedida(UnidadMedida.desdeCodigo(fila.getString("UNIDAD_MEDIDA")));
        return p;
    }

    /** Lee las variantes de todos los productos del negocio con una sola consulta. */
    @Override
    protected void completar(List<Product> productos) {
        Map<String, List<ProductVariant>> porProducto = new LinkedHashMap<>();
        bd.consultar("SELECT PRODUCTO_ID, ID, TALLA, COLOR, CODIGO, STOCK, STOCK_MIN, PRECIO_VENTA, ACTIVO "
                        + "FROM PRODUCTO_VARIANTE WHERE PERFIL_ID = ? ORDER BY PRODUCTO_ID, ORDEN, ID",
                fila -> {
                    ProductVariant v = new ProductVariant();
                    v.setId(fila.getString("ID"));
                    v.setTalla(fila.getString("TALLA"));
                    v.setColor(fila.getString("COLOR"));
                    v.setCodigo(fila.getString("CODIGO"));
                    v.setStock(leerCantidad(fila, "STOCK"));
                    v.setStockMin(leerCantidad(fila, "STOCK_MIN"));
                    double precio = leerNumero(fila, "PRECIO_VENTA");
                    v.setPrecioVenta(precio > 0 ? precio : null);
                    v.setActivo(fila.getInt("ACTIVO") == 1);
                    porProducto.computeIfAbsent(fila.getString("PRODUCTO_ID"), k -> new ArrayList<>()).add(v);
                    return v;
                }, perfilId);
        for (Product p : productos) {
            p.setVariantes(porProducto.getOrDefault(p.getId(), List.of()));
        }
    }

    /**
     * Guarda las variantes: actualiza las que ya existen e inserta las nuevas.
     * Nunca las borra, porque las ventas y entradas viejas las mencionan
     * (una variante que ya no se usa queda con ACTIVO = 0).
     */
    @Override
    protected void guardarHijos(Connection conexion, Product p, boolean nuevo) throws SQLException {
        int orden = 1;
        for (ProductVariant v : p.getVariantes()) {
            Object precio = v.getPrecioVenta() == null ? new Database.Nulo(Types.NUMERIC) : v.getPrecioVenta();
            int cambiadas = nuevo ? 0 : Database.actualizar(conexion,
                    "UPDATE PRODUCTO_VARIANTE SET TALLA = ?, COLOR = ?, CODIGO = ?, STOCK = ?, STOCK_MIN = ?, "
                            + "PRECIO_VENTA = ?, ACTIVO = ?, ORDEN = ? WHERE PERFIL_ID = ? AND ID = ? AND PRODUCTO_ID = ?",
                    v.getTalla(), v.getColor(), texto(v.getCodigo()), v.getStock(), v.getStockMin(), precio,
                    v.isActivo(), orden, perfilId, v.getId(), p.getId());
            if (cambiadas == 0) {
                Database.actualizar(conexion,
                        "INSERT INTO PRODUCTO_VARIANTE (PERFIL_ID, ID, PRODUCTO_ID, TALLA, COLOR, CODIGO, STOCK, "
                                + "STOCK_MIN, PRECIO_VENTA, ACTIVO, ORDEN) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        perfilId, v.getId(), p.getId(), v.getTalla(), v.getColor(), texto(v.getCodigo()),
                        v.getStock(), v.getStockMin(), precio, v.isActivo(), orden);
            }
            orden++;
        }
    }
}
