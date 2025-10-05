package com.alemandan.crm.controller;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.service.VentaService;
import com.alemandan.crm.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
public class VentaApiController {

    @Autowired
    private VentaService ventaService;
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/registrar")
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
}