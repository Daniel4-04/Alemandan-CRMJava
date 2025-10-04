package com.alemandan.crm.repository;

import com.alemandan.crm.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}