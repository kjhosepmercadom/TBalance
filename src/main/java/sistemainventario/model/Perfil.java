package sistemainventario.model;

import sistemainventario.util.FechaUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa un perfil de trabajo independiente creado por el usuario.
 * Cada perfil posee su propio espacio de datos aislado (productos, categorías,
 * proveedores, ventas, etc.) y su propia configuración.
 *
 * Aplica SRP: solo modela los datos del perfil, la lógica de negocio
 * reside en {@link sistemainventario.service.GestorPerfiles}.
 */
public class Perfil extends EntidadConId {

    private String nombre;
    private String descripcion;
    private String color;
    private String tipoNegocio;
    private String fechaCreacion;
    private Map<String, String> configuracion;

    public Perfil() {
        super();
        this.configuracion = new LinkedHashMap<>();
    }

    public Perfil(String id, String nombre, String descripcion,
                  String color, String tipoNegocio) {
        super(id);
        this.nombre         = nombre;
        this.descripcion    = descripcion;
        this.color          = color != null ? color : "#d4821b";
        this.tipoNegocio    = tipoNegocio;
        this.fechaCreacion  = FechaUtil.hoy();
        this.configuracion  = new LinkedHashMap<>();
        initConfiguracionPorDefecto();
    }

    private void initConfiguracionPorDefecto() {
        configuracion.put("moneda",       "COP");
        configuracion.put("stockMin",     "5");
        configuracion.put("notificaciones", "true");
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : id;
    }

    public String  getNombre()                                  { return nombre; }
    public void    setNombre(String n)                          { this.nombre = n; }
    public String  getDescripcion()                             { return descripcion; }
    public void    setDescripcion(String d)                     { this.descripcion = d; }
    public String  getColor()                                   { return color; }
    public void    setColor(String c)                           { this.color = c; }
    public String  getTipoNegocio()                             { return tipoNegocio; }
    public void    setTipoNegocio(String t)                     { this.tipoNegocio = t; }
    public String  getFechaCreacion()                           { return fechaCreacion; }
    public void    setFechaCreacion(String f)                   { this.fechaCreacion = f; }
    public Map<String, String> getConfiguracion()               { return configuracion; }
    public void    setConfiguracion(Map<String, String> cfg)    { this.configuracion = cfg; }

    public String getConfig(String clave, String defecto) {
        if (configuracion == null) return defecto;
        return configuracion.getOrDefault(clave, defecto);
    }

    public void setConfig(String clave, String valor) {
        if (configuracion == null) configuracion = new LinkedHashMap<>();
        configuracion.put(clave, valor);
    }
}
