package sistemainventario.bridge;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

/**
 * Implementación de {@link NotificadorVista} sobre el {@link WebEngine} de JavaFX.
 * Ejecuta el script en el hilo de la interfaz gráfica.
 */
public class WebViewNotificador implements NotificadorVista {

    private final WebEngine motor;

    public WebViewNotificador(WebEngine motor) {
        this.motor = motor;
    }

    @Override
    public void ejecutarScript(String script) {
        Platform.runLater(() -> {
            try {
                motor.executeScript(script);
            } catch (RuntimeException e) {
                System.err.println("[Bridge] Error ejecutando script: " + e.getMessage());
            }
        });
    }
}
