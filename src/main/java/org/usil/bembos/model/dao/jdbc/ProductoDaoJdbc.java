package org.usil.bembos.model.dao.jdbc;

import org.usil.bembos.config.ConexionBD;
import org.usil.bembos.model.dao.AccesoDatosException;
import org.usil.bembos.model.dao.ProductoDAO;
import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.factory.ProductoFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProductoDaoJdbc implements ProductoDAO {

    private static final String SQL_PRODUCTOS = """
            SELECT id_producto, codigo, nombre, descripcion, tipo, precio_base
            FROM producto
            WHERE activo
            ORDER BY CASE tipo WHEN 'HAMBURGUESA' THEN 1 WHEN 'COMPLEMENTO' THEN 2
                               WHEN 'BEBIDA' THEN 3 ELSE 4 END, nombre""";

    private static final String SQL_RECETAS = """
            SELECT r.id_producto, i.id_ingrediente, i.nombre, i.precio_extra, i.disponible, r.obligatorio
            FROM receta r
            JOIN ingrediente i ON i.id_ingrediente = r.id_ingrediente
            ORDER BY r.id_producto, r.orden""";

    private static final String SQL_ADICIONALES = """
            SELECT id_ingrediente, nombre, precio_extra, disponible
            FROM ingrediente
            WHERE es_adicional
            ORDER BY nombre""";

    private final ConexionBD conexionBD;

    public ProductoDaoJdbc(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    @Override
    public List<Producto> listarActivos() {
        try (Connection cn = conexionBD.obtenerConexion()) {
            Map<Integer, Producto> productos = new LinkedHashMap<>();
            try (PreparedStatement ps = cn.prepareStatement(SQL_PRODUCTOS);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = ProductoFactory.crear(rs.getString("tipo"), rs.getInt("id_producto"),
                            rs.getString("codigo"), rs.getString("nombre"),
                            rs.getString("descripcion"), rs.getBigDecimal("precio_base"));
                    productos.put(p.getId(), p);
                }
            }
            try (PreparedStatement ps = cn.prepareStatement(SQL_RECETAS);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (productos.get(rs.getInt("id_producto")) instanceof Hamburguesa h) {
                        h.agregarIngredienteReceta(mapearIngrediente(rs), rs.getBoolean("obligatorio"));
                    }
                }
            }
            return new ArrayList<>(productos.values());
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo cargar el catálogo de productos", e);
        }
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        return listarActivos().stream().filter(p -> p.getCodigo().equalsIgnoreCase(codigo)).findFirst();
    }

    @Override
    public List<Ingrediente> listarAdicionales() {
        List<Ingrediente> lista = new ArrayList<>();
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_ADICIONALES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearIngrediente(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo cargar la lista de adicionales", e);
        }
    }

    private static Ingrediente mapearIngrediente(ResultSet rs) throws SQLException {
        return new Ingrediente(rs.getInt("id_ingrediente"), rs.getString("nombre"),
                rs.getBigDecimal("precio_extra"), rs.getBoolean("disponible"));
    }
}
