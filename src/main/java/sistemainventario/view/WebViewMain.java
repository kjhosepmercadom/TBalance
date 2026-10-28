package sistemainventario.view;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import sistemainventario.AppContext;
import sistemainventario.bridge.JavaBridge;
import sistemainventario.model.Perfil;

import java.net.URL;

public class WebViewMain {

    private final AppContext ctx;
    private WebEngine engine;
    private JavaBridge bridge;

    public WebViewMain(AppContext ctx) {
        this.ctx = ctx;
    }

    public void show(Stage stage) {
        WebView webView = new WebView();
        engine  = webView.getEngine();

        engine.setOnAlert(e -> System.out.println("[HTML Alert] " + e.getData()));

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                registrarBridge();
                inicializarEstadoHTML();
            }
        });

        URL htmlUrl = getClass().getResource("/html/SistemaInventario.html");
        if (htmlUrl == null) {
            throw new RuntimeException("No se encontro /html/SistemaInventario.html en resources");
        }
        engine.load(htmlUrl.toExternalForm());

        Scene scene = new Scene(webView, 1300, 800);
        stage.setScene(scene);
        actualizarTitulo(stage);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public void actualizarTitulo(Stage stage) {
        Perfil activo = ctx.gestorPerfiles.getPerfilActivo();
        String nombre = activo != null ? activo.getNombre() : "Sin perfil activo";
        stage.setTitle("Tbalance \u2014 " + nombre);
    }

    private void registrarBridge() {
        bridge = new JavaBridge(engine, ctx);
        JSObject window = (JSObject) engine.executeScript("window");
        window.setMember("javaBridge", bridge);
        System.out.println("[WebViewMain] Bridge registrado correctamente.");
    }

    private void inicializarEstadoHTML() {
        try {
            engine.executeScript("if(typeof cargarEstadoDesdeJava === 'function') cargarEstadoDesdeJava();");
        } catch (Exception e) {
            System.err.println("[WebViewMain] Error en inicializacion: " + e.getMessage());
        }
    }
}
