# Utilidad neta real, costo oculto y comprobante de venta (v3.7.0)

Hasta la v3.6.0 la *ganancia* era precio de venta menos precio de compra. Eso no cuenta lo que cuesta
mantener el negocio abierto ni lo que se gasta en cada venta. Desde la v3.7.0 el sistema muestra cuánto
**le queda de verdad** al dueño y puede imprimir el comprobante de cada venta.

## Qué cambia para quien usa el sistema

### 1. Gastos y utilidad neta (sección Costos; solo dueño y administrador)
- **Agregar gasto:** agua, luz, gas, internet, arriendo, nómina u otro, con su valor, el mes al que corresponde y
  la fecha de pago.
- **Los gastos cambian de un mes a otro.** Un gasto que se repite (arriendo, internet, nómina) se marca
  *Se repite cada mes*. Al abrir el mes siguiente el sistema crea su copia **con el valor del último mes**, y el
  dueño la **corrige** si cambió. Corregir un mes no cambia los demás. Si se quita el gasto fijo, deja de copiarse.
- **Utilidad neta del mes** = lo vendido − costo de la mercancía − costos ocultos − gastos del negocio. Se muestra
  en pesos y en porcentaje de lo vendido.
- **Punto de equilibrio:** cuánto hay que vender en el mes para cubrir los gastos fijos, y cuánto falta.
- Las **compras de mercancía no se anotan como gasto**: ya están en el costo de lo que se vendió.
- Las ventas **fiadas** cuentan el día que se hicieron, aunque el cliente todavía no haya pagado.
- Registrar, corregir o quitar un gasto queda en el **Historial**.

### 2. Ganancia real por producto
- En el producto hay un campo **Costo oculto por unidad** (empaque, bolsa, flete, comisión).
- La tarjeta del producto muestra: ganancia por unidad, ganancia después del costo oculto y una **estimación
  después de gastos** (el porcentaje de gastos sobre las ventas).
- La venta **guarda el costo oculto que tenía el producto ese día**: editar el producto después no cambia las
  ventas viejas.
- El porcentaje de gastos se escribe en *Editar negocio* o, si se deja vacío, se calcula con el mes anterior
  (gastos ÷ ventas). **No se reparte la luz o el arriendo producto por producto**: daría cifras que parecen
  exactas y no lo son.
- El cajero **no ve** costos ocultos ni ganancias (como los precios de compra).

### 3. Comprobante de venta
- Cada venta tiene un **número propio del negocio**, de una en una (000001, 000002...). Una venta anulada conserva
  su número y el número no se reutiliza. Las ventas que ya existían se numeran por fecha al actualizar.
- **Imprimir** en el cuaderno de ventas, o marcar *Mostrar el comprobante para imprimir al cobrar*.
- Dos formatos: **ticket de 80 mm** y **carta / A4**. En el cuadro de impresión también se puede guardar como PDF.
- NIT, dirección, teléfono y mensaje al pie se escriben en *Editar negocio*.
- Una venta anulada se imprime marcada **ANULADA**.
- Se llama *comprobante de venta*, no factura: **no es factura electrónica de la DIAN** y el texto lo dice.

## Cómo está hecho (para sustentar)
- **Base de datos PostgreSQL (cambio v9):** tabla `GASTO` (con `UQ_GASTO_COPIA`: un gasto fijo no se copia dos
  veces al mismo mes aunque dos cajas abran el mes a la vez), `PRODUCTO.COSTO_OCULTO`, `DETALLE_VENTA.COSTO_OCULTO`,
  `VENTA.NUMERO` y la tabla `CONSECUTIVO`. La vista `V_UTILIDAD_NETA_MES` permite revisar las cifras desde pgAdmin.
- **Consecutivo sin saltos ni repetidos:** el número se da dentro de la misma transacción que guarda la venta
  (`JdbcRepository.antesDeInsertar`); la fila de `CONSECUTIVO` queda bloqueada hasta el final.
- **Servicios:** `ExpenseService` (gastos, copias mensuales, utilidad neta, punto de equilibrio) y `ExpenseController`
  en el puente. Los permisos son los de Costos y VER_GANANCIA (`AccessPolicy`).
- **Archivos JSON:** los gastos van en `gastos.json` del negocio y las ventas viejas se numeran al cargar.
- **Dinero:** sigue en `NUMERIC(14,2)`; las cantidades, `NUMERIC(13,3)`.
- **Solo PostgreSQL y archivos JSON.** Oracle no tiene el cambio v9; si se conecta, el programa avisa en lugar de fallar.

## Límites
- La utilidad neta es tan buena como los datos: las ventas anteriores a la v3.4.0 no guardaron el costo y el
  sistema avisa cuántas hay.
- No hay prorrateo de gastos por producto ni depreciación de equipos.
- El comprobante no se envía por correo ni a la DIAN.
