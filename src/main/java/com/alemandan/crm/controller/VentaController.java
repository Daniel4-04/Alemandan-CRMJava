package com.alemandan.crm.controller;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.VentaRepository;
import com.alemandan.crm.repository.ProductoRepository;
import com.alemandan.crm.service.VentaService;
import com.alemandan.crm.service.ProductoService;
import com.alemandan.crm.service.UsuarioService;
import com.alemandan.crm.service.ReportService;
import com.alemandan.crm.util.ExcelReportUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import java.util.Optional;

/**
 * Controlador de ventas (caja, historial y exportes).
 * - Guarda ventas (form clásico y AJAX).
 * - Al generar recibo por AJAX recarga la venta desde BD para asegurar que DetalleVenta->Producto esté inicializado.
 * - Rellena precios/IVA si faltan (y recalcula totales) antes de generar el PDF.
 * - Provee endpoint /ventas/recibo/{id} para descarga directa del PDF.
 */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    @Autowired
    private VentaService ventaService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // Mostrar formulario de caja
    @GetMapping("/caja")
    public String mostrarCaja(Model model, Authentication auth,
                              @RequestParam(value = "exito", required = false) String exito,
                              @RequestParam(value = "error", required = false) String error) {
        List<Producto> productos = productoService.getAllProductos();
        model.addAttribute("productos", productos);

        Venta venta = new Venta();
        venta.setDetalles(new ArrayList<>());
        model.addAttribute("venta", venta);

        // Agregar el empleado logueado al modelo (para caja.html)
        if (auth != null && auth.isAuthenticated()) {
            String email = ((User) auth.getPrincipal()).getUsername();
            Usuario empleado = usuarioService.findByEmail(email);
            model.addAttribute("empleado", empleado);
        }

        if ("true".equals(exito)) model.addAttribute("ventaExitosa", true);
        if (error != null && !error.isEmpty()) model.addAttribute("ventaError", error);

        return "ventas/caja";
    }

    // Registrar venta (POST para formulario clásico)
    @PostMapping("/registrar")
    public String registrarVenta(@ModelAttribute Venta venta, Authentication auth) {
        String email = ((User) auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(email);
        venta.setUsuario(usuario);

        try {
            Venta saved = ventaService.procesarYGuardarVenta(venta);
            // flujo clásico: redirigir con éxito
            return "redirect:/ventas/caja?exito=true";
        } catch (IllegalArgumentException e) {
            return "redirect:/ventas/caja?error=" + e.getMessage();
        } catch (Exception e) {
            return "redirect:/ventas/caja?error=Error inesperado al guardar la venta";
        }
    }

    // Registrar venta por AJAX (flujo moderno): devuelve JSON con success, ventaId y receiptBase64 (PDF)
    @PostMapping("/api/ventas/registrar")
    @ResponseBody
    public Map<String, Object> registrarVentaAjax(@RequestBody Venta venta, Authentication auth) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String email = ((User) auth.getPrincipal()).getUsername();
            Usuario usuario = usuarioService.findByEmail(email);
            venta.setUsuario(usuario);

            // 1) Procesar y guardar la venta (valida stock, actualiza stock y persiste)
            Venta saved = ventaService.procesarYGuardarVenta(venta);

            // 2) Recargar la venta desde la BD para asegurar que las relaciones (detalles -> producto) estén inicializadas
            Venta full = ventaRepository.findById(saved.getId()).orElse(saved);

            // 3) Rellenar valores faltantes en cada detalle (precioUnitario, ivaRate, ivaMonto) desde la entidad Producto si es necesario
            BigDecimal calcSubtotal = BigDecimal.ZERO;
            BigDecimal calcIva = BigDecimal.ZERO;
            if (full.getDetalles() != null) {
                for (DetalleVenta d : full.getDetalles()) {
                    if (d.getProducto() != null && d.getProducto().getId() != null) {
                        Long pid = d.getProducto().getId();
                        Optional<Producto> optP = productoRepository.findById(pid);
                        if (optP.isPresent()) {
                            Producto p = optP.get();
                            d.setProducto(p); // asegurar que producto esté completo

                            // precio unitario
                            if (d.getPrecioUnitario() == null || d.getPrecioUnitario().compareTo(BigDecimal.ZERO) == 0) {
                                if (p.getPrecio() != null) {
                                    d.setPrecioUnitario(BigDecimal.valueOf(p.getPrecio()));
                                } else {
                                    d.setPrecioUnitario(BigDecimal.ZERO);
                                }
                            }

                            // iva rate
                            if (d.getIvaRate() == null) {
                                d.setIvaRate(p.getIva() == null ? BigDecimal.ZERO : p.getIva());
                            }

                            // iva monto
                            if (d.getIvaMonto() == null || d.getIvaMonto().compareTo(BigDecimal.ZERO) == 0) {
                                BigDecimal qty = BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad());
                                BigDecimal lineaSubtotal = d.getPrecioUnitario().multiply(qty);
                                BigDecimal ivaMonto = lineaSubtotal.multiply(d.getIvaRate() == null ? BigDecimal.ZERO : d.getIvaRate())
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                d.setIvaMonto(ivaMonto);
                            }
                        } else {
                            // producto no encontrado: asegurar campos a 0
                            if (d.getPrecioUnitario() == null) d.setPrecioUnitario(BigDecimal.ZERO);
                            if (d.getIvaRate() == null) d.setIvaRate(BigDecimal.ZERO);
                            if (d.getIvaMonto() == null) d.setIvaMonto(BigDecimal.ZERO);
                        }
                    } else {
                        // sin producto: asegurar campos a 0
                        if (d.getPrecioUnitario() == null) d.setPrecioUnitario(BigDecimal.ZERO);
                        if (d.getIvaRate() == null) d.setIvaRate(BigDecimal.ZERO);
                        if (d.getIvaMonto() == null) d.setIvaMonto(BigDecimal.ZERO);
                    }

                    // acumular para recálculo de totales locales
                    BigDecimal qty = BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad());
                    BigDecimal lineaSubtotal = (d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario()).multiply(qty);
                    BigDecimal ivaLinea = d.getIvaMonto() == null ? BigDecimal.ZERO : d.getIvaMonto();
                    calcSubtotal = calcSubtotal.add(lineaSubtotal);
                    calcIva = calcIva.add(ivaLinea);
                }
            }

            // 4) Si la Venta no tiene totales (o son cero), rellenarlos con cálculos
            if (full.getSubtotal() == null || full.getSubtotal().compareTo(BigDecimal.ZERO) == 0) {
                full.setSubtotal(calcSubtotal.setScale(2, RoundingMode.HALF_UP));
            }
            if (full.getIva() == null || full.getIva().compareTo(BigDecimal.ZERO) == 0) {
                full.setIva(calcIva.setScale(2, RoundingMode.HALF_UP));
            }
            if (full.getTotal() == null || full.getTotal().compareTo(BigDecimal.ZERO) == 0) {
                full.setTotal(full.getSubtotal().add(full.getIva()).setScale(2, RoundingMode.HALF_UP));
            }

            // 5) Persistir cambios (opcional, asegura que los detalles ahora contienen precio/iva en BD)
            full = ventaRepository.save(full);

            // 6) Generar PDF usando la entidad recargada y completa
            byte[] pdf = reportService.generarReciboVentaPdf(full);
            String base64 = Base64.getEncoder().encodeToString(pdf);

            resp.put("success", true);
            resp.put("ventaId", saved.getId());
            resp.put("receiptBase64", base64);
        } catch (IllegalArgumentException e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", "Error inesperado al registrar la venta: " + e.getMessage());
        }
        return resp;
    }

    // Endpoint para descargar recibo PDF por id (más eficiente que base64)
    @GetMapping("/recibo/{id}")
    public void descargarRecibo(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Venta v = ventaRepository.findById(id).orElse(null);
        if (v == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Venta no encontrada");
            return;
        }

        // Asegurar detalles/ producto inicializados y rellenar valores faltantes
        if (v.getDetalles() != null) {
            for (DetalleVenta d : v.getDetalles()) {
                if (d.getProducto() != null && d.getProducto().getId() != null) {
                    Long pid = d.getProducto().getId();
                    productoRepository.findById(pid).ifPresent(p -> {
                        d.setProducto(p);
                        if (d.getPrecioUnitario() == null || d.getPrecioUnitario().compareTo(BigDecimal.ZERO) == 0) {
                            if (p.getPrecio() != null) d.setPrecioUnitario(BigDecimal.valueOf(p.getPrecio()));
                            else d.setPrecioUnitario(BigDecimal.ZERO);
                        }
                        if (d.getIvaRate() == null) {
                            d.setIvaRate(p.getIva() == null ? BigDecimal.ZERO : p.getIva());
                        }
                        if (d.getIvaMonto() == null || d.getIvaMonto().compareTo(BigDecimal.ZERO) == 0) {
                            BigDecimal pu = d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario();
                            BigDecimal cantidad = BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad());
                            BigDecimal rate = d.getIvaRate() == null ? BigDecimal.ZERO : d.getIvaRate();
                            BigDecimal lineaSubtotal = pu.multiply(cantidad);
                            BigDecimal ivaMonto = lineaSubtotal.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            d.setIvaMonto(ivaMonto);
                        }
                    });
                } else {
                    if (d.getPrecioUnitario() == null) d.setPrecioUnitario(BigDecimal.ZERO);
                    if (d.getIvaRate() == null) d.setIvaRate(BigDecimal.ZERO);
                    if (d.getIvaMonto() == null) d.setIvaMonto(BigDecimal.ZERO);
                }
            }
            // Recalcular totales si faltan
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal ivaTotal = BigDecimal.ZERO;
            for (DetalleVenta d : v.getDetalles()) {
                BigDecimal pu = d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario();
                BigDecimal qty = BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad());
                BigDecimal lineaSubtotal = pu.multiply(qty);
                subtotal = subtotal.add(lineaSubtotal);
                ivaTotal = ivaTotal.add(d.getIvaMonto() == null ? BigDecimal.ZERO : d.getIvaMonto());
            }
            if (v.getSubtotal() == null || v.getSubtotal().compareTo(BigDecimal.ZERO) == 0) v.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
            if (v.getIva() == null || v.getIva().compareTo(BigDecimal.ZERO) == 0) v.setIva(ivaTotal.setScale(2, RoundingMode.HALF_UP));
            if (v.getTotal() == null || v.getTotal().compareTo(BigDecimal.ZERO) == 0) v.setTotal(v.getSubtotal().add(v.getIva()).setScale(2, RoundingMode.HALF_UP));
            // Persistir los ajustes (opcional)
            v = ventaRepository.save(v);
        }

        byte[] pdf = reportService.generarReciboVentaPdf(v);
        String filename = "recibo_venta_" + (v.getId() == null ? "recibo" : v.getId()) + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        response.setContentLength(pdf.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(pdf);
            os.flush();
        }
    }

    // Historial de ventas con filtros
    @GetMapping("/mis-ventas")
    public String verMisVentas(
            Model model, Authentication auth,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago) {

        String email = ((User) auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(email);

        List<Producto> productos = productoService.getAllProductos();
        model.addAttribute("productos", productos);

        List<Venta> ventas = ventaService.filtrarVentas(usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);
        model.addAttribute("ventas", ventas);

        return "ventas/misventas";
    }

    // Exportar historial filtrado a PDF: se obtiene la MISMA lista que usa la UI y se la pasa al ReportService
    @GetMapping("/exportar-pdf")
    public void exportarPdf(
            Authentication auth,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response) {
        try {
            String email = ((User) auth.getPrincipal()).getUsername();
            Usuario usuario = usuarioService.findByEmail(email);
            
            logger.info("Exportando PDF empleado para usuario ID: {}, filtros: fechaInicio={}, fechaFin={}, productoId={}, metodoPago={}",
                    usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);

            // Obtener exactamente la misma lista que se muestra en la UI
            List<Venta> ventas = ventaService.filtrarVentas(usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);
            logger.info("Generando PDF para {} ventas", ventas.size());

            // Parsear fechas con defaults (ISO yyyy-MM-dd esperadas en los inputs)
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

            // Llamar al ReportService con la LISTA de ventas para generar el PDF que coincida con la UI
            byte[] pdf = reportService.generarMisVentasPdfFromList(ventas, start, end, usuario.getNombre());

            String safeName = usuario.getNombre() == null ? "mis_ventas" : usuario.getNombre().replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
            String filename = "mis_ventas_" + safeName + "_" + fromDate + "_" + toDate + ".pdf";

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(pdf.length);

            try (OutputStream os = response.getOutputStream()) {
                os.write(pdf);
                os.flush();
            }
            
            logger.info("PDF empleado exportado exitosamente: {} bytes", pdf.length);
        } catch (Exception e) {
            logger.error("Error al exportar PDF empleado: {}", e.getMessage(), e);
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

    // Exportar historial filtrado a Excel
    @GetMapping("/exportar-excel")
    public void exportarExcel(
            Authentication auth,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response) {
        try {
            String email = ((User) auth.getPrincipal()).getUsername();
            Usuario usuario = usuarioService.findByEmail(email);
            
            logger.info("Exportando Excel empleado para usuario ID: {}, filtros: fechaInicio={}, fechaFin={}, productoId={}, metodoPago={}",
                    usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);

            List<Venta> ventas = ventaService.filtrarVentas(usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);
            logger.info("Generando Excel para {} ventas", ventas.size());

            // Generate Excel to memory BEFORE setting headers to prevent corrupted response
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ExcelReportUtil.exportVentasExcel(ventas, baos);
            byte[] excelBytes = baos.toByteArray();

            // Set headers only after successful generation
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=mis_ventas.xlsx");
            response.setContentLength(excelBytes.length);

            // Write to response stream
            try (OutputStream os = response.getOutputStream()) {
                os.write(excelBytes);
                os.flush();
            }
            
            logger.info("Excel empleado exportado exitosamente: {} bytes", excelBytes.length);
        } catch (Exception e) {
            logger.error("Error al exportar Excel empleado: {}", e.getMessage(), e);
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
}