package sistemainventario.model;

import java.util.Arrays;
import java.util.Optional;

/** Clases de gasto operativo: lo que cuesta mantener el negocio abierto. */
public enum CategoriaGasto {
    AGUA("Agua"),
    LUZ("Luz"),
    GAS("Gas"),
    INTERNET("Internet"),
    ARRIENDO("Arriendo"),
    NOMINA("Nómina"),
    OTRO("Otro");

    private final String etiqueta;

    CategoriaGasto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** @return nombre para mostrar */
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @param texto nombre del enum ("LUZ") o la etiqueta ("Luz"), sin importar mayúsculas
     * @return la categoría, o vacío si no existe
     */
    public static Optional<CategoriaGasto> desde(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String t = texto.trim();
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(t) || c.etiqueta.equalsIgnoreCase(t))
                .findFirst();
    }
}
