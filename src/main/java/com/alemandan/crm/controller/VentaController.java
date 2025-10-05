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
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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
            String email = ((User)auth.getPrincipal()).getUsername();
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

    // NUEVO: Registrar venta por AJAX (flujo moderno)
    @PostMapping("/api/ventas/registrar")
    @ResponseBody
    public Map<String, Object> registrarVentaAjax(@RequestBody Venta venta, Authentication auth) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String email = ((User)auth.getPrincipal()).getUsername();
            Usuario usuario = usuarioService.findByEmail(email);
            venta.setUsuario(usuario);
            String error = ventaService.registrarVenta(venta);
            if (error == null) {
                resp.put("success", true);
                resp.put("ventaId", venta.getId());
            } else {
                resp.put("success", false);
                resp.put("error", error);
            }
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", "Error inesperado al registrar la venta: " + e.getMessage());
        }
        return resp;
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