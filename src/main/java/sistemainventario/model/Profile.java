package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.DateUtil;
import sistemainventario.util.TextUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Espacio de trabajo independiente (un negocio). Cada perfil tiene su propio
 * directorio con productos, categorías, proveedores, movimientos y ventas.
 *
 * <p><b>SRP:</b> solo modela los datos del perfil; su ciclo de vida lo
 * administra {@link sistemainventario.service.ProfileService}.</p>
 */
public class Profile extends IdentifiedEntity implements Deshabilitable {

    public static final String COLOR_POR_DEFECTO = "#315D53";

    private String nombre;
    private String descripcion;
    private String color;
    private String tipoNegocio;
    private String fechaCreacion;
    private Map<String, String> configuracion = new LinkedHashMap<>();

    /** {@code null} en los datos de versiones anteriores, que cuentan como habilitados. */
    private Boolean activo;

    /** Constructor vacío requerido por Gson. */
    public Profile() {
        super();
    }

    public Profile(String id, String nombre, String descripcion, String color, String tipoNegocio) {
        super(id);
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.color = TextUtil.esVacio(color) ? COLOR_POR_DEFECTO : color;
        this.tipoNegocio = tipoNegocio;
        this.fechaCreacion = DateUtil.hoy();
        inicializarConfiguracionPorDefecto();
    }

    private void inicializarConfiguracionPorDefecto() {
        configuracion.put("moneda", "COP");
        configuracion.put("stockMin", "5");
        configuracion.put("notificaciones", "true");
    }

    /**
     * @throws ValidationException si el nombre está vacío
     */
    public void validar() {
        if (TextUtil.esVacio(nombre)) {
            throw new ValidationException("El nombre del perfil es obligatorio");
        }
    }

    /**
     * <b>GRASP Experto:</b> actualiza solo los datos editables y conserva la
     * fecha de creación, la configuración y el tipo de negocio (que no puede
     * cambiarse después de crear el perfil).
     *
     * @param nuevoNombre      nombre nuevo (obligatorio)
     * @param nuevaDescripcion descripción nueva (opcional)
     * @param nuevoColor       color nuevo; si está vacío se conserva el actual
     */
    public void actualizarDatos(String nuevoNombre, String nuevaDescripcion, String nuevoColor) {
        if (TextUtil.esVacio(nuevoNombre)) {
            throw new ValidationException("El nombre del perfil es obligatorio");
        }
        this.nombre = nuevoNombre.trim();
        this.descripcion = TextUtil.recortar(nuevaDescripcion);
        if (!TextUtil.esVacio(nuevoColor)) {
            this.color = nuevoColor;
        }
    }

    /** @return el tipo de negocio como enumeración ({@code NINGUNO} si no tiene) */
    public BusinessType getPerfilNegocio() {
        return BusinessType.desdeClave(tipoNegocio);
    }

    /** @return {@code true} si el tipo de negocio exige controlar vencimientos */
    public boolean requiereControlVencimiento() {
        return getCapacidades().vencimiento() != ControlVencimiento.NINGUNO;
    }

    /**
     * <b>v3.6.0:</b> lo que maneja este negocio. Si nunca se guardaron, son las
     * del tipo de negocio.
     *
     * @return las capacidades del negocio
     */
    public BusinessCapabilities getCapacidades() {
        return BusinessCapabilities.desde(configuracion, getPerfilNegocio().capacidadesPorDefecto());
    }

    /** @param capacidades lo que maneja el negocio desde ahora */
    public void setCapacidades(BusinessCapabilities capacidades) {
        capacidades.comoConfiguracion().forEach(this::setConfig);
    }

    /** @return {@code true} si las capacidades ya están guardadas en la configuración */
    public boolean tieneCapacidadesGuardadas() {
        return BusinessCapabilities.estanGuardadas(configuracion);
    }

    /**
     * Toma las capacidades que vienen de la pantalla (claves {@code cap.*}); las
     * que no vengan quedan como estaban.
     *
     * @param datos perfil recibido de la vista
     * @return las capacidades resultantes
     */
    public BusinessCapabilities capacidadesPedidas(Profile datos) {
        Map<String, String> pedidas = datos == null ? null : datos.getConfiguracion();
        return BusinessCapabilities.desde(pedidas, getCapacidades());
    }

    /**
     * @param clave   clave de configuración
     * @param defecto valor devuelto si la clave no existe
     * @return el valor configurado o {@code defecto}
     */
    public String getConfig(String clave, String defecto) {
        return configuracion == null ? defecto : configuracion.getOrDefault(clave, defecto);
    }

    public void setConfig(String clave, String valor) {
        if (configuracion == null) {
            configuracion = new LinkedHashMap<>();
        }
        configuracion.put(clave, valor);
    }

    /**
     * Cuentas donde el negocio recibe pagos digitales (se muestran al cobrar).
     * TBalance solo las muestra: no se conecta con los bancos.
     */
    public static final java.util.List<String> CLAVES_CUENTAS = java.util.List.of(
            "cuentaNequi", "cuentaDaviplata", "cuentaBancolombia", "llaveBreB");

    /**
     * Copia de otro perfil los medios de pago y las cuentas que llegaron al
     * crear o editar el negocio (solo esas claves, recortadas a 60 caracteres).
     *
     * @param datos perfil recibido de la pantalla
     */
    public void copiarAjustesDePago(Profile datos) {
        if (datos == null || datos.getConfiguracion() == null) {
            return;
        }
        String medios = datos.getConfiguracion().get(CLAVE_MEDIOS_PAGO);
        if (medios != null) {
            setMediosPago(java.util.Arrays.asList(medios.split(",")));
        }
        for (String clave : CLAVES_CUENTAS) {
            String valor = datos.getConfiguracion().get(clave);
            if (valor != null) {
                String limpio = valor.replaceAll("[\\r\\n\\t]", " ").trim();
                setConfig(clave, limpio.length() > 60 ? limpio.substring(0, 60) : limpio);
            }
        }
    }

    /** Datos que salen en el comprobante de venta y su largo máximo (la columna VALOR admite 200). */
    public static final java.util.Map<String, Integer> CLAVES_COMPROBANTE = java.util.Map.of(
            "negocio.nit", 30, "negocio.direccion", 100, "negocio.telefono", 30, "negocio.pie", 150);
    /** Porcentaje de gastos operativos sobre las ventas (opcional; vacío = se calcula con el mes anterior). */
    public static final String CLAVE_CARGA_GASTOS = "gastos.cargaPct";

    /**
     * Copia los datos del comprobante (NIT, dirección, teléfono, pie) y el porcentaje de gastos
     * que llegan de la pantalla. Un valor vacío borra el dato. El resto de la configuración no se toca.
     *
     * @param datos perfil recibido de la pantalla
     */
    public void copiarDatosComprobante(Profile datos) {
        if (datos == null || datos.getConfiguracion() == null) {
            return;
        }
        CLAVES_COMPROBANTE.forEach((clave, largo) -> {
            String valor = datos.getConfiguracion().get(clave);
            if (valor != null) {
                String limpio = valor.replaceAll("[\\r\\n\\t]", " ").trim();
                guardarOQuitar(clave, limpio.length() > largo ? limpio.substring(0, largo) : limpio);
            }
        });
        String carga = datos.getConfiguracion().get(CLAVE_CARGA_GASTOS);
        if (carga != null) {
            String limpio = carga.trim().replace(',', '.');
            try {
                double pct = limpio.isEmpty() ? -1 : Double.parseDouble(limpio);
                guardarOQuitar(CLAVE_CARGA_GASTOS, pct >= 0 && pct <= 100 ? limpio : "");
            } catch (NumberFormatException e) {
                throw new sistemainventario.exception.ValidationException("El porcentaje de gastos debe ser un número entre 0 y 100");
            }
        }
    }

    private void guardarOQuitar(String clave, String valor) {
        if (valor.isEmpty()) {
            if (configuracion != null) {
                configuracion.remove(clave);
            }
        } else {
            setConfig(clave, valor);
        }
    }

    /** Margen de ganancia (sobre el precio) con que el negocio fija sus precios. */
    public static final String CLAVE_MARGEN_ALTA = "alta.margen";
    /** Cómo terminan los precios sugeridos: nombre de {@link RedondeoPrecio}. */
    public static final String CLAVE_REDONDEO_ALTA = "alta.redondeo";

    /** @return el margen y el redondeo del negocio; los de fábrica si nunca los eligió */
    public PoliticaPrecio getPoliticaPrecio() {
        double margen = PoliticaPrecio.POR_DEFECTO.margenPct();
        try {
            margen = Double.parseDouble(getConfig(CLAVE_MARGEN_ALTA, "").trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            margen = PoliticaPrecio.POR_DEFECTO.margenPct();
        }
        if (margen < PoliticaPrecio.MARGEN_MINIMO || margen > PoliticaPrecio.MARGEN_MAXIMO) {
            margen = PoliticaPrecio.POR_DEFECTO.margenPct();
        }
        return new PoliticaPrecio(margen, RedondeoPrecio.desde(
                getConfig(CLAVE_REDONDEO_ALTA, ""), PoliticaPrecio.POR_DEFECTO.redondeo()));
    }

    /**
     * @param politica margen y redondeo elegidos por el dueño
     */
    public void setPoliticaPrecio(PoliticaPrecio politica) {
        setConfig(CLAVE_MARGEN_ALTA, String.valueOf(politica.margenPct()));
        setConfig(CLAVE_REDONDEO_ALTA, politica.redondeo().name());
    }

    /** Si el negocio usa fotos de producto: {@code true} o {@code false}; vacío = según el tipo de negocio. */
    public static final String CLAVE_FOTOS = "cap.fotos";

    /**
     * Las fotos ayudan donde el cliente elige con los ojos (ropa, celulares, accesorios) y
     * estorban donde se vende por código o peso (tienda, droguería, ferretería). Por eso
     * vienen activas solo en boutique y tecnología; el dueño puede cambiarlo.
     *
     * @return {@code true} si el negocio puede poner foto a sus productos
     */
    public boolean usaFotos() {
        String guardado = getConfig(CLAVE_FOTOS, "").trim();
        if (!guardado.isEmpty()) {
            return Boolean.parseBoolean(guardado);
        }
        BusinessType tipo = getPerfilNegocio();
        return tipo == BusinessType.BOUTIQUE || tipo == BusinessType.TECNOLOGIA;
    }

    /** @param usa {@code true} para activar las fotos de producto en este negocio */
    public void setUsaFotos(boolean usa) {
        setConfig(CLAVE_FOTOS, String.valueOf(usa));
    }

    /** Clave de configuración con los medios de pago que acepta el negocio. */
    public static final String CLAVE_MEDIOS_PAGO = "mediosPago";

    /**
     * Medios de pago que el negocio acepta. Si nunca se configuró, acepta
     * todos; el efectivo siempre queda activo.
     *
     * @return etiquetas oficiales de los medios activos, en el orden del catálogo
     */
    public java.util.Set<String> getMediosPago() {
        String guardado = getConfig(CLAVE_MEDIOS_PAGO, "");
        java.util.Set<String> activos = new java.util.LinkedHashSet<>();
        for (PaymentMethod m : PaymentMethod.values()) {
            if (guardado.isBlank() || m == PaymentMethod.EFECTIVO
                    || java.util.Arrays.asList(guardado.split(",")).contains(m.getEtiqueta())) {
                activos.add(m.getEtiqueta());
            }
        }
        return activos;
    }

    /**
     * Guarda los medios de pago activos. Ignora los que no reconoce y siempre
     * deja el efectivo.
     *
     * @param medios etiquetas o nombres de los medios activos
     */
    public void setMediosPago(java.util.Collection<String> medios) {
        java.util.Set<PaymentMethod> elegidos = java.util.EnumSet.of(PaymentMethod.EFECTIVO);
        if (medios != null) {
            medios.forEach(t -> PaymentMethod.desdeTexto(t).ifPresent(elegidos::add));
        }
        setConfig(CLAVE_MEDIOS_PAGO, String.join(",", elegidos.stream().map(PaymentMethod::getEtiqueta).toList()));
    }

    /**
     * @param metodo etiqueta del medio de pago
     * @return {@code true} si el negocio acepta ese medio
     */
    public boolean aceptaMedioPago(String metodo) {
        return getMediosPago().contains(metodo);
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : super.getEtiqueta();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTipoNegocio() {
        return tipoNegocio;
    }

    public void setTipoNegocio(String tipoNegocio) {
        this.tipoNegocio = tipoNegocio;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Map<String, String> getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(Map<String, String> configuracion) {
        this.configuracion = configuracion;
    }

    @Override
    public boolean isActivo() {
        return !Boolean.FALSE.equals(activo);
    }

    @Override
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
