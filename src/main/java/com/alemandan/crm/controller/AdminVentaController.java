package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Venta;
import com.alemandan.crm.service.AdminVentaService;
import com.alemandan.crm.service.UsuarioService;
import com.alemandan.crm.service.ProductoService;
import com.alemandan.crm.util.PdfReportUtilAdmin;
import com.alemandan.crm.util.ExcelReportUtilAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.Base64;

@Controller
@RequestMapping("/admin/ventas")
public class AdminVentaController {

    @Autowired
    private AdminVentaService adminVentaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoService productoService;

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

    @GetMapping("/exportar-pdf")
    public void exportarPdf(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response
    ) throws Exception {
        usuarioId   = cleanLong(usuarioId);
        productoId  = cleanLong(productoId);
        metodoPago  = cleanString(metodoPago);

        List<Venta> ventas = adminVentaService.filtrarVentas(fechaInicio, fechaFin, usuarioId, productoId, metodoPago);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=ventas_admin.pdf");

        PdfReportUtilAdmin.exportVentasPdf(ventas, response.getOutputStream());
    }

    @GetMapping("/exportar-excel")
    public void exportarExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response
    ) throws Exception {
        usuarioId   = cleanLong(usuarioId);
        productoId  = cleanLong(productoId);
        metodoPago  = cleanString(metodoPago);

        List<Venta> ventas = adminVentaService.filtrarVentas(fechaInicio, fechaFin, usuarioId, productoId, metodoPago);

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=ventas_admin.xlsx");

        ExcelReportUtilAdmin.exportVentasExcel(ventas, response.getOutputStream());
    }

    @PostMapping("/exportar-grafico-pdf")
    public void exportarGraficoPdf(@RequestBody Map<String, String> body, HttpServletResponse response) throws Exception {
        System.out.println("Método exportar-grafico-pdf INVOCADO");
        try {
            String imgBase64 = body.get("imgBase64");
            String titulo = body.get("titulo");
            String fecha = body.get("fecha");
            System.out.println("TITULO: " + titulo);
            System.out.println("FECHA: " + fecha);
            System.out.println("BASE64 length: " + (imgBase64 != null ? imgBase64.length() : "null"));
            if (imgBase64 != null) {
                System.out.println("BASE64 (first 100): " + imgBase64.substring(0, Math.min(100, imgBase64.length())));
            }

            if (imgBase64 == null || imgBase64.trim().isEmpty()) {
                System.out.println("ERROR: No se recibió la imagen base64.");
                throw new Exception("No se recibió la imagen base64.");
            }
            imgBase64 = imgBase64.replace("data:image/png;base64,", "").replace("\n", "").replace("\r", "");

            System.out.println("Decodificando imagen...");
            byte[] imgBytes = java.util.Base64.getDecoder().decode(imgBase64);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=grafico_ventas.pdf");

            System.out.println("Abriendo documento PDF...");
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            document.add(new com.itextpdf.text.Paragraph(titulo, titleFont));
            document.add(new com.itextpdf.text.Paragraph("Fecha de exportación: " + fecha));
            document.add(com.itextpdf.text.Chunk.NEWLINE);

            System.out.println("Añadiendo imagen...");
            com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(imgBytes);
            image.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
            image.scaleToFit(500, 300);
            document.add(image);

            document.close();
            System.out.println("PDF generado correctamente.");
        } catch (Exception e) {
            System.out.println("ERROR PDF: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generando el PDF: " + e.getMessage());
        }
    }
}