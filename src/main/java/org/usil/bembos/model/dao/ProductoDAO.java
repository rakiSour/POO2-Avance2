package org.usil.bembos.model.dao;

import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoDAO {

    /** Productos activos, con la receta cargada en el caso de las hamburguesas. */
    List<Producto> listarActivos();

    Optional<Producto> buscarPorCodigo(String codigo);

    /** Ingredientes que se ofrecen como adicionales. */
    List<Ingrediente> listarAdicionales();
}
