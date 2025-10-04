package com.alemandan.crm.repository;

import com.alemandan.crm.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();
    List<Producto> findByNombreAndActivoFalse(String nombre);

    long count(); // Para contar productos
}