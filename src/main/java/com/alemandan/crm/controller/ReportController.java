package com.alemandan.crm.controller;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.repository.ProductoRepository;
import com.alemandan.crm.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Controlador para el informe avanzado.
 *
 * NOTA: Los endpoints de exportación administrativa (/admin/ventas/...) se han dejado
 * en AdminVentaController para evitar colisiones de rutas. Aquí solo quedan:
 * - GET  /ventas/reporte        -> formulario del reporte avanzado
 * - GET  /ventas/reporte/pdf    -> descarga del informe avanzado (PDF)
 * - GET  /ventas/reporte/excel  -> descarga del informe avanzado (Excel)
 */
@Controller
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping("/ventas/reporte")
    public String formReporte(Model model) {
        // Si en el futuro necesitas pasar empleados/productos al formulario, puedes inyectarlos aquí.
        return "ventas/reporte_form";
    }

    @GetMapping("/ventas/reporte/pdf")
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "productoId", required = false) Long productoId) throws Exception {

        LocalDate today = LocalDate.now();
        if (to == null) to = today;
        if (from == null) from = today.minusDays(30);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        logger.info("Generando reporte avanzado: from={} to={} productoId={}", start, end, productoId);

        byte[] pdf = reportService.generarReporteVentasPdf(start, end, productoId);

        String fileSuffix = "";
        if (productoId != null) {
            Optional<Producto> opt = productoRepository.findById(productoId);
            String prodName = opt.map(Producto::getNombre).orElse("prod" + productoId);
            fileSuffix = "_" + sanitizeFilename(prodName);
        }
        String filename = "reporte_ventas_" + from + "_" + to + fileSuffix + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/ventas/reporte/excel")
    public void descargarReporteExcel(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "productoId", required = false) Long productoId,
            HttpServletResponse response) {
        try {
            logger.info("Exportación a Excel deshabilitada - petición de reporte avanzado recibida: from={} to={} productoId={}", from, to, productoId);
            
            response.sendError(HttpServletResponse.SC_GONE, 
                "La exportación a Excel ha sido deshabilitada. Por favor, utilice la exportación a PDF.");
        } catch (Exception e) {
            logger.error("Error al enviar respuesta 410: {}", e.getMessage(), e);
        }
    }

    private String sanitizeFilename(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
    }
}