package com.alemandan.crm.service;

import com.alemandan.crm.model.Proveedor;
import com.alemandan.crm.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;

    public List<Proveedor> listarProveedores() {
        return proveedorRepository.findAll();
    }

    public Proveedor guardarProveedor(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public Proveedor obtenerProveedor(Long id) {
        return proveedorRepository.findById(id).orElse(null);
    }

    public void inactivarProveedor(Long id) {
        Proveedor p = proveedorRepository.findById(id).orElse(null);
        if (p != null) {
            p.setActivo(false);
            proveedorRepository.save(p);
        }
    }

    public void activarProveedor(Long id) {
        Proveedor p = proveedorRepository.findById(id).orElse(null);
        if (p != null) {
            p.setActivo(true);
            proveedorRepository.save(p);
        }
    }

    public void eliminarProveedor(Long id) {
        proveedorRepository.deleteById(id);
    }
}