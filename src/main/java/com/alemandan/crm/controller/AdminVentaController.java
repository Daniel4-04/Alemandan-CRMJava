package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Venta;
import com.alemandan.crm.service.AdminVentaService;
import com.alemandan.crm.service.UsuarioService;
import com.alemandan.crm.service.ProductoService;
import com.alemandan.crm.service.ReportService;
import com.alemandan.crm.repository.ProductoRepository;
import com.alemandan.crm.util.ExcelReportUtilAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/ventas")
public class AdminVentaController {

    private static final Logger logger = LoggerFactory.getLogger(AdminVentaController.class);

    @Autowired
    private AdminVentaService adminVentaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProductoRepository productoRepository;

    // Utilidad para limpiar parámetros
    private Long cleanLong(Long val) {
        if (val == null || val == 0) return null;
        return val;
    }
    private String cleanString(String val) {
        if (val == null || val.trim().isEmpty() || val.equalsIgnoreCase("null")) return null;
        return val;
    }

    @GetMapping
    public String verVentas(
            Model model,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago
    ) {
        usuarioId   = cleanLong(usuarioId);
        productoId  = cleanLong(productoId);
        metodoPago  = cleanString(metodoPago);

        List<Venta> ventas = adminVentaService.filtrarVentas(fechaInicio, fechaFin, usuarioId, productoId, metodoPago);
        model.addAttribute("ventas", ventas);

        List<Usuario> empleados = usuarioService.listarEmpleados();
        model.addAttribute("empleados", empleados);

        List<Producto> productos = productoService.listarProductos();
        model.addAttribute("productos", productos);

        return "adminventas";
    }

    /**
     * Exportar resumen PDF (integrado con ReportService).
     * Reemplaza la implementación anterior para usar el formato estilizado.
     */
    @GetMapping("/exportar-pdf")
    public void exportarPdf(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response
    ) {
        try {
            logger.info("Exportando PDF admin con filtros: fechaInicio={}, fechaFin={}, usuarioId={}, productoId={}, metodoPago={}",
                    fechaInicio, fechaFin, usuarioId, productoId, metodoPago);
            
            usuarioId   = cleanLong(usuarioId);
            productoId  = cleanLong(productoId);
            metodoPago  = cleanString(metodoPago);

            // Parsear fechas (espera formato ISO yyyy-MM-dd), con defaults
            LocalDate today = LocalDate.now();
            LocalDate fromDate;
            LocalDate toDate;
            try {
                fromDate = (fechaInicio == null || fechaInicio.isBlank()) ? today.minusDays(30) : LocalDate.parse(fechaInicio);
            } catch (Exception ex) {
                fromDate = today.minusDays(30);
            }
            try {
                toDate = (fechaFin == null || fechaFin.isBlank()) ? today : LocalDate.parse(fechaFin);
            } catch (Exception ex) {
                toDate = today;
            }

            LocalDateTime start = fromDate.atStartOfDay();
            LocalDateTime end = toDate.atTime(23, 59, 59);

            // Generar PDF con ReportService (resumen estilizado)
            byte[] pdf = reportService.generarResumenVentasPdf(start, end, usuarioId, productoId, metodoPago);

            // Construir filename, incluyendo nombre de producto sanitizado si aplica
            String fileSuffix = "";
            if (productoId != null) {
                Optional<Producto> opt = productoRepository.findById(productoId);
                String prodName = opt.map(Producto::getNombre).orElse("prod" + productoId);
                fileSuffix = "_" + prodName.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
            }
            String filename = "resumen_ventas_" + fromDate + "_" + toDate + fileSuffix + ".pdf";

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(pdf.length);

            try (OutputStream os = response.getOutputStream()) {
                os.write(pdf);
                os.flush();
            }
            
            logger.info("PDF admin exportado exitosamente: {} bytes", pdf.length);
        } catch (Exception e) {
            logger.error("Error al exportar PDF admin: {}", e.getMessage(), e);
            try {
                // Send error response with generic message (detailed error already logged)
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "No se pudo generar el PDF. Por favor, intente nuevamente o contacte al administrador.");
            } catch (Exception sendErrorException) {
                // If sending error fails, log it - response may already be committed
                logger.error("No se pudo enviar respuesta de error al cliente", sendErrorException);
            }
        }
    }

    @GetMapping("/exportar-excel")
    public void exportarExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response
    ) {
        try {
            logger.info("Exportando Excel admin con filtros: fechaInicio={}, fechaFin={}, usuarioId={}, productoId={}, metodoPago={}",
                    fechaInicio, fechaFin, usuarioId, productoId, metodoPago);
            
            usuarioId   = cleanLong(usuarioId);
            productoId  = cleanLong(productoId);
            metodoPago  = cleanString(metodoPago);

            List<Venta> ventas = adminVentaService.filtrarVentas(fechaInicio, fechaFin, usuarioId, productoId, metodoPago);
            logger.info("Generando Excel para {} ventas", ventas.size());

            // Generate Excel to memory BEFORE setting headers to prevent corrupted response
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ExcelReportUtilAdmin.exportVentasExcel(ventas, baos);
            byte[] excelBytes = baos.toByteArray();

            // Set headers only after successful generation
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=ventas_admin.xlsx");
            response.setContentLength(excelBytes.length);

            // Write to response stream (servlet container manages the stream)
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            
            logger.info("Excel admin exportado exitosamente: {} bytes", excelBytes.length);
        } catch (Exception e) {
            logger.error("Error al exportar Excel admin: {}", e.getMessage(), e);
            // Only attempt to send error if response not committed
            if (!response.isCommitted()) {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "No se pudo generar el archivo Excel. Por favor, intente nuevamente o contacte al administrador.");
                } catch (Exception sendErrorException) {
                    logger.error("No se pudo enviar respuesta de error al cliente", sendErrorException);
                }
            } else {
                logger.error("No se pudo enviar respuesta de error: respuesta ya enviada al cliente");
            }
        }
    }

    /**
     * Exportar PDF a partir de una imagen (base64). Delegamos a ReportService para usar el mismo estilo (header/footer).
     * Recibe JSON body con keys: imgBase64, titulo, fecha
     */
    @PostMapping("/exportar-grafico-pdf")
    public void exportarGraficoPdf(@RequestBody Map<String, String> body, HttpServletResponse response) throws Exception {
        String imgBase64 = body.get("imgBase64");
        String titulo = body.get("titulo");
        String fecha = body.get("fecha");

        if (imgBase64 == null || imgBase64.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se recibió la imagen base64.");
            return;
        }

        // delegate to ReportService which returns a byte[] PDF
        byte[] pdf = reportService.generarPdfFromChartBase64(imgBase64, titulo, fecha);

        String safeTitle = (titulo == null || titulo.isBlank()) ? "grafico" : titulo.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
        String filename = "grafico_" + safeTitle + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdf.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(pdf);
            os.flush();
        }
    }
}