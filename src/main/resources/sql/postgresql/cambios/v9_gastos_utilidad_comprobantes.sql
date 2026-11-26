-- =====================================================================
-- TBalance - PostgreSQL
-- Cambio v9 (v3.7.0): utilidad neta real, costo oculto por producto y comprobantes.
--
--  1. Tabla GASTO: lo que cuesta mantener el negocio abierto (luz, agua, gas,
--     internet, arriendo, nómina y otros), por mes.
--  2. PRODUCTO.COSTO_OCULTO: empaque, bolsa, flete o comisión por unidad.
--  3. VENTA.NUMERO y tabla CONSECUTIVO: número de comprobante propio de cada
--     negocio, sin repetirse ni saltarse aunque haya varias cajas.
--  4. Vistas para consultar la utilidad neta desde pgAdmin.
--
-- NIT, dirección, teléfono, pie del comprobante y el "% de gastos" no
-- necesitan columnas: van en PERFIL_CONFIGURACION (claves negocio.* y
-- gastos.*), igual que las capacidades del cambio v8.
-- =====================================================================

-- 1. Gastos operativos
CREATE TABLE GASTO (
  PERFIL_ID     VARCHAR(20)   NOT NULL,
  ID            VARCHAR(30)   NOT NULL,
  PERIODO       CHAR(7)       NOT NULL,              -- mes al que corresponde: 2026-11
  FECHA_PAGO    DATE          NOT NULL,              -- cuándo se pagó (puede ser de otro mes)
  CATEGORIA     VARCHAR(12)   NOT NULL,
  DESCRIPCION   VARCHAR(150),
  MONTO         NUMERIC(14,2) NOT NULL,
  RECURRENTE    SMALLINT      DEFAULT 0 NOT NULL,    -- 1: se repite cada mes (plantilla)
  ORIGEN_ID     VARCHAR(30),                         -- gasto fijo del que salió esta copia mensual
  ACTIVO        SMALLINT      DEFAULT 1 NOT NULL,    -- borrado lógico, como en el cambio v2
  REGISTRADO_POR VARCHAR(60),
  FECHA_REGISTRO TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT PK_GASTO PRIMARY KEY (PERFIL_ID, ID),
  CONSTRAINT FK_GASTO_PERFIL FOREIGN KEY (PERFIL_ID) REFERENCES PERFIL (ID) ON DELETE CASCADE,
  CONSTRAINT FK_GASTO_ORIGEN FOREIGN KEY (PERFIL_ID, ORIGEN_ID) REFERENCES GASTO (PERFIL_ID, ID),
  CONSTRAINT CK_GASTO_CATEGORIA CHECK (CATEGORIA IN ('AGUA', 'LUZ', 'GAS', 'INTERNET', 'ARRIENDO', 'NOMINA', 'OTRO')),
  CONSTRAINT CK_GASTO_MONTO CHECK (MONTO > 0),
  CONSTRAINT CK_GASTO_PERIODO CHECK (PERIODO ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
  CONSTRAINT CK_GASTO_RECURRENTE CHECK (RECURRENTE IN (0, 1)),
  CONSTRAINT CK_GASTO_ACTIVO CHECK (ACTIVO IN (0, 1)),
  -- un gasto fijo no se copia dos veces al mismo mes (varias cajas abriendo el mes a la vez)
  CONSTRAINT UQ_GASTO_COPIA UNIQUE (PERFIL_ID, ORIGEN_ID, PERIODO)
);

CREATE INDEX IX_GASTO_PERIODO ON GASTO (PERFIL_ID, PERIODO);

-- 2. Costo oculto por unidad
ALTER TABLE PRODUCTO ADD COLUMN COSTO_OCULTO NUMERIC(14,2) DEFAULT 0 NOT NULL;
ALTER TABLE PRODUCTO ADD CONSTRAINT CK_PRODUCTO_COSTO_OCULTO CHECK (COSTO_OCULTO >= 0);

-- La venta recuerda el costo oculto que tenía el producto al venderse,
-- así la utilidad de una venta vieja no cambia si después se edita el producto.
ALTER TABLE DETALLE_VENTA ADD COLUMN COSTO_OCULTO NUMERIC(14,2) DEFAULT 0 NOT NULL;

-- 3. Número de comprobante
CREATE TABLE CONSECUTIVO (
  PERFIL_ID  VARCHAR(20) NOT NULL,
  TIPO       VARCHAR(20) NOT NULL,                   -- por ahora solo 'COMPROBANTE'
  ULTIMO     BIGINT      DEFAULT 0 NOT NULL,
  CONSTRAINT PK_CONSECUTIVO PRIMARY KEY (PERFIL_ID, TIPO),
  CONSTRAINT FK_CONSECUTIVO_PERFIL FOREIGN KEY (PERFIL_ID) REFERENCES PERFIL (ID) ON DELETE CASCADE,
  CONSTRAINT CK_CONSECUTIVO_ULTIMO CHECK (ULTIMO >= 0)
);

ALTER TABLE VENTA ADD COLUMN NUMERO BIGINT;

-- Las ventas que ya existen se numeran por fecha, de una en una, en cada negocio
UPDATE VENTA V
   SET NUMERO = N.POS
  FROM (SELECT PERFIL_ID, ID, ROW_NUMBER() OVER (PARTITION BY PERFIL_ID ORDER BY FECHA, ID) AS POS
          FROM VENTA) N
 WHERE N.PERFIL_ID = V.PERFIL_ID AND N.ID = V.ID;

INSERT INTO CONSECUTIVO (PERFIL_ID, TIPO, ULTIMO)
SELECT PERFIL_ID, 'COMPROBANTE', COUNT(*) FROM VENTA GROUP BY PERFIL_ID;

ALTER TABLE VENTA ALTER COLUMN NUMERO SET NOT NULL;
ALTER TABLE VENTA ADD CONSTRAINT UQ_VENTA_NUMERO UNIQUE (PERFIL_ID, NUMERO);

-- 4. Utilidad neta por mes (la pantalla Hoy e Informes calculan lo mismo en Java;
--    esta vista sirve para revisar las cifras desde pgAdmin)
CREATE OR REPLACE VIEW V_UTILIDAD_NETA_MES AS
WITH VENTAS AS (
  SELECT V.PERFIL_ID, TO_CHAR(V.FECHA, 'YYYY-MM') AS PERIODO,
         SUM(D.SUBTOTAL)                                       AS INGRESOS,
         SUM(D.CANTIDAD * D.COSTO_UNITARIO)                    AS COSTO_MERCANCIA,
         SUM(D.CANTIDAD * D.COSTO_OCULTO)                      AS COSTOS_OCULTOS
    FROM VENTA V JOIN DETALLE_VENTA D ON D.PERFIL_ID = V.PERFIL_ID AND D.VENTA_ID = V.ID
   WHERE COALESCE(V.ESTADO, 'COMPLETADA') <> 'ANULADA'
   GROUP BY V.PERFIL_ID, TO_CHAR(V.FECHA, 'YYYY-MM')
), GASTOS AS (
  SELECT PERFIL_ID, PERIODO, SUM(MONTO) AS GASTOS_OPERATIVOS
    FROM GASTO WHERE ACTIVO = 1 GROUP BY PERFIL_ID, PERIODO
)
SELECT COALESCE(V.PERFIL_ID, G.PERFIL_ID) AS PERFIL_ID,
       COALESCE(V.PERIODO, G.PERIODO)     AS PERIODO,
       COALESCE(V.INGRESOS, 0)            AS INGRESOS,
       COALESCE(V.COSTO_MERCANCIA, 0)     AS COSTO_MERCANCIA,
       COALESCE(V.COSTOS_OCULTOS, 0)      AS COSTOS_OCULTOS,
       COALESCE(V.INGRESOS, 0) - COALESCE(V.COSTO_MERCANCIA, 0) - COALESCE(V.COSTOS_OCULTOS, 0) AS UTILIDAD_BRUTA,
       COALESCE(G.GASTOS_OPERATIVOS, 0)   AS GASTOS_OPERATIVOS,
       COALESCE(V.INGRESOS, 0) - COALESCE(V.COSTO_MERCANCIA, 0) - COALESCE(V.COSTOS_OCULTOS, 0)
         - COALESCE(G.GASTOS_OPERATIVOS, 0) AS UTILIDAD_NETA
  FROM VENTAS V FULL OUTER JOIN GASTOS G ON G.PERFIL_ID = V.PERFIL_ID AND G.PERIODO = V.PERIODO;
