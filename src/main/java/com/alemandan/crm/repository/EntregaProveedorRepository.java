package com.alemandan.crm.repository;

import com.alemandan.crm.model.EntregaProveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregaProveedorRepository extends JpaRepository<EntregaProveedor, Long> {
    List<EntregaProveedor> findByProveedorId(Long proveedorId);
}