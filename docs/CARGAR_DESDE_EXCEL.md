# Cargar productos desde un archivo de Excel (v3.9.1)

En **Agregar producto → Varios desde Excel** hay un botón **Elegir archivo de Excel…**. Al elegir un `.xlsx` (o un `.csv`), TBalance lee las filas, las revisa solo y deja listo el botón **Cargar N productos**. Antes de cargar se ve qué se va a guardar y qué tiene error; no se guarda nada hasta pulsar Cargar.

## Cómo debe ser el archivo
Una fila por producto, con estas columnas (el encabezado es opcional y el orden puede cambiar si hay encabezado):

| Columna | Obligatoria | Nota |
|---|---|---|
| Nombre | Sí | |
| Costo | Sí | |
| Precio | No | Vacío: TBalance lo calcula con el margen del negocio |
| Cantidad | No | |
| Código | No | |
| Categoría | No | Si no existe, queda en "Sin categoría" |

- Máximo 500 productos por carga y 5 MB por archivo. Los nombres que ya existen se omiten (sin importar mayúsculas).
- Si el libro tiene varias hojas usa la que se llama "Importar…" o "Productos"; si no hay, la primera.
- Los `.xls` antiguos no se leen: guardar como `.xlsx`.

## Por dentro (para la sustentación)
- `ListaArchivoReader` (servicio, sin librerías externas): un `.xlsx` es un zip con XML; lee las cadenas compartidas y la hoja, y entrega texto con tabuladores. Los números salen con coma decimal (89.5 → 89,5) para que el importador no los confunda con miles. El XML se lee sin DTD (protegido contra entidades externas) y con tope de tamaño.
- `JavaBridge.leerArchivoLista` (permiso Editar catálogo): recibe el archivo en Base64 y devuelve el texto o un error en palabras sencillas.
- El texto pasa por `revisarListaProductos` / `importarListaProductos`: **una sola puerta** con las mismas reglas que pegar la lista (GRASP Protected Variations).
- Prueba rápida: `pruebas/PruebaListaArchivoReader.java` (`javac` + `java` con la ruta de un `.xlsx`).
