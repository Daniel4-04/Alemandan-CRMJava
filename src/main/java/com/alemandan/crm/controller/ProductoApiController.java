package com.alemandan.crm.controller;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoApiController {

    @Autowired
    private ProductoService productoService;

    @GetMapping("/buscar")
    public List<Producto> buscarProductos(@RequestParam(required = false) String term) {
        if (term == null || term.trim().isEmpty()) {
            return productoService.getAllProductos();
        }
        return productoService.buscarPorNombre(term.trim());
    }
}