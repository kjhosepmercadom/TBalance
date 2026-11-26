-- =====================================================================
-- TBalance - PostgreSQL
-- Cambio v11 (v3.9.0): foto opcional de los productos (boutique y tecnología).
--
-- La foto va en su propia tabla y NO en PRODUCTO, por dos razones:
--  1. Las listas de productos se leen todo el tiempo; si cada fila arrastrara
--     la imagen, abrir el inventario bajaría megas por nada.
--  2. Así se puede pedir la foto de un solo producto cuando se necesita
--     (carga perezosa) y medir cuánto espacio gastan las fotos de un negocio.
--
-- La página reduce la foto a unos 400 px antes de enviarla (unos 30 KB) y el
-- servicio rechaza todo lo que pase de 60 KB, así que 1 GB de base alcanza
-- de sobra. La tabla se borra sola con el producto o con el negocio.
-- =====================================================================

CREATE TABLE PRODUCTO_FOTO (
  PERFIL_ID    VARCHAR(20)  NOT NULL,
  PRODUCTO_ID  VARCHAR(30)  NOT NULL,
  TIPO         VARCHAR(20)  NOT NULL,
  DATOS        BYTEA        NOT NULL,
  BYTES        INTEGER      NOT NULL,
  FECHA        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT PK_PRODUCTO_FOTO PRIMARY KEY (PERFIL_ID, PRODUCTO_ID),
  CONSTRAINT FK_PRODUCTO_FOTO_PRODUCTO FOREIGN KEY (PERFIL_ID, PRODUCTO_ID)
    REFERENCES PRODUCTO (PERFIL_ID, ID) ON DELETE CASCADE,
  CONSTRAINT CK_PRODUCTO_FOTO_TIPO CHECK (TIPO IN ('image/webp', 'image/jpeg', 'image/png')),
  CONSTRAINT CK_PRODUCTO_FOTO_BYTES CHECK (BYTES > 0 AND BYTES <= 204800)
);
