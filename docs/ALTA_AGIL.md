# Registrar productos más rápido (v3.8.0)

Antes, agregar un producto pedía llenar un formulario largo, igual para todos los negocios. En la v3.8.0 el registro se hizo para que un cajero o un dueño lo termine en segundos, sin importar si el negocio es tienda, boutique, farmacia, ferretería o tecnología.

## Qué cambió para quien registra

| Antes | Ahora |
|---|---|
| Escribir el precio de venta a mano | Se escribe el costo y TBalance **calcula el precio** con el margen del negocio. Cuatro botones (20, 30, 40, 50 %) prueban otros márgenes con un toque. |
| Un solo producto a la vez, formulario de arriba abajo | Los datos de siempre (nombre, código, categoría, costo, precio, cantidad) van arriba; **proveedor, costo oculto, stock mínimo y descripción** quedan en "Más detalles". |
| Volver a abrir el panel por cada producto | **Guardar y agregar otro** (Ctrl + Enter) deja la misma categoría y las mismas tallas, y solo se borra lo que cambia. |
| Repetir todo para una camisa de otro color | **Copiar uno parecido**: se cambia solo el nombre, el color o el precio. |
| Crear productos repetidos sin darse cuenta | Al **escanear el código**, si ya existe, TBalance lo avisa y ofrece abrir ese producto. |
| Cargar 200 productos uno por uno | **Varios desde Excel**: se pegan las filas, se revisan y se cargan. Los que ya existen o tienen error se saltan y se muestran. |

## El margen y el redondeo del negocio

El margen se mide **sobre el precio de venta**, igual que la barra de utilidad del formulario:

`precio = costo ÷ (1 − margen)`

Ejemplo: costo $52.000 y margen 42 % dan $89.655,17. El redondeo siempre es **hacia arriba**, así el margen elegido nunca se pierde:

| Redondeo | Resultado |
|---|---|
| Exacto (al peso) | $89.656 |
| A 100 | $89.700 |
| A 500 | $90.000 |
| A 1.000 | $90.000 |
| Termina en 900 | $89.900 |

Cada negocio guarda su margen (por defecto 40 %) y su redondeo (por defecto "A 100") con el botón **Cambiar mi margen**. Se guardan en `PERFIL_CONFIGURACION` con las claves `alta.margen` y `alta.redondeo`, igual que las capacidades (v3.6.0) y los datos del comprobante (v3.7.0). Un cajero con permiso de editar el catálogo puede cambiarlos.

## Pegar una lista desde Excel

Una fila por producto, columnas separadas por tabulador (lo que Excel copia) o por punto y coma:

`Nombre · Costo · Precio · Cantidad · Código · Categoría`

- Si la primera fila tiene encabezados (Nombre, Costo, Precio, Cantidad, Código, Categoría), las columnas pueden venir en cualquier orden.
- Los números se leen como en Colombia: `52.000` son cincuenta y dos mil y `89,5` es ochenta y nueve y medio.
- Si el precio queda vacío, se calcula con el margen del negocio y la fila lo avisa.
- Si la categoría no existe, el producto queda en "Sin categoría".
- Un producto con el mismo nombre (sin importar mayúsculas) **no se duplica**.
- Máximo 500 filas por pegada.
- **Revisar** muestra qué se cargaría sin guardar nada. **Cargar** guarda las filas buenas; una fila mala no frena a las demás.
- Esta carga crea productos simples. Los que llevan tallas o vencimiento obligatorio se registran en el panel, que ya pide esos datos.

## Cómo está hecho

| Pieza | Qué hace |
|---|---|
| `RedondeoPrecio`, `PoliticaPrecio` (modelo) | El margen y el redondeo. Solo cálculo: no consultan ni guardan nada. |
| `Profile.getPoliticaPrecio()` | Lee `alta.margen` y `alta.redondeo`; si faltan o están mal, usa los de fábrica. |
| `ImportadorProductos` | Lee el texto pegado. No toca la base, así se prueba solo. |
| `AltaAgilService` | Sugiere el precio, reconoce un código, arma la copia de un producto y revisa o carga la lista. Todo guardado pasa por `InventoryService.guardar`, así las reglas de siempre valen igual. |
| `AltaController` | Puente entre la página y el servicio; los números llegan como texto. |
| `cambios/v10_alta_agil.sql` | Solo PostgreSQL: dos índices (por código y por nombre) para que escanear y revisar duplicados sea rápido. No usa índices UNIQUE, para no bloquear a un negocio que ya tenga nombres repetidos. |
| `tbalance-alta.js` y `tbalance-alta.css` | La interfaz. Se cargan después de `tbalance-gastos.js` y usan los colores de `tbalance-vistas.css`, así se ven bien en claro y oscuro. |

## Lo que todavía no cambia

La pantalla de **venta** sigue igual. Una venta por tipo de negocio (toque en talla y color, teclado de metros, elegir serial) se hará aparte, para no mezclar dos cambios grandes en una sola versión.
