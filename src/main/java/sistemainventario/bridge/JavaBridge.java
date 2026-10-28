package sistemainventario.bridge;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import sistemainventario.AppContext;
import sistemainventario.model.*;
import sistemainventario.service.GestorInventario;
import sistemainventario.util.FechaUtil;
import sistemainventario.util.JsonUtil;

import java.util.*;

public class JavaBridge {

    private final WebEngine engine;
    private final AppContext ctx;
    private final Gson gson = JsonUtil.getGson();

    public JavaBridge(WebEngine engine, AppContext ctx) {
        this.engine = engine;
        this.ctx    = ctx;
    }

    // ══════════════════════════════════════════════════════════════════
    //  PERFILES
    // ══════════════════════════════════════════════════════════════════

    public String listarPerfiles() {
        return gson.toJson(ctx.perfilRepo.listarTodos());
    }

    public String obtenerPerfilActivo() {
        Perfil p = ctx.gestorPerfiles.getPerfilActivo();
        return p != null ? gson.toJson(p) : "{}";
    }

    public String obtenerSugerencias() {
        Perfil activo = ctx.gestorPerfiles.getPerfilActivo();
        PerfilNegocio tipo = activo != null
                ? PerfilNegocio.desdeClave(activo.getTipoNegocio())
                : PerfilNegocio.NINGUNO;
        return gson.toJson(tipo.getSugerencias());
    }

    public boolean requiereControlVencimiento() {
        Perfil activo = ctx.gestorPerfiles.getPerfilActivo();
        if (activo == null) return false;
        return PerfilNegocio.desdeClave(activo.getTipoNegocio()).requiereControlVencimiento();
    }

    public String obtenerTiposNegocio() {
        List<Map<String, Object>> tipos = new ArrayList<>();
        for (PerfilNegocio t : PerfilNegocio.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clave",      t.name());
            m.put("etiqueta",   t.getEtiqueta());
            m.put("categorias", t.getCategoriasPredefinidas());
            m.put("vencimiento",t.requiereControlVencimiento());
            tipos.add(m);
        }
        return gson.toJson(tipos);
    }

    public String crearPerfil(String jsonPerfil) {
        try {
            Perfil nuevo = gson.fromJson(jsonPerfil, Perfil.class);
            Perfil guardado = ctx.gestorPerfiles.crear(
                    nuevo.getNombre(),
                    nuevo.getDescripcion(),
                    nuevo.getColor(),
                    nuevo.getTipoNegocio());
            ctx.gestorPerfiles.sincronizarConfiguracion();
            ejecutarEnUI("onPerfilesActualizados()");
            return gson.toJson(guardado);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String editarPerfil(String jsonPerfil) {
        try {
            Perfil p = gson.fromJson(jsonPerfil, Perfil.class);
            boolean ok = ctx.gestorPerfiles.actualizar(p);
            if (ok) {
                ctx.gestorPerfiles.sincronizarConfiguracion();
                ejecutarEnUI("onPerfilesActualizados()");
            }
            return ok ? "OK" : "ERROR: Perfil no encontrado";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String eliminarPerfil(String id) {
        boolean ok = ctx.gestorPerfiles.eliminar(id);
        if (ok) {
            ctx.gestorPerfiles.sincronizarConfiguracion();
            ejecutarEnUI("onPerfilesActualizados()");
        }
        return ok ? "OK" : "ERROR: Perfil no encontrado";
    }

    public String seleccionarPerfil(String id) {
        try {
            if (id == null || id.isBlank() || "null".equals(id)) {
                ctx.gestorPerfiles.seleccionar(null);
                ejecutarEnUI("onPerfilSeleccionado(null)");
                return "OK";
            }
            if (ctx.gestorPerfiles.buscarPorId(id) == null)
                return "ERROR: Perfil no encontrado";
            ctx.gestorPerfiles.seleccionar(id);
            ctx.cargarServiciosDePerfil(id);
            ejecutarEnUI("onPerfilSeleccionado('" + id + "')");
            return "OK";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  DASHBOARD
    // ══════════════════════════════════════════════════════════════════

    public String obtenerEstadisticas() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalProductos",     ctx.gestorInventario.listarTodos().size());
        stats.put("bajoStock",          ctx.gestorInventario.getBajoStock().size());
        stats.put("agotados",           ctx.gestorInventario.getAgotados().size());
        stats.put("totalCategorias",    ctx.gestorCategorias.listarTodos().size());
        stats.put("totalProveedores",   ctx.gestorProveedores.listarTodos().size());
        stats.put("totalVentas",        ctx.gestorVentas.getTotalVentas());
        stats.put("totalTransacciones", ctx.gestorVentas.getTotalTransacciones());

        if (requiereControlVencimiento()) {
            stats.put("vencidos",    ctx.gestorInventario.getVencidos().size());
            stats.put("porVencer7",  ctx.gestorInventario.getPorVencer(7).size());
            stats.put("porVencer30", ctx.gestorInventario.getPorVencer(30).size());
        }
        return gson.toJson(stats);
    }

    public String obtenerProductosVencidos() {
        return gson.toJson(ctx.gestorInventario.getVencidos());
    }

    public String obtenerProductosPorVencer(int dias) {
        return gson.toJson(ctx.gestorInventario.getPorVencer(dias));
    }

    // ══════════════════════════════════════════════════════════════════
    //  PRODUCTOS
    // ══════════════════════════════════════════════════════════════════

    public String obtenerProductos() {
        return gson.toJson(ctx.gestorInventario.listarTodos());
    }

    public String buscarProductos(String texto) {
        return gson.toJson(ctx.gestorInventario.buscarPorTexto(texto));
    }

    public String filtrarProductosPorCategoria(String categoria) {
        return gson.toJson(ctx.gestorInventario.filtrarPorCategoria(categoria));
    }

    public String guardarProducto(String jsonProducto) {
        try {
            Producto p = gson.fromJson(jsonProducto, Producto.class);
            String validacion = GestorInventario.validarFechaVencimiento(
                    p.isRequiereVencimiento(), p.getFechaVencimiento());
            if (!"OK".equals(validacion)) return "ERROR: " + validacion;

            boolean ok;
            if (p.getId() == null || p.getId().isBlank() ||
                ctx.gestorInventario.buscarPorId(p.getId()) == null) {
                if (p.getId() == null || p.getId().isBlank())
                    p.setId(ctx.gestorInventario.generarNuevoId());
                ok = ctx.gestorInventario.agregar(p);
            } else {
                ok = ctx.gestorInventario.actualizar(p);
            }
            return ok ? "OK" : "ERROR: No se pudo guardar el producto";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String eliminarProducto(String id) {
        return ctx.gestorInventario.eliminar(id) ? "OK" : "ERROR: Producto no encontrado";
    }

    // ══════════════════════════════════════════════════════════════════
    //  CATEGORIAS
    // ══════════════════════════════════════════════════════════════════

    public String obtenerCategorias() {
        return gson.toJson(ctx.gestorCategorias.listarTodos());
    }

    public String guardarCategoria(String jsonCategoria) {
        try {
            Categoria c = gson.fromJson(jsonCategoria, Categoria.class);
            boolean ok;
            if (c.getId() == null || c.getId().isBlank() ||
                ctx.gestorCategorias.buscarPorId(c.getId()) == null) {
                if (c.getId() == null || c.getId().isBlank())
                    c.setId("CAT-" + System.currentTimeMillis());
                ok = ctx.gestorCategorias.agregar(c);
            } else {
                ok = ctx.gestorCategorias.actualizar(c);
            }
            return ok ? "OK" : "ERROR";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String eliminarCategoria(String id) {
        return ctx.gestorCategorias.eliminar(id) ? "OK" : "ERROR";
    }

    // ══════════════════════════════════════════════════════════════════
    //  PROVEEDORES
    // ══════════════════════════════════════════════════════════════════

    public String obtenerProveedores() {
        return gson.toJson(ctx.gestorProveedores.listarTodos());
    }

    public String guardarProveedor(String jsonProveedor) {
        try {
            Proveedor p = gson.fromJson(jsonProveedor, Proveedor.class);
            boolean ok;
            if (p.getId() == null || ctx.gestorProveedores.buscarPorId(p.getId()) == null) {
                if (p.getId() == null || p.getId().isBlank())
                    p.setId(ctx.gestorProveedores.generarNuevoId());
                ok = ctx.gestorProveedores.agregar(p);
            } else {
                ok = ctx.gestorProveedores.actualizar(p);
            }
            return ok ? "OK" : "ERROR";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String eliminarProveedor(String id) {
        return ctx.gestorProveedores.eliminar(id) ? "OK" : "ERROR";
    }

    // ══════════════════════════════════════════════════════════════════
    //  ENTRADAS
    // ══════════════════════════════════════════════════════════════════

    public String obtenerEntradas() {
        return gson.toJson(ctx.gestorMovimientos.listarEntradas());
    }

    public String registrarEntrada(String jsonEntrada) {
        try {
            Entrada e = gson.fromJson(jsonEntrada, Entrada.class);
            e.setId("ENT-" + System.currentTimeMillis());
            e.setFecha(FechaUtil.hoy());
            boolean ok = ctx.gestorMovimientos.registrarEntrada(e);
            return ok ? "OK" : "ERROR: Stock o producto invalido";
        } catch (Exception ex) {
            return "ERROR: " + ex.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  SALIDAS
    // ══════════════════════════════════════════════════════════════════

    public String obtenerSalidas() {
        return gson.toJson(ctx.gestorMovimientos.listarSalidas());
    }

    public String registrarSalida(String jsonSalida) {
        try {
            Salida s = gson.fromJson(jsonSalida, Salida.class);
            s.setId("SAL-" + System.currentTimeMillis());
            s.setFecha(FechaUtil.hoy());
            boolean ok = ctx.gestorMovimientos.registrarSalida(s);
            return ok ? "OK" : "ERROR: Stock insuficiente";
        } catch (Exception ex) {
            return "ERROR: " + ex.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  VENTAS
    // ══════════════════════════════════════════════════════════════════

    public String obtenerVentas() {
        var ventas = ctx.gestorVentas.listarTodas();
        for (Venta v : ventas) v.recalcularTotal();
        return gson.toJson(ventas);
    }

    public String crearVenta(String jsonVenta) {
        try {
            Venta v = gson.fromJson(jsonVenta, Venta.class);
            v.setId("VTA-" + System.currentTimeMillis());
            v.setFecha(FechaUtil.hoy());
            v.recalcularTotal();
            Venta creada = ctx.gestorVentas.crear(v);
            return creada != null ? gson.toJson(creada) : "ERROR: Stock insuficiente en algun producto";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILITARIOS
    // ══════════════════════════════════════════════════════════════════

    public void recargarEstado() {
        ejecutarEnUI("cargarEstadoDesdeJava()");
    }

    private void ejecutarEnUI(String script) {
        Platform.runLater(() -> {
            try { engine.executeScript(script); }
            catch (Exception e) {
                System.err.println("[Bridge] Error ejecutando script: " + e.getMessage());
            }
        });
    }

    public void mostrarToast(String mensaje, String tipo) {
        String safe = mensaje.replace("'", "\\'").replace("\n", "");
        ejecutarEnUI("showToast('" + safe + "', '" + tipo + "')");
    }
}
