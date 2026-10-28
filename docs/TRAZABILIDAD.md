# Tabla de trazabilidad de principios aplicados — Avance 1 P3

Cada fila indica dónde se aplica el principio en el código y por qué.

## Patrones GRASP

| Principio | Clase / método | Justificación técnica |
|---|---|---|
| **Creator** | `Venta.agregarItem(Producto, int, double)` | La venta contiene y registra sus `ItemVenta`; por eso es quien los crea. El constructor con datos de `ItemVenta` tiene visibilidad de paquete para que solo `Venta` lo use. |
| **Creator** | `FabricaServiciosPerfil.crear(String perfilId)` | Posee el dato de inicialización (la carpeta del perfil) y crea los repositorios JSON y los gestores del perfil. Ningún gestor hace `new XxxRepository(...)`. |
| **Creator** | `GestorPerfiles.crear(Perfil)` | Registra los perfiles en el repositorio; crea la instancia de `Perfil`, su carpeta y, si aplica, sus categorías iniciales. |
| **Creator** | `GestorCategorias.crearPredefinidas(PerfilNegocio)` | Es el agregador de las categorías del perfil; crea las categorías predefinidas del tipo de negocio (en P2 se creaban sin ID). |
| **Controller** | `JavaBridge` | Controlador fachada: único objeto que recibe los eventos de la interfaz web (`window.javaBridge`). No contiene lógica, solo delega. |
| **Controller** | `PerfilController`, `InventarioController`, `CatalogoController`, `TransaccionController` | Controladores de caso de uso: cada uno coordina un grupo de operaciones del sistema y delega en los servicios. |
| **Experto** | `Producto.disminuirStock(int)`, `aumentarStock(int)`, `verificarStockDisponible(int)`, `getEstado()` | El producto posee `stock` y `stockMin`; él decide si puede descontar unidades y cuál es su estado. Los servicios ya no usan `setStock(getStock() - n)`. |
| **Experto** | `Producto.estaVencido(LocalDate)`, `venceDentroDe(int, LocalDate)`, `validar(LocalDate)` | Solo el producto conoce su fecha de vencimiento y si la controla. |
| **Experto** | `Venta.calcularTotal()`, `cantidadesPorProducto()`, `estaCompletada()` / `ItemVenta.getSubtotal()` | La venta conoce sus ítems y cada ítem su cantidad y precio. |
| **Experto** | `ConfiguracionGlobal.getPerfilActivo()`, `tienePerfiles()` | Conoce la lista de perfiles y el ID activo. |
| **Experto** | `PerfilNegocio.describir()`, `Perfil.actualizarDatos(...)`, `JsonRepository.siguienteIdNumerico()` | Cada clase responde con la información que ella misma posee (tipo de negocio, datos del perfil, IDs almacenados). |
| **Bajo acoplamiento** | `GestorEntidad(Repository<T>)`, `GestorMovimientos(...)`, `GestorVentas(...)` | Los servicios dependen de la interfaz `Repository<T>` y reciben sus dependencias por constructor. En P2 `GestorMovimientos` y `GestorVentas` creaban sus propios repositorios, duplicando cachés del mismo archivo. |
| **Bajo acoplamiento** | `NotificadorVista` / `WebViewNotificador` | Los controladores avisan a la vista mediante una interfaz y no dependen de `WebEngine` de JavaFX. |
| **Bajo acoplamiento** | `AppContext.getServiciosActivos()` | Los campos públicos y mutables de P2 se reemplazan por métodos de acceso; el bridge no conoce los repositorios. |
| **Alta cohesión** | Paquete `bridge` | El `JavaBridge` de P2 (338 líneas) mezclaba JSON, reglas, IDs y errores. Ahora cada controlador atiende un único grupo de casos de uso. |
| **Alta cohesión** | `JsonFileStore`, `DirectorioPerfiles`, `RespuestaBridge` | Clases pequeñas con un propósito único: E/S de archivos, carpetas de perfil y formato de respuestas a la vista. |
| **Controller** | `RespaldoController` | Controlador del caso de uso "copias de seguridad"; `JavaBridge` le delega crear, listar, restaurar y eliminar. |
| **Experto** | `RespaldoRepository.listar()`, `describir(Path)` | Conoce la carpeta de respaldos y los archivos que contiene; construye cada `Respaldo` con su nombre, fecha y tamaño. |
| **Bajo acoplamiento** | `GestorRespaldo(Path, RespaldoRepository, ArchivadorZip)` | Recibe sus dependencias por constructor; no sabe cómo se comprime ni cómo se recargan los servicios (`AppContext.restaurarRespaldo`). |
| **Experto** | `Producto.contenidoEtiquetaQR()`, `idDesdeEtiquetaQR(String)` | El producto define el texto de su QR (`TBalance`, ID y precio) y, como conoce ese formato, también es quien lo interpreta al escanear. |
| **Creator** | `AppContext` → `GestorEtiquetas(new GeneradorQR(), new EtiquetaRepository(...))` | La raíz de composición crea el servicio de etiquetas con sus colaboradores. |
| **Controller** | `EtiquetaController`, `EscaneoController` | Controladores de los casos de uso "etiquetas QR" y "venta por QR"; `JavaBridge` les delega generar etiquetas y encender o apagar la cámara. |
| **Bajo acoplamiento** | `GeneradorQR` / `LectorQR`, `Camara` | Solo esas dos clases importan ZXing y solo `WebcamCamara` importa *webcam-capture*. `EscaneoController` recibe la cámara como `Supplier<Camara>`, por eso se prueba con una cámara simulada. |
| **Alta cohesión** | `EscanerVentaQR` | Coordina leer el QR (`LectorQR`), interpretar el texto (`Producto`) y buscar el producto (`GestorInventario`), sin hacer ninguna de esas tareas. |

## Principios SOLID

| Principio | Clase / método | Justificación técnica |
|---|---|---|
| **SRP** | `JsonFileStore` / `JsonRepository<T>` / `Gestor*` | Tres razones de cambio separadas: cómo se escribe en disco, cómo se administra la colección en memoria y qué reglas de negocio aplican. |
| **SRP** | `DirectorioPerfiles` (extraída de `GestorPerfiles`) | Las operaciones de sistema de archivos salieron del servicio de perfiles. |
| **SRP** | `GestorConfiguracionGlobal` | Es el único que lee/escribe `configuracion-global.json`; en P2 `GestorPerfiles` también lo hacía. |
| **SRP** | `ArchivadorZip` / `RespaldoRepository` / `GestorRespaldo` | Comprimir y extraer archivos, administrar la carpeta de respaldos y coordinar el caso de uso son tres responsabilidades separadas. |
| **SRP** | `GeneradorQR` / `GestorEtiquetas` / `EtiquetaRepository` | Generar el código, dibujar la etiqueta y guardar la imagen son tres razones de cambio separadas. |
| **SRP** | `WebcamCamara` / `LectorQR` / `EscanerVentaQR` / `EscaneoController` | Manejar la cámara, decodificar, decidir qué producto es y comunicarse con la vista en un hilo propio están separados. |
| **SRP** | `RespuestaBridge.ejecutar(Supplier<String>)` | Centraliza la traducción de excepciones a `"ERROR: ..."`; en P2 cada método del bridge repetía su `try/catch`. |
| **OCP** | `JsonRepository<T>` y sus subclases (`ProductoRepository`, ...) | Para una entidad nueva se crea una subclase de pocas líneas; la clase base no se modifica. |
| **OCP** | `Repository<T>` | Permitirá agregar en fases futuras una implementación DAO con base de datos relacional sin tocar la capa de servicios. |
| **OCP** | `GestorEntidad<T>.guardar(T)` (método `final`, Template Method) | El algoritmo crear/actualizar es cerrado; cada gestor lo extiende con `validar()` y `generarId()`. |
| **OCP** | `MovimientoInventario.aplicarA(Producto)` | `GestorMovimientos.registrar(...)` trabaja con el tipo abstracto; un nuevo movimiento (devolución, ajuste) se añade como subclase sin modificar el servicio. |
| **OCP** | `PerfilNegocio` | Un tipo de negocio nuevo es una constante más; la vista lo recibe mediante `describir()`. |
| **DIP** (complementario) | `service` → `Repository<T>`; `bridge` → `NotificadorVista`, `AbridorArchivos`, `Camara` | Los módulos de alto nivel dependen de abstracciones. |

## Código limpio

| Práctica | Evidencia |
|---|---|
| Nombres significativos | `disminuirStock`, `cantidadesPorProducto`, `registrarCambioDeStock`, `resolverPerfilInicial`. Español consistente para el dominio. |
| Métodos pequeños y de una sola responsabilidad | `GestorVentas.registrar()` delega en `verificarStock()` y `descontarStock()`. |
| Eliminación de código muerto | Se eliminaron `Usuario`, `UsuarioRepository` y `GestorUsuarios` (nunca se usaban), `GestorInventario.hoy()`, `diasParaVencer()`, `Venta.addItem()` e `IdGenerator.generar()`. |
| Constantes en vez de valores mágicos | `Venta.ESTADO_COMPLETADA`, `Entrada.PREFIJO_ID`, `Perfil.COLOR_POR_DEFECTO`, dimensiones de ventana en `WebViewMain`. |
| Manejo de excepciones | `ValidacionException` (reglas) y `PersistenciaException` (E/S) en lugar de `System.err` silencioso o códigos `"OK"` internos. |
| Validación de datos de entrada | `RespaldoRepository.rutaNueva()` solo acepta nombres `.zip` válidos; `ArchivadorZip.extraer()` rechaza rutas fuera de la carpeta destino (*zip slip*); `GestorRespaldo` verifica que el zip sea de TBalance antes de reemplazar datos; `EtiquetaRepository` solo acepta nombres `.png` válidos y `GeneradorQR` rechaza contenido vacío; un QR ajeno o de un producto borrado muestra un aviso en lugar de agregarse a la venta, y la vista no deja vender más unidades que el stock. |
| Javadoc | Todas las clases y los métodos públicos relevantes están documentados. |
| Convenciones Java | `PascalCase` para clases, `camelCase` para métodos y atributos, `UPPER_SNAKE_CASE` para constantes. |
