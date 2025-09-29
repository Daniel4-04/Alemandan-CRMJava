package com.alemandan.crm.service;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminVentaService {
    @Autowired
    private VentaRepository ventaRepository;

    public List<Venta> filtrarVentas(String fechaInicio, String fechaFin, Long usuarioId, Long productoId, String metodoPago) {
        LocalDateTime fechaInicioDT = null, fechaFinDT = null;
        if (fechaInicio != null && !fechaInicio.isEmpty())
            fechaInicioDT = LocalDateTime.parse(fechaInicio + "T00:00:00");
        if (fechaFin != null && !fechaFin.isEmpty())
            fechaFinDT = LocalDateTime.parse(fechaFin + "T23:59:59");

        return ventaRepository.filtrarAdmin(fechaInicioDT, fechaFinDT, usuarioId, productoId, metodoPago);
    }
}