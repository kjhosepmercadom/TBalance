# Arquitectura — TBalance P3 (Avance 1)

## Capas y dependencias

La dependencia entre capas es siempre descendente: la vista conoce al bridge, el bridge a los servicios, los servicios a la **interfaz** de repositorio y al modelo. El modelo no depende de ninguna otra capa (solo de `util` y `exception`).

```mermaid
flowchart TB
    subgraph Presentacion["Presentación"]
        HTML["SistemaInventario.html<br/>(HTML/CSS/JS)"]
        VIEW["view<br/>WebViewMain"]
        BRIDGE["bridge<br/>JavaBridge (fachada) · PerfilController · InventarioController<br/>CatalogoController · TransaccionController · RespaldoController · EtiquetaController<br/>EscaneoController · RespuestaBridge"]
    end
    subgraph Negocio["Lógica de negocio"]
        SERVICE["service<br/>GestorEntidad&lt;T&gt; · GestorInventario · GestorCategorias · GestorProveedores<br/>GestorMovimientos · GestorVentas · GestorPerfiles · GestorConfiguracionGlobal<br/>FabricaServiciosPerfil · ServiciosPerfil · GestorRespaldo · GestorEtiquetas · EscanerVentaQR"]
        MODEL["model<br/>EntidadConId · Producto · Categoria · Proveedor · Perfil · PerfilNegocio<br/>MovimientoInventario · Entrada · Salida · Venta · ItemVenta · ConfiguracionGlobal · Respaldo"]
    end
    subgraph Datos["Acceso a datos"]
        REPO["repository<br/>Repository&lt;T&gt; · JsonRepository&lt;T&gt; · *Repository<br/>ConfiguracionGlobalRepository · JsonFileStore · DirectorioPerfiles<br/>ArchivadorZip · RespaldoRepository · EtiquetaRepository"]
        JSON[("Archivos JSON<br/>~/.inventariopro/data")]
        ZIP[("Copias de seguridad .zip<br/>~/.inventariopro/respaldos")]
        PNG[("Etiquetas QR .png<br/>~/.inventariopro/etiquetas")]
    end
    UTIL["util · exception<br/>GeneradorQR · LectorQR (ZXing)"]
    DISP["dispositivo<br/>Camara · WebcamCamara"]
    CAM(("Webcam"))

    HTML -- "window.javaBridge" --> BRIDGE
    VIEW --> BRIDGE
    BRIDGE --> SERVICE
    SERVICE --> REPO
    SERVICE --> MODEL
    REPO --> MODEL
    REPO --> JSON
    REPO --> ZIP
    REPO --> PNG
    SERVICE --> UTIL
    BRIDGE --> DISP
    DISP --> CAM
    MODEL --> UTIL
```

`AppContext` (Singleton) es la **raíz de composición**: crea los repositorios JSON concretos, los inyecta en los servicios y reemplaza los servicios del perfil activo cuando el usuario cambia de perfil.

## Responsabilidad de cada capa

| Capa | Responsabilidad | Cambios del avance P3 |
|---|---|---|
| `model` | Datos y reglas propias de cada entidad. | Las entidades pasan a ser **Expertas**: `Producto` controla su stock y vencimiento, `Venta` crea sus ítems y calcula su total. Nueva jerarquía `MovimientoInventario` → `Entrada`/`Salida`. |
| `repository` | Leer y escribir JSON. | Interfaz `Repository<T>`; `JsonFileStore` concentra la E/S con escritura segura; `DirectorioPerfiles` maneja carpetas. Errores como `PersistenciaException`. |
| `service` | Casos de uso. | `GestorEntidad<T>` (Template Method) elimina el CRUD duplicado; dependencias inyectadas por `FabricaServiciosPerfil`. |
| `bridge` | Traducir eventos de la vista a casos de uso. | `JavaBridge` (338 líneas en P2) se convierte en fachada que delega en 4 controladores; manejo de errores centralizado en `RespuestaBridge`. |
| `view` | Ventana JavaFX. | Solo construye la ventana y registra el bridge. `Launcher` permite ejecutar desde IntelliJ. |

## Copias de seguridad (versión 3.1.0)

| Capa | Clase | Responsabilidad |
|---|---|---|
| `repository` | `ArchivadorZip` | Comprimir la carpeta de datos en un `.zip` y extraerlo, rechazando rutas inseguras (*zip slip*). |
| `repository` | `RespaldoRepository` | Listar, ubicar y eliminar los archivos de `~/.inventariopro/respaldos`, validando los nombres recibidos. |
| `model` | `Respaldo` | Datos de solo lectura de una copia: nombre, fecha y tamaño. |
| `service` | `GestorRespaldo` | Crear, listar, restaurar (con copia automática previa) y eliminar copias. |
| raíz | `AppContext.recargar()` | Volver a leer todos los datos desde disco después de restaurar. |
| `bridge` | `RespaldoController` | Atender desde la vista las acciones de la sección **Respaldos**. |

## Venta por QR (versión 3.2.0)

| Capa | Clase | Responsabilidad |
|---|---|---|
| `util` | `GeneradorQR` | Convertir un texto en la imagen de un código QR (ZXing, corrección de errores L). |
| `util` | `LectorQR` | Leer el texto de un QR que aparece en un cuadro de la cámara (ZXing). |
| `util` | `CodificadorImagen` | Convertir un cuadro de la cámara en JPEG liviano para mostrarlo en la vista. |
| `model` | `Producto.contenidoEtiquetaQR()` / `idDesdeEtiquetaQR()` | El producto define el texto de su QR (`TBalance`, ID y precio) y sabe interpretarlo de vuelta. |
| `dispositivo` | `Camara` / `WebcamCamara` | Encender la webcam, entregar cuadros y apagarla; solo `WebcamCamara` conoce la librería *webcam-capture*. |
| `repository` | `EtiquetaRepository` | Guardar las etiquetas PNG en `~/.inventariopro/etiquetas`. |
| `service` | `GestorEtiquetas` | Dibujar la etiqueta (QR + nombre + precio) y armar hojas carta de 3 × 6. |
| `service` | `EscanerVentaQR` | Pasar de un cuadro de la cámara al producto, sin contar dos veces la misma etiqueta quieta. |
| `bridge` | `EtiquetaController` | Vista previa, guardar una etiqueta y generar la hoja de todos los productos. |
| `bridge` | `EscaneoController` | Encender y apagar la cámara en un hilo propio y avisar a la vista cada producto leído. |

```mermaid
sequenceDiagram
    participant V as Vista (Venta por QR)
    participant C as EscaneoController
    participant K as WebcamCamara
    participant E as EscanerVentaQR
    participant P as Producto / GestorInventario
    V->>C: iniciarEscaneoQR()
    C->>K: abrir()
    loop unas 10 veces por segundo
        C->>K: capturar()
        C-->>V: tbCuadroCamara(jpeg)
        C->>E: procesar(cuadro)
        E->>P: idDesdeEtiquetaQR(texto) y buscarPorId(id)
        E-->>C: producto (si hay una etiqueta nueva)
        C-->>V: tbProductoEscaneado(producto)
    end
    V->>C: crearVenta(items) al pulsar Registrar Venta
```

## Persistencia (sin cambios de formato)

Se conservan los mismos archivos, nombres de atributos y ubicación que en P2, por lo que los datos existentes siguen funcionando. La única corrección automática es asignar ID a las categorías predefinidas que P2 guardaba con `"id": null`.
