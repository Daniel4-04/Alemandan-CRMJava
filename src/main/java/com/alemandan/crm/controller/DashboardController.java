package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.service.UsuarioService;
import com.alemandan.crm.service.ProveedorService;
import com.alemandan.crm.service.ProductoService;
import com.alemandan.crm.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ProveedorService proveedorService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private VentaService ventaService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        if(auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
            Usuario admin = usuarioService.findByEmail(auth.getName());
            model.addAttribute("admin", admin);

            model.addAttribute("totalEmpleados", usuarioService.countEmpleados());
            model.addAttribute("totalProveedores", proveedorService.countProveedores());
            model.addAttribute("totalProductos", productoService.countProductos());
            model.addAttribute("totalVentasDia", ventaService.countVentasDelDia());

            return "dashboardadmin";
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("EMPLEADO"))) {
            Usuario empleado = usuarioService.findByEmail(auth.getName());
            model.addAttribute("empleado", empleado);

            model.addAttribute("misVentasDia", ventaService.countVentasDelDiaEmpleado(empleado.getId()));
            model.addAttribute("totalMisVentas", ventaService.countVentasTotalesEmpleado(empleado.getId()));

            // Obtiene el resumen directamente del servicio, NO con RestTemplate
            try {
                Map resumen = ventaService.getResumenVentas();

                // Formatear la fecha si existe
                Object fechaObj = resumen.get("fecha");
                String fechaFormateada = "N/A";
                if (fechaObj != null && !fechaObj.toString().equals("N/A")) {
                    try {
                        OffsetDateTime odt = OffsetDateTime.parse(fechaObj.toString());
                        fechaFormateada = odt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    } catch (Exception e) {
                        fechaFormateada = fechaObj.toString();
                    }
                }
                resumen.put("fechaFormateada", fechaFormateada);

                model.addAttribute("resumenVentas", resumen);
            } catch (Exception e) {
                e.printStackTrace(); // Para depuración
                model.addAttribute("resumenVentas", Map.of(
                        "totalVentas", "N/A",
                        "montoTotal", "N/A",
                        "fechaFormateada", "N/A"
                ));
            }

            return "dashboardempleado";
        }
        return "redirect:/login?error";
    }
}