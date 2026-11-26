# Cada negocio maneja su inventario a su manera (v3.6.0, fase 1)

Hasta la v3.5.0 todos los productos eran iguales: un nombre, un precio y un número de unidades.
Desde la v3.6.0 el sistema se adapta al tipo de negocio sin que el cajero tenga que hacer nada distinto.

## Qué cambia para quien usa el sistema

- **Al crear o editar un negocio** aparece la sección *Qué maneja este negocio*. Ya viene marcada
  según el tipo (una boutique trae tallas y colores; una ferretería, venta por metro o kilo) y se puede cambiar.
- **Tallas y colores.** En el producto se elige *¿Cómo se controla este producto?* → *Con tallas y colores*.
  Cada talla tiene su propio stock, su mínimo y, si hace falta, su propio precio. El stock del producto es la suma.
- **Venta por metro, kilo o litro.** El producto tiene una *Unidad de medida* y las cantidades aceptan decimales (1,5 m).
- **Ventas, entradas y salidas** piden la talla cuando el producto la tiene y descuentan solo de esa talla.
  Al anular una venta, la talla recupera sus unidades.
- **No se puede quitar lo que ya se usa.** Si hay productos con tallas, el sistema no deja apagar *Tallas y colores*
  hasta que esos productos pasen a *Simple*. Pasa igual con la venta fraccionada y el vencimiento.

## Cómo está hecho (para sustentar)

- **Strategy:** `InventoryHandling` define qué hace un producto al reservar, descontar y reponer stock.
  `SimpleHandling` (un solo stock) y `VariantHandling` (stock por talla) lo implementan.
  `InventoryHandlingFactory` entrega la estrategia de cada producto. Agregar lotes o seriales es una clase nueva.
- **Capacidades del negocio:** `BusinessCapabilities` se guarda en la configuración del negocio (claves `cap.*`).
- **Base de datos PostgreSQL (cambio v8):** las cantidades pasan a `NUMERIC(13,3)`, el producto tiene
  `MANEJO` y `UNIDAD_MEDIDA`, y existe la tabla `PRODUCTO_VARIANTE`. Talla y color vacíos se guardan como `''`
  (no `NULL`) para que la restricción `UNIQUE` no deje repetir una talla.
- **Dinero:** los precios siguen siendo `double`; solo las cantidades usan `BigDecimal`.

## Límites de esta fase

- La venta por QR no admite productos con tallas todavía: se venden desde *Nueva venta*.
- Lotes con vencimiento (farmacia) y seriales (tecnología) son las fases 2 y 3.
- Esta fase se probó con PostgreSQL y con archivos JSON.
