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
        // AHORA SIN PREFIJO "ROLE_"
        if(auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
            Usuario admin = usuarioService.findByEmail(auth.getName());
            model.addAttribute("admin", admin);

            model.addAttribute("totalEmpleados", usuarioService.countEmpleados());
            model.addAttribute("totalProveedores", proveedorService.countProveedores());
            model.addAttribute("totalProductos", productoService.countProductos());
            model.addAttribute("totalVentasDia", ventaService.countVentasDelDia());

            return "dashboardadmin";
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("EMPLEADO"))) {
            return "dashboardempleado";
        }
        return "redirect:/login?error";
    }
}