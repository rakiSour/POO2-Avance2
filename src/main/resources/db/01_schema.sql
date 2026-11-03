-- =====================================================================
--  Bembos - Sistema de Gestión de Pedidos (MVC + Builder)
--  Script 01: creación del esquema "bembos" en PostgreSQL 14+
--
--  Uso en pgAdmin:
--    1) Crear la base de datos:   CREATE DATABASE bembos_db ENCODING 'UTF8';
--    2) Conectarse a bembos_db, abrir Query Tool y ejecutar este script.
--    3) Ejecutar luego 02_datos.sql (catálogo inicial).
--  El script es re-ejecutable: elimina y vuelve a crear el esquema.
-- =====================================================================

DROP SCHEMA IF EXISTS bembos CASCADE;
CREATE SCHEMA bembos;
SET search_path TO bembos;

-- ---------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------
CREATE TABLE producto (
    id_producto   SERIAL        PRIMARY KEY,
    codigo        VARCHAR(10)   NOT NULL UNIQUE,
    nombre        VARCHAR(80)   NOT NULL,
    descripcion   VARCHAR(200)  NOT NULL DEFAULT '',
    tipo          VARCHAR(15)   NOT NULL
                  CHECK (tipo IN ('HAMBURGUESA', 'COMPLEMENTO', 'BEBIDA', 'POSTRE')),
    precio_base   NUMERIC(10,2) NOT NULL CHECK (precio_base >= 0),
    activo        BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE ingrediente (
    id_ingrediente SERIAL        PRIMARY KEY,
    nombre         VARCHAR(60)   NOT NULL UNIQUE,
    precio_extra   NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (precio_extra >= 0),
    es_adicional   BOOLEAN       NOT NULL DEFAULT FALSE,  -- se ofrece como "extra"
    disponible     BOOLEAN       NOT NULL DEFAULT TRUE    -- false = sin stock en tienda
);

-- Receta base de cada hamburguesa
CREATE TABLE receta (
    id_producto    INT     NOT NULL REFERENCES producto (id_producto) ON DELETE CASCADE,
    id_ingrediente INT     NOT NULL REFERENCES ingrediente (id_ingrediente),
    obligatorio    BOOLEAN NOT NULL DEFAULT FALSE,        -- no se puede retirar
    orden          SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id_producto, id_ingrediente)
);

-- ---------------------------------------------------------------------
-- Clientes y pedidos
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id_cliente  SERIAL       PRIMARY KEY,
    documento   VARCHAR(11)  NOT NULL UNIQUE CHECK (documento ~ '^[0-9]{8}$|^[0-9]{11}$'),
    nombre      VARCHAR(120) NOT NULL
);

CREATE TABLE pedido (
    id_pedido           SERIAL        PRIMARY KEY,
    codigo              VARCHAR(12)   GENERATED ALWAYS AS ('PED-' || lpad(id_pedido::text, 6, '0')) STORED,
    id_cliente          INT           REFERENCES cliente (id_cliente),
    nombre_cliente      VARCHAR(120)  NOT NULL DEFAULT 'Clientes varios',
    canal               VARCHAR(10)   NOT NULL CHECK (canal IN ('MOSTRADOR', 'KIOSCO', 'DELIVERY')),
    estado              VARCHAR(15)   NOT NULL DEFAULT 'REGISTRADO'
                        CHECK (estado IN ('REGISTRADO', 'PAGADO', 'EN_PREPARACION', 'LISTO', 'ENTREGADO', 'ANULADO')),
    observacion         VARCHAR(200)  NOT NULL DEFAULT '',
    base_imponible      NUMERIC(10,2) NOT NULL,
    igv                 NUMERIC(10,2) NOT NULL,
    total               NUMERIC(10,2) NOT NULL CHECK (total >= 0),
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uq_pedido_codigo UNIQUE (codigo)
);

CREATE TABLE detalle_pedido (
    id_detalle      SERIAL        PRIMARY KEY,
    id_pedido       INT           NOT NULL REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    id_producto     INT           NOT NULL REFERENCES producto (id_producto),
    cantidad        SMALLINT      NOT NULL CHECK (cantidad BETWEEN 1 AND 20),
    tamano          VARCHAR(10)   NOT NULL DEFAULT 'REGULAR' CHECK (tamano IN ('REGULAR', 'AGRANDADO')),
    precio_unitario NUMERIC(10,2) NOT NULL,
    descuento       NUMERIC(10,2) NOT NULL DEFAULT 0,
    subtotal        NUMERIC(10,2) NOT NULL,
    combo           VARCHAR(40),
    nota            VARCHAR(120)  NOT NULL DEFAULT '',
    descripcion     VARCHAR(400)  NOT NULL            -- texto de la comanda para cocina
);

-- Personalización a nivel de sub-componente (resultado del patrón Builder)
CREATE TABLE detalle_personalizacion (
    id_personalizacion SERIAL        PRIMARY KEY,
    id_detalle         INT           NOT NULL REFERENCES detalle_pedido (id_detalle) ON DELETE CASCADE,
    id_ingrediente     INT           NOT NULL REFERENCES ingrediente (id_ingrediente),
    accion             VARCHAR(7)    NOT NULL CHECK (accion IN ('QUITAR', 'AGREGAR')),
    precio             NUMERIC(10,2) NOT NULL DEFAULT 0
);

-- Trazabilidad de estados: permite medir tiempos de atención
CREATE TABLE historial_estado (
    id_historial SERIAL      PRIMARY KEY,
    id_pedido    INT         NOT NULL REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    estado       VARCHAR(15) NOT NULL,
    fecha        TIMESTAMP   NOT NULL DEFAULT clock_timestamp()
);

-- ---------------------------------------------------------------------
-- Pagos y comprobantes
-- ---------------------------------------------------------------------
CREATE TABLE pago (
    id_pago        SERIAL        PRIMARY KEY,
    id_pedido      INT           NOT NULL UNIQUE REFERENCES pedido (id_pedido),
    metodo         VARCHAR(20)   NOT NULL CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'BILLETERA_DIGITAL')),
    monto          NUMERIC(10,2) NOT NULL,
    monto_recibido NUMERIC(10,2) NOT NULL,
    vuelto         NUMERIC(10,2) NOT NULL DEFAULT 0,
    referencia     VARCHAR(40)   NOT NULL,
    fecha          TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE comprobante (
    id_comprobante    SERIAL        PRIMARY KEY,
    id_pedido         INT           NOT NULL UNIQUE REFERENCES pedido (id_pedido),
    tipo              VARCHAR(7)    NOT NULL CHECK (tipo IN ('BOLETA', 'FACTURA')),
    serie             CHAR(4)       NOT NULL,
    numero            INT           NOT NULL,
    documento_cliente VARCHAR(11)   NOT NULL DEFAULT '',
    razon_social      VARCHAR(120)  NOT NULL DEFAULT '',
    total             NUMERIC(10,2) NOT NULL,
    fecha_emision     TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uq_comprobante_serie_numero UNIQUE (serie, numero)
);

-- ---------------------------------------------------------------------
-- Índices
-- ---------------------------------------------------------------------
CREATE INDEX ix_pedido_estado        ON pedido (estado);
CREATE INDEX ix_pedido_fecha         ON pedido (fecha_registro);
CREATE INDEX ix_detalle_pedido       ON detalle_pedido (id_pedido);
CREATE INDEX ix_personalizacion_det  ON detalle_personalizacion (id_detalle);
CREATE INDEX ix_historial_pedido     ON historial_estado (id_pedido, estado);

-- ---------------------------------------------------------------------
-- Vistas de apoyo para reportes (consultables desde pgAdmin)
-- ---------------------------------------------------------------------
CREATE VIEW v_comandas_cocina AS
SELECT p.id_pedido, p.codigo, p.estado, p.canal, p.nombre_cliente, p.fecha_registro, p.total,
       string_agg(d.descripcion, E'\n' ORDER BY d.id_detalle) AS detalle
FROM pedido p
JOIN detalle_pedido d ON d.id_pedido = p.id_pedido
GROUP BY p.id_pedido;

CREATE VIEW v_tiempo_atencion AS
SELECT p.id_pedido, p.codigo, p.fecha_registro::date AS fecha,
       EXTRACT(EPOCH FROM (l.fecha - r.fecha)) / 60.0 AS minutos_hasta_listo
FROM pedido p
JOIN historial_estado r ON r.id_pedido = p.id_pedido AND r.estado = 'REGISTRADO'
JOIN historial_estado l ON l.id_pedido = p.id_pedido AND l.estado = 'LISTO';

CREATE VIEW v_ventas_diarias AS
SELECT fecha_registro::date AS fecha,
       count(*)                                   AS pedidos,
       sum(total) FILTER (WHERE estado <> 'ANULADO') AS total_vendido,
       round(avg(total) FILTER (WHERE estado <> 'ANULADO'), 2) AS ticket_promedio
FROM pedido
GROUP BY fecha_registro::date
ORDER BY fecha DESC;
