package sistemainventario.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FechaUtil {

    public static final DateTimeFormatter FMT_FECHA    = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static String hoy() {
        return LocalDate.now().format(FMT_FECHA);
    }

    public static String ahora() {
        return LocalDateTime.now().format(FMT_DATETIME);
    }

    public static String formatearFecha(String isoDate) {
        try {
            return LocalDate.parse(isoDate, FMT_FECHA)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) { return isoDate; }
    }
}
