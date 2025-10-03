package com.alemandan.crm.repository;

import com.alemandan.crm.model.SolicitudRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitudRegistroRepository extends JpaRepository<SolicitudRegistro, Long> {
    List<SolicitudRegistro> findByAprobadaFalseAndRechazadaFalse();
    boolean existsByEmail(String email);
}