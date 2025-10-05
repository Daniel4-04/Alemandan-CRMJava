package com.alemandan.crm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class VentasWebService {

    @GetMapping("/api/ventas/resumen")
    public Map<String, Object> getResumenVentas() {
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalVentas", 158);
        resumen.put("montoTotal", 5420000);
        resumen.put("fecha", new Date());
        return resumen;
    }
}