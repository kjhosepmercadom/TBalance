package sistemainventario;

/**
 * Punto de entrada que no extiende {@code Application}.
 *
 * <p>Permite ejecutar el proyecto con el botón Run de IntelliJ IDEA o como
 * JAR, cargando JavaFX desde el classpath sin configurar {@code --module-path}.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
