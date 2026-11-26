-- =====================================================================
-- TBalance - PostgreSQL
-- Cambio v10 (v3.8.0): alta ágil de productos.
--
-- El margen y el redondeo del negocio ("alta.margen" y "alta.redondeo") van en
-- PERFIL_CONFIGURACION, igual que las capacidades (v8) y los datos del
-- comprobante (v9): no necesitan columnas.
--
-- Lo único que pide la base son dos índices, porque ahora el alta consulta
-- con frecuencia por código de barras y por nombre:
--  1. Escanear una caja busca el producto por código antes de crear otro.
--  2. Pegar una lista desde Excel avisa de los nombres que ya existen
--     (sin importar mayúsculas), para no duplicar productos.
--
-- No se usan índices UNIQUE: los negocios que ya tienen nombres o códigos
-- repetidos no deben quedar bloqueados al actualizar.
-- =====================================================================

CREATE INDEX IF NOT EXISTS IX_PRODUCTO_CODIGO ON PRODUCTO (PERFIL_ID, CODIGO) WHERE CODIGO IS NOT NULL;
CREATE INDEX IF NOT EXISTS IX_PRODUCTO_NOMBRE ON PRODUCTO (PERFIL_ID, LOWER(NOMBRE));
