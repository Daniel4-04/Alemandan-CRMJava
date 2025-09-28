package com.alemandan.crm.controller;

import com.alemandan.crm.model.Proveedor;
import com.alemandan.crm.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("proveedores", proveedorService.listarProveedores());
        return "listaprov";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "formprov";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Proveedor proveedor) {
        proveedorService.guardarProveedor(proveedor);
        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("proveedor", proveedorService.obtenerProveedor(id));
        return "formprov";
    }

    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Proveedor proveedor) {
        proveedorService.guardarProveedor(proveedor);
        return "redirect:/proveedores";
    }

    @GetMapping("/inactivar/{id}")
    public String inactivar(@PathVariable Long id) {
        proveedorService.inactivarProveedor(id);
        return "redirect:/proveedores";
    }

    @GetMapping("/activar/{id}")
    public String activar(@PathVariable Long id) {
        proveedorService.activarProveedor(id);
        return "redirect:/proveedores";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        proveedorService.eliminarProveedor(id);
        return "redirect:/proveedores";
    }
}