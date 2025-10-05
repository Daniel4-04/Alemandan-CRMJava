package com.alemandan.crm.service;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.model.Producto;
import com.alemandan.crm.repository.VentaRepository;
import com.alemandan.crm.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    public String registrarVenta(Venta venta) {
        venta.setFecha(LocalDateTime.now());
        double total = 0.0;

        // Validación de stock
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId()).orElseThrow();
            if (detalle.getCantidad() > producto.getCantidad()) {
                return "No hay suficiente stock del producto '" + producto.getNombre() + "'. Disponible: " + producto.getCantidad();
            }
            if (detalle.getCantidad() < 1) {
                return "La cantidad del producto '" + producto.getNombre() + "' debe ser al menos 1.";
            }
        }

        // Si pasó validación, realiza la venta
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId()).orElseThrow();
            producto.setCantidad(producto.getCantidad() - detalle.getCantidad());
            productoRepository.save(producto);

            detalle.setPrecioUnitario(producto.getPrecio());
            total += detalle.getCantidad() * producto.getPrecio();
            detalle.setVenta(venta);
        }
        venta.setTotal(total);
        ventaRepository.save(venta);
        return null; // null = venta exitosa
    }

    // Buscar ventas por usuario
    public List<Venta> obtenerVentasPorUsuario(Long usuarioId) {
        return ventaRepository.findByUsuarioId(usuarioId);
    }

    // Filtrar ventas
    public List<Venta> filtrarVentas(Long usuarioId, String fechaInicio, String fechaFin, Long productoId, String metodoPago) {
        List<Venta> ventas = ventaRepository.findByUsuarioId(usuarioId);

        // Filtrado por fecha
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            ventas = ventas.stream().filter(v -> !v.getFecha().toLocalDate().isBefore(inicio)).collect(Collectors.toList());
        }
        if (fechaFin != null && !fechaFin.isEmpty()) {
            LocalDate fin = LocalDate.parse(fechaFin);
            ventas = ventas.stream().filter(v -> !v.getFecha().toLocalDate().isAfter(fin)).collect(Collectors.toList());
        }
        // Filtrado por producto
        if (productoId != null) {
            ventas = ventas.stream().filter(v -> v.getDetalles().stream().anyMatch(d -> d.getProducto().getId().equals(productoId))).collect(Collectors.toList());
        }
        // Filtrado por método de pago
        if (metodoPago != null && !metodoPago.isEmpty()) {
            ventas = ventas.stream().filter(v -> metodoPago.equalsIgnoreCase(v.getMetodoPago())).collect(Collectors.toList());
        }
        return ventas;
    }

    // Actualizado: conteo de ventas del día usando LocalDateTime
    public long countVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
        return ventaRepository.countByFechaBetween(inicio, fin);
    }

    // NUEVO: ventas del día de un empleado
    public long countVentasDelDiaEmpleado(Long empleadoId) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
        return ventaRepository.countByUsuarioIdAndFechaBetween(empleadoId, inicio, fin);
    }

    // NUEVO: ventas totales de un empleado
    public long countVentasTotalesEmpleado(Long empleadoId) {
        return ventaRepository.countByUsuarioId(empleadoId);
    }
}