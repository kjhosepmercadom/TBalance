package sistemainventario.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import sistemainventario.exception.ValidationException;

/**
 * Gasto operativo de un mes (luz, agua, arriendo, nómina...). El valor cambia de un mes a
 * otro: un gasto recurrente es la <b>plantilla</b> y cada mes se crea una copia con el mismo
 * valor que el mes anterior, que se puede corregir sin tocar los demás meses.
 */
public class Expense extends IdentifiedEntity implements Deshabilitable {

    /** Prefijo de los IDs de los gastos. */
    public static final String PREFIJO_ID = "GAS";
    /** Largo máximo de la descripción (igual que la columna de la base). */
    public static final int MAX_DESCRIPCION = 150;

    /** Mes al que corresponde el gasto, "yyyy-MM". */
    private String periodo;
    /** Día en que se pagó, "yyyy-MM-dd" (puede ser de otro mes). */
    private String fechaPago;
    private String categoria;
    private String descripcion;
    private double monto;
    /** {@code true}: se repite cada mes (es la plantilla). */
    private boolean recurrente;
    /** Gasto recurrente del que salió esta copia mensual. */
    private String origenId;
    private Boolean activo;
    private String registradoPor;
    private String fechaRegistro;

    /** Constructor vacío requerido por Gson. */
    public Expense() {
        super();
    }

    /**
     * @param periodo   mes al que corresponde, "yyyy-MM"
     * @param fechaPago día de pago, "yyyy-MM-dd"
     * @param categoria clase de gasto
     * @param monto     valor pagado
     */
    public Expense(String periodo, String fechaPago, CategoriaGasto categoria, double monto) {
        this.periodo = periodo;
        this.fechaPago = fechaPago;
        this.categoria = categoria == null ? null : categoria.name();
        this.monto = monto;
    }

    /**
     * Revisa los datos y los deja limpios.
     *
     * @throws ValidationException si falta o está mal algún dato
     */
    public void validar() {
        try {
            periodo = YearMonth.parse(periodo == null ? "" : periodo.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new ValidationException("Elija el mes del gasto");
        }
        try {
            fechaPago = LocalDate.parse(fechaPago == null ? "" : fechaPago.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new ValidationException("Escriba la fecha en que se pagó el gasto");
        }
        categoria = CategoriaGasto.desde(categoria)
                .orElseThrow(() -> new ValidationException("Elija la clase de gasto")).name();
        if (!(monto > 0) || Double.isInfinite(monto)) {
            throw new ValidationException("Escriba cuánto costó el gasto (mayor que cero)");
        }
        monto = Math.round(monto * 100.0) / 100.0;
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (descripcion != null && descripcion.length() > MAX_DESCRIPCION) {
            throw new ValidationException("La descripción del gasto es muy larga (máximo " + MAX_DESCRIPCION + " letras)");
        }
    }

    /**
     * @param nuevoPeriodo mes de la copia, "yyyy-MM"
     * @param nuevoId      ID de la copia
     * @return la copia mensual de este gasto recurrente: mismo valor, sin ser plantilla
     */
    public Expense copiarAlMes(String nuevoPeriodo, String nuevoId) {
        Expense copia = new Expense();
        copia.setId(nuevoId);
        copia.periodo = nuevoPeriodo;
        YearMonth mes = YearMonth.parse(nuevoPeriodo);
        int dia = Math.min(LocalDate.parse(fechaPago).getDayOfMonth(), mes.lengthOfMonth());
        copia.fechaPago = mes.atDay(dia).toString();
        copia.categoria = categoria;
        copia.descripcion = descripcion;
        copia.monto = monto;
        copia.recurrente = false;
        copia.origenId = getId();
        return copia;
    }

    @Override
    public boolean isActivo() {
        return activo == null || activo;
    }

    @Override
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String getEtiqueta() {
        String clase = CategoriaGasto.desde(categoria).map(CategoriaGasto::getEtiqueta).orElse("Gasto");
        return descripcion == null ? clase : clase + " - " + descripcion;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(String fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public boolean isRecurrente() {
        return recurrente;
    }

    public void setRecurrente(boolean recurrente) {
        this.recurrente = recurrente;
    }

    public String getOrigenId() {
        return origenId;
    }

    public void setOrigenId(String origenId) {
        this.origenId = origenId;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
