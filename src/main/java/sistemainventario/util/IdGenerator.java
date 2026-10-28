package sistemainventario.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {

    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis());

    public static String generar(String prefijo) {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefijo + "-" + fecha + "-" + (COUNTER.incrementAndGet() % 10_000);
    }

    public static String siguienteNumerico(Collection<String> ids) {
        int max = ids.stream().mapToInt(id -> {
            try { return Integer.parseInt(id.replaceAll("[^0-9]", "")); }
            catch (NumberFormatException e) { return 0; }
        }).max().orElse(1000);
        return String.valueOf(max + 1);
    }
}
