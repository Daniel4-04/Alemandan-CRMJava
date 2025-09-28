package com.alemandan.crm.controller;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.service.VentaService;
import com.alemandan.crm.service.ProductoService;
import com.alemandan.crm.service.UsuarioService;
import com.alemandan.crm.util.PdfReportUtil;
import com.alemandan.crm.util.ExcelReportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private UsuarioService usuarioService;

    // Mostrar formulario de caja
    @GetMapping("/caja")
    public String mostrarCaja(Model model, @RequestParam(value = "exito", required = false) String exito,
                              @RequestParam(value = "error", required = false) String error) {
        List<Producto> productos = productoService.getAllProductos();
        model.addAttribute("productos", productos);

        Venta venta = new Venta();
        venta.setDetalles(new ArrayList<>());
        model.addAttribute("venta", venta);

        if ("true".equals(exito)) model.addAttribute("ventaExitosa", true);
        if (error != null && !error.isEmpty()) model.addAttribute("ventaError", error);

        return "ventas/caja";
    }

    // Registrar venta (POST)
    @PostMapping("/registrar")
    public String registrarVenta(@ModelAttribute Venta venta, Authentication auth) {
        String email = ((User)auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(email);
        venta.setUsuario(usuario);

        String error = ventaService.registrarVenta(venta);
        if (error == null) {
            return "redirect:/ventas/caja?exito=true";
        } else {
            return "redirect:/ventas/caja?error=" + error;
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

        String email = ((User)auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(email);

        List<Producto> productos = productoService.getAllProductos();
        model.addAttribute("productos", productos);

        List<Venta> ventas = ventaService.filtrarVentas(usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);
        model.addAttribute("ventas", ventas);

        return "ventas/misventas";
    }

    // Exportar historial filtrado a PDF
    @GetMapping("/exportar-pdf")
    public void exportarPdf(
            Authentication auth,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response) throws Exception {

        String email = ((User)auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(email);

        List<Venta> ventas = ventaService.filtrarVentas(usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=mis_ventas.pdf");

        PdfReportUtil.exportVentasPdf(ventas, response.getOutputStream());
    }

    // Exportar historial filtrado a Excel
    @GetMapping("/exportar-excel")
    public void exportarExcel(
            Authentication auth,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String metodoPago,
            HttpServletResponse response) throws Exception {

        String email = ((User)auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(email);

        List<Venta> ventas = ventaService.filtrarVentas(usuario.getId(), fechaInicio, fechaFin, productoId, metodoPago);

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=mis_ventas.xlsx");

        ExcelReportUtil.exportVentasExcel(ventas, response.getOutputStream());
    }
}