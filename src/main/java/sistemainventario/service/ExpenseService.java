package sistemainventario.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.CategoriaGasto;
import sistemainventario.model.Expense;
import sistemainventario.model.Sale;
import sistemainventario.model.SaleItem;
import sistemainventario.repository.Repository;
import sistemainventario.util.DateUtil;
import sistemainventario.util.IdGenerator;

/**
 * Gastos operativos del negocio y la utilidad neta real de cada mes.
 *
 * <p>Un gasto fijo (arriendo, internet...) se guarda una vez como <b>recurrente</b>; al abrir cada mes
 * el sistema crea su copia con el valor del mes anterior, y esa copia se puede corregir sin cambiar los
 * demás meses (la luz o el agua casi nunca cuestan lo mismo).</p>
 *
 * <p>Las compras de mercancía no se registran como gasto: ya están en el costo de lo vendido.</p>
 */
public class ExpenseService {

    /** Usuario que se anota en las copias que crea el sistema solo. */
    public static final String AUTOMATICO = "Automático";
    /** Hasta cuántos meses hacia atrás se crean copias pendientes (por si el gasto es muy viejo). */
    private static final int MAX_MESES_ATRAS = 36;

    private final Repository<Expense> gastos;
    private final SaleService ventas;
    private Supplier<Double> cargaConfigurada = () -> null;

    /**
     * Resumen de la plata de un mes.
     *
     * @param periodo            mes, "yyyy-MM"
     * @param ingresos           lo vendido (sin ventas anuladas)
     * @param costoMercancia     lo que costó lo vendido
     * @param costosOcultos      empaque, bolsa, flete y comisión de lo vendido
     * @param utilidadBruta      ingresos - costo de la mercancía - costos ocultos
     * @param gastos             gastos operativos del mes
     * @param utilidadNeta       utilidad bruta - gastos
     * @param margenBrutoPct     utilidad bruta / ingresos (0 si no hubo ventas)
     * @param margenNetoPct      utilidad neta / ingresos (0 si no hubo ventas)
     * @param gastosFijos        parte de los gastos que se repite cada mes
     * @param puntoEquilibrio    ventas necesarias para cubrir los gastos fijos ({@code null} si no se puede calcular)
     * @param faltaParaEquilibrio lo que falta vender para llegar al equilibrio (0 si ya se llegó)
     * @param ventasSinCosto     ventas del mes que no guardaron costo (la utilidad bruta puede quedar alta)
     * @param porCategoria       gastos del mes por clase
     */
    public record MonthlySummary(String periodo, double ingresos, double costoMercancia, double costosOcultos,
                                 double utilidadBruta, double gastos, double utilidadNeta, double margenBrutoPct,
                                 double margenNetoPct, double gastosFijos, Double puntoEquilibrio,
                                 double faltaParaEquilibrio, int ventasSinCosto, List<CategoryTotal> porCategoria) {
    }

    /**
     * @param categoria nombre de la clase ("LUZ")
     * @param etiqueta  nombre para mostrar ("Luz")
     * @param total     gastado en el mes
     */
    public record CategoryTotal(String categoria, String etiqueta, double total) {
    }

    /**
     * @param gastos repositorio de gastos del negocio
     * @param ventas servicio de ventas (para los ingresos y costos)
     */
    public ExpenseService(Repository<Expense> gastos, SaleService ventas) {
        this.gastos = Objects.requireNonNull(gastos);
        this.ventas = Objects.requireNonNull(ventas);
    }

    /** @param porcentaje de dónde sale el porcentaje de gastos que fijó el dueño ({@code null} si no lo fijó) */
    public void usarCargaConfigurada(Supplier<Double> porcentaje) {
        this.cargaConfigurada = Objects.requireNonNull(porcentaje);
    }

    /**
     * Gastos de un mes (activos), creando antes las copias de los gastos fijos si ese mes ya empezó.
     *
     * @param periodo mes, "yyyy-MM"
     * @return gastos del mes, del más reciente al más antiguo
     */
    public List<Expense> listarMes(String periodo) {
        YearMonth mes = mes(periodo);
        gastos.refrescar();
        if (!mes.isAfter(YearMonth.now())) {
            crearCopiasHasta(mes);
        }
        return gastos.filtrar(g -> g.isActivo() && mes.toString().equals(g.getPeriodo())).stream()
                .sorted(Comparator.comparing(Expense::getFechaPago, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed().thenComparing(Expense::getId))
                .toList();
    }

    /** @return gastos fijos (plantillas) activos */
    public List<Expense> listarRecurrentes() {
        return gastos.filtrar(g -> g.isActivo() && g.isRecurrente() && g.getOrigenId() == null);
    }

    /**
     * Guarda un gasto nuevo o los cambios de uno que existe. Corregir un gasto cambia solo el de ese mes.
     *
     * @param gasto datos del gasto
     * @param quien nombre de quien lo registra (lo pone Java con la sesión)
     * @return el gasto guardado
     * @throws ValidationException si falta algún dato
     */
    public Expense guardar(Expense gasto, String quien) {
        if (gasto == null) {
            throw new ValidationException("No se recibieron datos del gasto");
        }
        gasto.validar();
        if (gasto.tieneId()) {
            Expense actual = obtener(gasto.getId());
            gasto.setOrigenId(actual.getOrigenId());
            gasto.setRegistradoPor(actual.getRegistradoPor());
            gasto.setFechaRegistro(actual.getFechaRegistro());
            gasto.setActivo(actual.isActivo());
            if (gasto.getOrigenId() != null) {
                gasto.setRecurrente(false);
                gasto.setPeriodo(actual.getPeriodo());
            }
            gastos.actualizar(gasto);
        } else {
            gasto.setId(IdGenerator.conPrefijo(Expense.PREFIJO_ID));
            gasto.setOrigenId(null);
            gasto.setRegistradoPor(quien);
            gasto.setFechaRegistro(DateUtil.ahora());
            gasto.setActivo(true);
            gastos.agregar(gasto);
        }
        return gasto;
    }

    /**
     * Quita un gasto (borrado lógico). Si era un gasto fijo, deja de copiarse a los meses que siguen.
     *
     * @param id ID del gasto
     * @return el gasto quitado
     */
    public Expense eliminar(String id) {
        Expense gasto = obtener(id);
        gasto.setActivo(false);
        gastos.actualizar(gasto);
        return gasto;
    }

    /**
     * @param id ID del gasto
     * @return el gasto
     * @throws ValidationException si no existe
     */
    public Expense obtener(String id) {
        return gastos.buscarPorId(id).orElseThrow(() -> new ValidationException("Ese gasto no existe"));
    }

    /**
     * Calcula la utilidad neta real del mes.
     *
     * @param periodo mes, "yyyy-MM"
     * @return el resumen del mes
     */
    public MonthlySummary resumen(String periodo) {
        YearMonth mes = mes(periodo);
        List<Expense> delMes = listarMes(mes.toString());
        double ingresos = 0;
        double costo = 0;
        double ocultos = 0;
        int sinCosto = 0;
        for (Sale venta : ventas.listarTodas()) {
            if (venta.getFecha() == null || !venta.getFecha().startsWith(mes.toString())) {
                continue;
            }
            boolean falta = false;
            for (SaleItem item : venta.getItems()) {
                double cantidad = item.getCantidad().doubleValue();
                costo += cantidad * item.getCostoUnitario();
                ocultos += cantidad * item.getCostoOculto();
                falta |= !item.tieneCosto();
            }
            ingresos += venta.getTotal();
            sinCosto += falta ? 1 : 0;
        }
        double totalGastos = delMes.stream().mapToDouble(Expense::getMonto).sum();
        double fijos = delMes.stream().filter(g -> g.isRecurrente() || g.getOrigenId() != null)
                .mapToDouble(Expense::getMonto).sum();
        double bruta = ingresos - costo - ocultos;
        double margenBruto = ingresos > 0 ? bruta / ingresos * 100 : 0;
        double neta = bruta - totalGastos;
        Double equilibrio;
        if (ingresos > 0 && bruta > 0) {
            equilibrio = redondear(fijos / (bruta / ingresos));
        } else {
            equilibrio = fijos > 0 ? null : Double.valueOf(0);
        }
        double falta = equilibrio == null ? 0 : Math.max(0, equilibrio - ingresos);
        Map<CategoriaGasto, Double> porClase = new EnumMap<>(CategoriaGasto.class);
        for (Expense g : delMes) {
            CategoriaGasto.desde(g.getCategoria()).ifPresent(c -> porClase.merge(c, g.getMonto(), Double::sum));
        }
        List<CategoryTotal> categorias = new ArrayList<>();
        porClase.forEach((c, total) -> categorias.add(new CategoryTotal(c.name(), c.getEtiqueta(), redondear(total))));
        categorias.sort(Comparator.comparingDouble(CategoryTotal::total).reversed());
        return new MonthlySummary(mes.toString(), redondear(ingresos), redondear(costo), redondear(ocultos),
                redondear(bruta), redondear(totalGastos), redondear(neta), redondear(margenBruto),
                redondear(ingresos > 0 ? neta / ingresos * 100 : 0), redondear(fijos), equilibrio,
                redondear(falta), sinCosto, categorias);
    }

    /**
     * Porcentaje de gastos operativos sobre las ventas, para estimar la ganancia de un producto:
     * el que fijó el dueño o, si no, el del mes anterior (gastos ÷ ventas). Los gastos no se reparten
     * producto por producto: sería una cifra que parece exacta y no lo es.
     *
     * @return el porcentaje, o {@code null} si no hay gastos registrados o ventas con que calcularlo
     */
    public Double cargaGastosPct() {
        Double fijado = cargaConfigurada.get();
        if (fijado != null) {
            return fijado;
        }
        YearMonth anterior = YearMonth.now().minusMonths(1);
        MonthlySummary resumen = resumen(anterior.toString());
        return resumen.ingresos() > 0 && resumen.gastos() > 0
                ? redondear(resumen.gastos() / resumen.ingresos() * 100) : null;
    }

    private void crearCopiasHasta(YearMonth hasta) {
        YearMonth limite = hasta.minusMonths(MAX_MESES_ATRAS);
        for (Expense plantilla : listarRecurrentes()) {
            YearMonth desde = YearMonth.parse(plantilla.getPeriodo());
            if (desde.isBefore(limite)) {
                desde = limite;
            }
            for (YearMonth m = desde.plusMonths(1); !m.isAfter(hasta); m = m.plusMonths(1)) {
                copiar(plantilla, m);
            }
        }
    }

    /** Crea la copia de un mes con el valor del último mes que tuvo ese gasto (no del primero). */
    private void copiar(Expense plantilla, YearMonth mes) {
        String periodo = mes.toString();
        boolean existe = gastos.filtrar(g -> plantilla.getId().equals(g.getOrigenId()) && periodo.equals(g.getPeriodo()))
                .stream().findAny().isPresent();
        if (existe) {
            return;
        }
        Expense modelo = gastos.filtrar(g -> g.isActivo() && (plantilla.getId().equals(g.getOrigenId())
                        || plantilla.getId().equals(g.getId())) && g.getPeriodo().compareTo(periodo) < 0).stream()
                .max(Comparator.comparing(Expense::getPeriodo)).orElse(plantilla);
        Expense copia = modelo.copiarAlMes(periodo, IdGenerator.conPrefijo(Expense.PREFIJO_ID));
        copia.setOrigenId(plantilla.getId());
        copia.setRegistradoPor(AUTOMATICO);
        copia.setFechaRegistro(DateUtil.ahora());
        copia.setActivo(true);
        try {
            gastos.agregar(copia);
        } catch (PersistenceException e) {
            // otra caja la creó al mismo tiempo (UQ_GASTO_COPIA): se vuelve a leer y ya está
            gastos.refrescar();
        }
    }

    private static YearMonth mes(String periodo) {
        try {
            return YearMonth.parse(periodo == null ? "" : periodo.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new ValidationException("Elija un mes válido (por ejemplo 2026-11)");
        }
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
