package sistemainventario;

import javafx.application.Application;
import javafx.stage.Stage;
import sistemainventario.view.WebViewMain;

/**
 * Aplicación JavaFX del Sistema de Inventario TBalance.
 *
 * <p>Para ejecutar desde IntelliJ IDEA use {@link Launcher} (evita el error
 * "JavaFX runtime components are missing"). Con Maven: {@code mvn javafx:run}.</p>
 */
public class Main extends Application {

    @Override
    public void start(Stage ventanaPrincipal) {
        new WebViewMain(AppContext.get()).show(ventanaPrincipal);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
