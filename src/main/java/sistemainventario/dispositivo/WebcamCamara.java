package sistemainventario.dispositivo;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import sistemainventario.exception.ValidacionException;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;

/**
 * {@link Camara} implementada con la webcam del computador mediante la
 * librería <i>webcam-capture</i>.
 *
 * <p><b>Bajo acoplamiento:</b> es la única clase que conoce esa librería.</p>
 */
public class WebcamCamara implements Camara {

    /** Tiempo máximo para detectar la cámara al abrir. */
    private static final long ESPERA_DETECCION_MS = 8000;
    /** Ancho máximo del cuadro: suficiente para leer etiquetas pequeñas sin ser lento. */
    private static final int ANCHO_MAXIMO = 1280;

    private Webcam webcam;

    @Override
    public void abrir() {
        try {
            webcam = Webcam.getDefault(ESPERA_DETECCION_MS);
            if (webcam == null) {
                throw new ValidacionException("No se encontro ninguna camara conectada");
            }
            elegirResolucion();
            if (!webcam.open()) {
                throw new ValidacionException("No se pudo encender la camara. Cierra otras aplicaciones que la esten usando");
            }
        } catch (TimeoutException e) {
            throw new ValidacionException("La camara no respondio a tiempo");
        } catch (WebcamException e) {
            throw new ValidacionException("No se pudo usar la camara: " + e.getMessage());
        } catch (LinkageError e) {
            // La librería usa código nativo; si el sistema no lo admite, se informa en vez de cerrar el hilo.
            throw new ValidacionException("Este computador no permite usar la camara desde Java: " + e.getMessage());
        }
    }

    /** Usa la resolución más alta que la cámara ofrezca sin pasar de {@link #ANCHO_MAXIMO}. */
    private void elegirResolucion() {
        Arrays.stream(webcam.getViewSizes())
                .filter(tamano -> tamano.width <= ANCHO_MAXIMO)
                .max(Comparator.comparingInt((Dimension tamano) -> tamano.width * tamano.height))
                .ifPresent(webcam::setViewSize);
    }

    @Override
    public BufferedImage capturar() {
        return webcam != null && webcam.isOpen() ? webcam.getImage() : null;
    }

    @Override
    public void cerrar() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
