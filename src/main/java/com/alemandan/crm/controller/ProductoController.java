package com.alemandan.crm.controller;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/productos")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    // Listar todos los productos (activos e inactivos)
    @GetMapping
    public String listarProductos(Model model) {
        List<Producto> productos = productoService.listarProductos();
        model.addAttribute("productos", productos);
        return "productos/listaprod";
    }

    // Mostrar formulario para agregar producto
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoProducto(Model model) {
        model.addAttribute("producto", new Producto());
        return "productos/nuevoprod";
    }

    // Guardar nuevo producto
    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto) {
        productoService.saveProducto(producto);
        return "redirect:/productos";
    }

    // Mostrar formulario para editar producto
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable Long id, Model model) {
        Optional<Producto> producto = productoService.getProductoById(id);
        if (producto.isPresent()) {
            model.addAttribute("producto", producto.get());
            return "productos/editarprod";
        } else {
            // Si no existe, redirige al listado
            return "redirect:/productos";
        }
    }

    // Actualizar producto
    @PostMapping("/actualizar")
    public String actualizarProducto(@ModelAttribute Producto producto) {
        productoService.saveProducto(producto);
        return "redirect:/productos";
    }

    // Inactivar producto
    @GetMapping("/inactivar/{id}")
    public String inactivarProducto(@PathVariable Long id) {
        productoService.inactivarProducto(id);
        return "redirect:/productos";
    }

    // Activar producto
    @GetMapping("/activar/{id}")
    public String activarProducto(@PathVariable Long id) {
        productoService.activarProducto(id);
        return "redirect:/productos";
    }

    // NUEVO: Endpoint para búsqueda AJAX (empleado/caja)
    @GetMapping("/api/productos/buscar")
    @ResponseBody
    public List<Producto> buscarProductos(@RequestParam(required = false) String term) {
        if (term == null || term.trim().isEmpty()) {
            return productoService.getAllProductos();
        }
        return productoService.buscarPorNombre(term.trim());
    }
}