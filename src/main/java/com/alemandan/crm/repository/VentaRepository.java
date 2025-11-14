package com.alemandan.crm.repository;

import com.alemandan.crm.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para consultas sobre ventas.
 */
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByUsuarioId(Long usuarioId);

    @Query("SELECT v FROM Venta v " +
            "WHERE (:fechaInicio IS NULL OR v.fecha >= :fechaInicio) " +
            "AND (:fechaFin IS NULL OR v.fecha <= :fechaFin) " +
            "AND (:usuarioId IS NULL OR v.usuario.id = :usuarioId) " +
            "AND (:productoId IS NULL OR EXISTS (SELECT d FROM DetalleVenta d WHERE d.venta = v AND d.producto.id = :productoId)) " +
            "AND (:metodoPago IS NULL OR v.metodoPago = :metodoPago)")
    List<Venta> filtrarAdmin(@Param("fechaInicio") LocalDateTime fechaInicio,
                             @Param("fechaFin") LocalDateTime fechaFin,
                             @Param("usuarioId") Long usuarioId,
                             @Param("productoId") Long productoId,
                             @Param("metodoPago") String metodoPago);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fecha >= :inicio AND v.fecha < :fin")
    long countByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.usuario.id = :usuarioId AND v.fecha >= :inicio AND v.fecha < :fin")
    long countByUsuarioIdAndFechaBetween(@Param("usuarioId") Long usuarioId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.usuario.id = :usuarioId")
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    /* -------------------
       Consultas para reportes e informes
       ------------------- */

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :from AND :to")
    BigDecimal totalVentasBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fecha BETWEEN :from AND :to")
    Long countVentasBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT dv.producto.id, dv.producto.nombre, SUM(dv.cantidad) as totalVendida " +
            "FROM DetalleVenta dv JOIN dv.venta v " +
            "WHERE v.fecha BETWEEN :from AND :to " +
            "GROUP BY dv.producto.id, dv.producto.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<Object[]> topProductosBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT v.usuario.id, v.usuario.nombre, SUM(v.total) as totalVendido " +
            "FROM Venta v WHERE v.fecha BETWEEN :from AND :to " +
            "GROUP BY v.usuario.id, v.usuario.nombre " +
            "ORDER BY SUM(v.total) DESC")
    List<Object[]> topVendedoresBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', v.fecha) as dia, SUM(v.total) " +
            "FROM Venta v WHERE v.fecha BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('DATE', v.fecha) ORDER BY FUNCTION('DATE', v.fecha)")
    List<Object[]> ventasPorDiaBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Top vendedores para un producto específico (cantidad vendida del producto por vendedor).
     * Devuelve lista de Object[] con: [usuarioId, usuarioNombre, cantidadTotalVendidaDelProducto]
     *
     * IMPORTANTE: aquí sumamos dv.cantidad — esto devuelve la cantidad de unidades vendidas del producto
     * por cada vendedor en el rango indicado.
     */
    @Query("SELECT v.usuario.id, v.usuario.nombre, SUM(dv.cantidad) as totalCantidad " +
            "FROM DetalleVenta dv JOIN dv.venta v " +
            "WHERE v.fecha BETWEEN :from AND :to " +
            "AND dv.producto.id = :productoId " +
            "GROUP BY v.usuario.id, v.usuario.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<Object[]> topVendedoresPorProductoBetween(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to,
                                                   @Param("productoId") Long productoId);
}