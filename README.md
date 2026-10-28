# TBalance — Sistema de Inventario (Programación III)

Aplicación de escritorio en **Java 17 + JavaFX (WebView)** para gestionar el inventario de pequeños negocios mediante perfiles independientes, con persistencia local en archivos **JSON**.

> Este proyecto es la **evolución del "Sistema de Inventario TBalance"** desarrollado en Programación II. En el primer avance de Programación III se **reestructuró la arquitectura** aplicando patrones GRASP, principios SOLID y Código Limpio, **sin modificar la persistencia JSON ni la interfaz web**: los datos creados con la versión de P2 se siguen leyendo sin migraciones manuales.

- **Universidad:** Universidad Popular del Cesar — Programación de Computadores III (SS462)
- **Docente:** Ing. Esp. Alfredo Bautista
- **Repositorio:** https://github.com/kjhosepmercadom/TBalance
- **Equipo:** Kevin Mercado ([@kjhosepmercadom](https://github.com/kjhosepmercadom)) — líder e integrador · Dair Quintero ([@djosequintero](https://github.com/djosequintero)) · Reinaldo Padilla ([@rjpadillap](https://github.com/rjpadillap))

## Arquitectura en capas

| Paquete | Responsabilidad |
|---|---|
| `model` | Entidades del dominio y sus reglas (Experto en Información). |
| `exception` | Excepciones de dominio (`ValidacionException`) y de persistencia (`PersistenciaException`). |
| `repository` | Acceso a datos JSON detrás de la interfaz `Repository<T>`. |
| `service` | Casos de uso y reglas de negocio (`Gestor*`). |
| `bridge` | Controladores GRASP que atienden los eventos de la vista web. |
| `view` | Ventana JavaFX con el `WebView`. |
| `dispositivo` | Acceso a la cámara del computador detrás de la interfaz `Camara`. |
| `util` | Utilidades sin estado (fechas, IDs, texto, JSON). |

Detalle en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) y trazabilidad de principios en [`docs/TRAZABILIDAD.md`](docs/TRAZABILIDAD.md).

## Cómo ejecutar

**IntelliJ IDEA:** `File > Open` y seleccionar `pom.xml` (abrir como proyecto). Esperar a que Maven descargue las dependencias y ejecutar la clase `sistemainventario.Launcher` (botón ▶).

**Maven (terminal):**

```bash
mvn clean javafx:run
```

Requisitos: JDK 17 o superior (se recomienda **JDK 21**; con JDK 26 JavaFX 21 se cierra al iniciar). JavaFX, Gson, ZXing y webcam-capture se descargan automáticamente con Maven.

## Datos

Los datos se guardan en `~/.inventariopro/data/` (en Windows `C:\Users\<usuario>\.inventariopro\data\`):

```
configuracion-global.json      perfil activo y lista de perfiles
perfiles.json                  perfiles creados
perfiles/<ID>/productos.json   categorias.json  proveedores.json
perfiles/<ID>/entradas.json    salidas.json     ventas.json
```

## Copias de seguridad

Sección **Respaldos** del menú lateral:

- **Crear copia:** genera `~/.inventariopro/respaldos/TBalance_<fecha>_<hora>.zip` con todos los perfiles y sus datos.
- **Restaurar:** reemplaza los datos actuales por los de la copia elegida. Antes guarda automáticamente una copia del estado actual (`TBalance_antes-de-restaurar_...zip`), así que una restauración siempre se puede deshacer.
- **Eliminar:** borra el archivo de la copia.

Clases involucradas: `ArchivadorZip` y `RespaldoRepository` (persistencia), `GestorRespaldo` (servicio), `AppContext.recargar()` y `RespaldoController` (bridge).

## Venta por QR

Cada producto tiene su etiqueta con código QR; se imprime, se pega en el producto y al vender basta con mostrarla a la webcam.

1. **Generar etiquetas (Productos):** el icono QR de cada fila muestra la etiqueta y permite **Guardar imagen** (`~/.inventariopro/etiquetas/etiqueta_<id>_<fecha>.png`). El botón **Etiquetas QR** genera hojas tamaño carta de 18 etiquetas (3 × 6) con todos los productos, listas para imprimir.
2. **Vender (Ventas → Venta por QR):** se enciende la cámara del computador. Cada vez que una etiqueta aparece frente a ella, el producto se agrega a la venta con 1 unidad (si ya estaba, suma una más). Luego se ajustan las cantidades con − / + y se pulsa **Registrar Venta**. También se puede agregar un producto sin escanear desde la lista.

El QR contiene un texto corto (`TBalance`, el ID y el precio) para que la webcam lo lea con facilidad; el nombre va impreso al lado. Una etiqueta que se queda quieta frente a la cámara cuenta una sola vez; para sumar otra unidad se retira unos 2 segundos y se vuelve a mostrar, o se usa el botón +.

Librerías (se descargan con Maven y funcionan sin internet): **ZXing** para crear y leer los códigos QR y **webcam-capture** para la cámara. Clases involucradas: `GeneradorQR` y `LectorQR` (util), `Camara` y `WebcamCamara` (dispositivo), `EtiquetaRepository`, `Producto.contenidoEtiquetaQR()` / `idDesdeEtiquetaQR()`, `GestorEtiquetas` y `EscanerVentaQR` (servicio), `EtiquetaController` y `EscaneoController` (bridge).

## Gestión con Git

| Rama | Uso |
|---|---|
| `main` | Versiones entregadas al docente (solo recibe fusiones desde `dev`). |
| `dev` | Integración del equipo; recibe los Pull Requests de las ramas `feature/*`. |
| `feature/dair-*` | Trabajo de Dair Quintero (modelo de dominio). |
| `feature/reinaldo-*` | Trabajo de Reinaldo Padilla (persistencia JSON). |
| `feature/kevin-*` | Trabajo de Kevin Mercado (servicios y controladores). |

Convención de commits: `tipo(alcance): descripción` — tipos `feat`, `refactor`, `fix`, `docs`, `build`, `chore`.
