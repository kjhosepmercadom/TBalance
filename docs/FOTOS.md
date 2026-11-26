# Fotos de producto (v3.9.0)

Un cliente de una boutique o de una tienda de tecnología elige con los ojos. Por eso, en esos dos tipos de negocio cada producto puede tener **una foto, opcional**. En farmacia, ferretería, supermercado y tienda de barrio no aparece: allí se vende por código, nombre o peso y la foto solo ocuparía espacio.

## Cómo se usa

1. En **Agregar producto**, la columna derecha muestra "Así lo verá el cajero" con el espacio de la foto.
2. **Agregar foto** (o arrastrar una imagen encima). La página la reduce sola a 400 px y la deja en unos 30 KB; la persona no tiene que preparar nada.
3. **Cambiar** o **Quitar** cuando haga falta. Si el producto no cambia de foto, el guardado es el de siempre.
4. En la lista de productos aparece una miniatura al lado del nombre.

El dueño puede **activar o apagar las fotos** de cualquier negocio (en "Más detalles" del producto, o con el enlace de la columna derecha cuando están apagadas). Apagarlas no borra las que ya se subieron.

## Por qué no llenan la base

Neon da 1 GB. Las fotos se cuidan en tres lugares, y el último no depende de la página:

| Capa | Qué hace |
|---|---|
| Navegador | Reduce a 400 px y busca calidad hasta pesar 45 KB o menos (WebP; JPG si el navegador no hace WebP). |
| `FotoService` | Rechaza lo que pese más de **60 KB**, confirma por los primeros bytes que sea WebP, JPG o PNG (no se fía del nombre ni del tipo que diga la página) y limita las fotos de un negocio a **150 MB**. |
| Base de datos | `CHECK` de tipo y de peso (máx. 200 KB) en la tabla `PRODUCTO_FOTO`. |

150 MB son unas 5.000 fotos de 30 KB. Con 1 GB caben varios negocios con fotos junto a todo lo demás.

## Dónde se guardan

- **PostgreSQL:** tabla `PRODUCTO_FOTO` (cambio **v11**, se aplica solo al abrir). Va aparte de `PRODUCTO` a propósito: la lista de productos se lee todo el tiempo y no debe arrastrar imágenes. Se borra sola con el producto o con el negocio (`ON DELETE CASCADE`).
- **Archivos JSON:** `perfiles/<negocio>/fotos/<id>.webp|jpg|png`.
- **Oracle:** sin fotos (no se tocó Oracle en esta versión).

La lista de productos no trae fotos. La página pregunta en **una** consulta cuáles productos tienen foto (`obtenerEstadoFotos`) y pide cada imagen (`obtenerFotoProducto`) solo cuando va a mostrarla.

## Piezas

| Pieza | Archivo |
|---|---|
| Interfaz de almacenamiento | `repository/FotoStore.java` (`FotoJdbcStore`, `FotoFileStore`) |
| Reglas | `service/FotoService.java` |
| Puente | `bridge/FotoController.java` (`guardarProductoConFoto`, `obtenerEstadoFotos`, `obtenerFotoProducto`, `quitarFotoProducto`, `usarFotosProducto`) |
| Regla por tipo de negocio | `Profile.usaFotos()`: activas en Boutique y Tecnología, o lo que el dueño haya elegido (`cap.fotos`) |
| Migración | `sql/postgresql/cambios/v11_fotos_producto.sql` |
| Interfaz | `html/js/tbalance-alta.js`, `html/css/tbalance-alta.css` |

`guardarProductoConFoto` guarda el producto y la foto en un solo paso: un producto nuevo recibe su foto con el ID que acaba de tener. Si la foto falla, el producto igual queda guardado (no se duplica al reintentar) y se avisa qué pasó.

Pide el permiso **Editar catálogo** y queda en el historial ("Guardó un producto", "Cambió la foto de un producto").

## Copias de seguridad

Con PostgreSQL las fotos viajan en la base. Con archivos JSON están en la carpeta del negocio: al copiar la carpeta de datos se copian también.

## Pendiente a propósito

Mostrar la foto en las pantallas de venta. Esta versión deja listo el almacenamiento y la lista de productos; la pantalla de venta por tipo de negocio es otra entrega.
