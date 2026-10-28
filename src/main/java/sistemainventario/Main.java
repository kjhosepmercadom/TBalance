package sistemainventario;

import javafx.application.Application;
import javafx.stage.Stage;
import sistemainventario.view.WebViewMain;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppContext ctx = AppContext.get();
        if (ctx.esPrimerArranque()) {
            System.out.println("[Main] Primer arranque: no hay perfiles creados.");
        }
        WebViewMain webViewMain = new WebViewMain(ctx);
        webViewMain.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
