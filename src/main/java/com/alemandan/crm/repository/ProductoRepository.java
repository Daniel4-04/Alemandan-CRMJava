package com.alemandan.crm.repository;

import com.alemandan.crm.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio para Producto.
 * Contiene consultas básicas y algunas consultas adicionales útiles para los reportes:
 * - stockProductos: devuelve [productoId, nombre, cantidad]
 * - productosConStockBajo: devuelve entidades Producto cuyo stock está por debajo del umbral indicado
 *
 * Ajusta los nombres de las propiedades en las consultas JPQL si tu entidad Producto usa nombres distintos.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrue();

    List<Producto> findByNombreAndActivoFalse(String nombre);

    long count(); // Para contar productos

    // NUEVO: Para búsqueda por nombre/código AJAX
    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    /**
     * Devuelve una lista de objetos con la forma: [productoId, productoNombre, cantidad]
     * Ordenado por cantidad ascendente (productos con menor stock primero).
     */
    @Query("SELECT p.id, p.nombre, p.cantidad FROM Producto p ORDER BY p.cantidad ASC")
    List<Object[]> stockProductos();

    /**
     * Devuelve los productos cuyo stock es menor o igual al umbral pasado.
     */
    @Query("SELECT p FROM Producto p WHERE p.cantidad <= :threshold ORDER BY p.cantidad ASC")
    List<Producto> productosConStockBajo(@Param("threshold") Integer threshold);
}