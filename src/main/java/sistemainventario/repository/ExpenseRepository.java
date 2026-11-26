package sistemainventario.repository;

import java.nio.file.Path;
import sistemainventario.model.Expense;

/** Gastos operativos del negocio, en {@code gastos.json}. */
public class ExpenseRepository extends JsonRepository<Expense> {

    /** Nombre del archivo dentro de la carpeta del negocio. */
    public static final String ARCHIVO = "gastos.json";

    /** @param directorio carpeta del negocio */
    public ExpenseRepository(Path directorio) {
        super(directorio, ARCHIVO, Expense.class);
    }
}
