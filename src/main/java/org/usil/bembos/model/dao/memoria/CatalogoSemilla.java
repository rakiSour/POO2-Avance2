package org.usil.bembos.model.dao.memoria;

import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.TipoProducto;
import org.usil.bembos.model.factory.ProductoFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo equivalente a {@code db/02_datos.sql}, usado por el modo en memoria
 * (pruebas unitarias y demostracion sin servidor PostgreSQL).
 */
public final class CatalogoSemilla {

    private final Map<String, Ingrediente> ingredientes = new LinkedHashMap<>();
    private final List<Ingrediente> adicionales = new ArrayList<>();
    private final List<Producto> productos = new ArrayList<>();

    public CatalogoSemilla() {
        ing(1, "Pan de hamburguesa", "0.00", false, true);
        ing(2, "Carne de res 1/4 lb", "6.00", true, true);
        ing(3, "Pollo crispy", "6.50", true, true);
        ing(4, "Queso cheddar", "2.50", true, true);
        ing(5, "Tocino", "3.50", true, true);
        ing(6, "Huevo frito", "2.50", true, true);
        ing(7, "Plátano frito", "2.50", true, true);
        ing(8, "Jamón", "2.50", true, true);
        ing(9, "Piña", "2.00", true, true);
        ing(10, "Lechuga", "0.00", false, true);
        ing(11, "Tomate", "0.00", false, true);
        ing(12, "Cebolla", "0.00", false, true);
        ing(13, "Pepinillos", "1.00", true, true);
        ing(14, "Salsa de la casa", "0.00", false, true);
        ing(15, "Mayonesa", "0.00", false, true);
        ing(16, "Ketchup", "0.00", false, true);
        ing(17, "Mostaza", "0.00", false, true);
        ing(18, "Champiñones", "3.00", true, false);
        adicionales.sort((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()));

        ham(1, "HAM-CLA", "Hamburguesa Clásica", "12.90", "Pan de hamburguesa*", "Carne de res 1/4 lb*",
                "Lechuga", "Tomate", "Salsa de la casa");
        ham(2, "HAM-CHE", "Cheese Burger", "14.90", "Pan de hamburguesa*", "Carne de res 1/4 lb*",
                "Queso cheddar*", "Pepinillos", "Ketchup", "Mostaza");
        ham(3, "HAM-ROY", "Hamburguesa Royal", "16.90", "Pan de hamburguesa*", "Carne de res 1/4 lb*",
                "Queso cheddar", "Huevo frito*", "Lechuga", "Tomate", "Mayonesa");
        ham(4, "HAM-POB", "Hamburguesa a lo Pobre", "17.90", "Pan de hamburguesa*", "Carne de res 1/4 lb*",
                "Huevo frito*", "Plátano frito*", "Lechuga", "Tomate", "Salsa de la casa");
        ham(5, "HAM-HAW", "Hamburguesa Hawaiana", "16.90", "Pan de hamburguesa*", "Carne de res 1/4 lb*",
                "Queso cheddar", "Jamón*", "Piña*", "Salsa de la casa");
        ham(6, "HAM-CRI", "Crispy de Pollo", "15.90", "Pan de hamburguesa*", "Pollo crispy*",
                "Lechuga", "Tomate", "Mayonesa");
        ham(7, "HAM-PAR", "Hamburguesa Parrillera", "19.90", "Pan de hamburguesa*", "Carne de res 1/4 lb*",
                "Tocino*", "Queso cheddar", "Cebolla", "Salsa de la casa");

        prod(8, TipoProducto.COMPLEMENTO, "COM-ARO", "Aros de cebolla", "7.90");
        prod(9, TipoProducto.COMPLEMENTO, "COM-NUG", "Nuggets x6", "9.90");
        prod(10, TipoProducto.COMPLEMENTO, "COM-PAP", "Papas fritas", "6.90");
        prod(11, TipoProducto.BEBIDA, "BEB-AGU", "Agua mineral", "3.90");
        prod(12, TipoProducto.BEBIDA, "BEB-CHI", "Chicha morada", "6.00");
        prod(13, TipoProducto.BEBIDA, "BEB-GAS", "Gaseosa 500 ml", "5.50");
        prod(14, TipoProducto.BEBIDA, "BEB-LIM", "Limonada frozen", "6.50");
        prod(15, TipoProducto.POSTRE, "POS-HEL", "Helado de vainilla", "4.90");
        prod(16, TipoProducto.POSTRE, "POS-PIE", "Pie de manzana", "5.90");
    }

    private void ing(int id, String nombre, String precio, boolean adicional, boolean disponible) {
        Ingrediente i = new Ingrediente(id, nombre, new BigDecimal(precio), disponible);
        ingredientes.put(nombre, i);
        if (adicional) {
            adicionales.add(i);
        }
    }

    /** Un asterisco al final del nombre marca el ingrediente como obligatorio. */
    private void ham(int id, String codigo, String nombre, String precio, String... receta) {
        Hamburguesa h = (Hamburguesa) ProductoFactory.crear(TipoProducto.HAMBURGUESA, id, codigo, nombre,
                "", new BigDecimal(precio));
        for (String r : receta) {
            boolean obligatorio = r.endsWith("*");
            String clave = obligatorio ? r.substring(0, r.length() - 1) : r;
            h.agregarIngredienteReceta(ingrediente(clave), obligatorio);
        }
        productos.add(h);
    }

    private void prod(int id, TipoProducto tipo, String codigo, String nombre, String precio) {
        productos.add(ProductoFactory.crear(tipo, id, codigo, nombre, "", new BigDecimal(precio)));
    }

    public Ingrediente ingrediente(String nombre) {
        Ingrediente i = ingredientes.get(nombre);
        if (i == null) {
            throw new IllegalArgumentException("Ingrediente no registrado: " + nombre);
        }
        return i;
    }

    public Producto producto(String codigo) {
        return productos.stream().filter(p -> p.getCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Producto no registrado: " + codigo));
    }

    public List<Producto> getProductos() {
        return List.copyOf(productos);
    }

    public List<Ingrediente> getAdicionales() {
        return List.copyOf(adicionales);
    }
}
