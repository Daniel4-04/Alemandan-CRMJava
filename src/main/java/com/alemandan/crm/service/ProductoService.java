package com.alemandan.crm.service;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> getAllProductos() {
        return productoRepository.findByActivoTrue(); // solo activos
    }

    public Producto saveProducto(Producto producto) {
        List<Producto> inactivos = productoRepository.findByNombreAndActivoFalse(producto.getNombre());
        if (!inactivos.isEmpty()) {
            Producto existente = inactivos.get(0);
            existente.setDescripcion(producto.getDescripcion());
            existente.setCantidad(producto.getCantidad());
            existente.setPrecio(producto.getPrecio());
            existente.setActivo(true);
            return productoRepository.save(existente);
        }
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    public Optional<Producto> getProductoById(Long id) {
        return productoRepository.findById(id);
    }

    public void inactivarProducto(Long id) {
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }
}