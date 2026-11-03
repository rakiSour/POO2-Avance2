-- =====================================================================
--  Bembos - Script 02: catálogo inicial (datos referenciales de prueba)
--  Ejecutar después de 01_schema.sql, conectado a bembos_db.
--  Los precios son referenciales para la simulación académica.
-- =====================================================================
SET search_path TO bembos;

INSERT INTO ingrediente (nombre, precio_extra, es_adicional, disponible) VALUES
    ('Pan de hamburguesa',   0.00, FALSE, TRUE),
    ('Carne de res 1/4 lb',  6.00, TRUE,  TRUE),
    ('Pollo crispy',         6.50, TRUE,  TRUE),
    ('Queso cheddar',        2.50, TRUE,  TRUE),
    ('Tocino',               3.50, TRUE,  TRUE),
    ('Huevo frito',          2.50, TRUE,  TRUE),
    ('Plátano frito',        2.50, TRUE,  TRUE),
    ('Jamón',                2.50, TRUE,  TRUE),
    ('Piña',                 2.00, TRUE,  TRUE),
    ('Lechuga',              0.00, FALSE, TRUE),
    ('Tomate',               0.00, FALSE, TRUE),
    ('Cebolla',              0.00, FALSE, TRUE),
    ('Pepinillos',           1.00, TRUE,  TRUE),
    ('Salsa de la casa',     0.00, FALSE, TRUE),
    ('Mayonesa',             0.00, FALSE, TRUE),
    ('Ketchup',              0.00, FALSE, TRUE),
    ('Mostaza',              0.00, FALSE, TRUE),
    ('Champiñones',          3.00, TRUE,  FALSE);   -- sin stock: prueba la regla de disponibilidad

INSERT INTO producto (codigo, nombre, descripcion, tipo, precio_base) VALUES
    ('HAM-CLA', 'Hamburguesa Clásica',      'Carne de res a la parrilla, lechuga, tomate y salsa de la casa', 'HAMBURGUESA', 12.90),
    ('HAM-CHE', 'Cheese Burger',            'Carne de res, queso cheddar, pepinillos, ketchup y mostaza',     'HAMBURGUESA', 14.90),
    ('HAM-ROY', 'Hamburguesa Royal',        'Carne de res, queso, huevo frito, lechuga, tomate y mayonesa',   'HAMBURGUESA', 16.90),
    ('HAM-POB', 'Hamburguesa a lo Pobre',   'Carne de res, huevo, plátano frito, lechuga y tomate',            'HAMBURGUESA', 17.90),
    ('HAM-HAW', 'Hamburguesa Hawaiana',     'Carne de res, queso, jamón y piña',                               'HAMBURGUESA', 16.90),
    ('HAM-CRI', 'Crispy de Pollo',          'Pollo crispy, lechuga, tomate y mayonesa',                        'HAMBURGUESA', 15.90),
    ('HAM-PAR', 'Hamburguesa Parrillera',   'Carne de res, tocino, queso, cebolla y salsa de la casa',         'HAMBURGUESA', 19.90),
    ('COM-PAP', 'Papas fritas',             'Porción de papas fritas',                                         'COMPLEMENTO',  6.90),
    ('COM-NUG', 'Nuggets x6',               'Seis piezas de pollo empanizado',                                 'COMPLEMENTO',  9.90),
    ('COM-ARO', 'Aros de cebolla',          'Porción de aros de cebolla',                                      'COMPLEMENTO',  7.90),
    ('BEB-GAS', 'Gaseosa 500 ml',           'Gaseosa personal',                                                'BEBIDA',       5.50),
    ('BEB-LIM', 'Limonada frozen',          'Limonada frozen',                                                 'BEBIDA',       6.50),
    ('BEB-CHI', 'Chicha morada',            'Chicha morada natural',                                           'BEBIDA',       6.00),
    ('BEB-AGU', 'Agua mineral',             'Agua sin gas 625 ml',                                             'BEBIDA',       3.90),
    ('POS-HEL', 'Helado de vainilla',       'Cono de helado de vainilla',                                      'POSTRE',       4.90),
    ('POS-PIE', 'Pie de manzana',           'Porción de pie de manzana',                                       'POSTRE',       5.90);

-- Recetas: (código de producto, ingrediente, obligatorio, orden)
INSERT INTO receta (id_producto, id_ingrediente, obligatorio, orden)
SELECT p.id_producto, i.id_ingrediente, r.obligatorio, r.orden
FROM (VALUES
    ('HAM-CLA', 'Pan de hamburguesa', TRUE, 1), ('HAM-CLA', 'Carne de res 1/4 lb', TRUE, 2),
    ('HAM-CLA', 'Lechuga', FALSE, 3), ('HAM-CLA', 'Tomate', FALSE, 4), ('HAM-CLA', 'Salsa de la casa', FALSE, 5),

    ('HAM-CHE', 'Pan de hamburguesa', TRUE, 1), ('HAM-CHE', 'Carne de res 1/4 lb', TRUE, 2),
    ('HAM-CHE', 'Queso cheddar', TRUE, 3), ('HAM-CHE', 'Pepinillos', FALSE, 4),
    ('HAM-CHE', 'Ketchup', FALSE, 5), ('HAM-CHE', 'Mostaza', FALSE, 6),

    ('HAM-ROY', 'Pan de hamburguesa', TRUE, 1), ('HAM-ROY', 'Carne de res 1/4 lb', TRUE, 2),
    ('HAM-ROY', 'Queso cheddar', FALSE, 3), ('HAM-ROY', 'Huevo frito', TRUE, 4),
    ('HAM-ROY', 'Lechuga', FALSE, 5), ('HAM-ROY', 'Tomate', FALSE, 6), ('HAM-ROY', 'Mayonesa', FALSE, 7),

    ('HAM-POB', 'Pan de hamburguesa', TRUE, 1), ('HAM-POB', 'Carne de res 1/4 lb', TRUE, 2),
    ('HAM-POB', 'Huevo frito', TRUE, 3), ('HAM-POB', 'Plátano frito', TRUE, 4),
    ('HAM-POB', 'Lechuga', FALSE, 5), ('HAM-POB', 'Tomate', FALSE, 6), ('HAM-POB', 'Salsa de la casa', FALSE, 7),

    ('HAM-HAW', 'Pan de hamburguesa', TRUE, 1), ('HAM-HAW', 'Carne de res 1/4 lb', TRUE, 2),
    ('HAM-HAW', 'Queso cheddar', FALSE, 3), ('HAM-HAW', 'Jamón', TRUE, 4),
    ('HAM-HAW', 'Piña', TRUE, 5), ('HAM-HAW', 'Salsa de la casa', FALSE, 6),

    ('HAM-CRI', 'Pan de hamburguesa', TRUE, 1), ('HAM-CRI', 'Pollo crispy', TRUE, 2),
    ('HAM-CRI', 'Lechuga', FALSE, 3), ('HAM-CRI', 'Tomate', FALSE, 4), ('HAM-CRI', 'Mayonesa', FALSE, 5),

    ('HAM-PAR', 'Pan de hamburguesa', TRUE, 1), ('HAM-PAR', 'Carne de res 1/4 lb', TRUE, 2),
    ('HAM-PAR', 'Tocino', TRUE, 3), ('HAM-PAR', 'Queso cheddar', FALSE, 4),
    ('HAM-PAR', 'Cebolla', FALSE, 5), ('HAM-PAR', 'Salsa de la casa', FALSE, 6)
) AS r(codigo, ingrediente, obligatorio, orden)
JOIN producto p    ON p.codigo = r.codigo
JOIN ingrediente i ON i.nombre = r.ingrediente;

-- Verificación rápida
SELECT p.nombre AS hamburguesa, count(*) AS ingredientes
FROM receta r JOIN producto p USING (id_producto)
GROUP BY p.nombre ORDER BY p.nombre;
