package com.alemandan.crm.service;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.model.Producto;
import com.alemandan.crm.repository.VentaRepository;
import com.alemandan.crm.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Nuevo: procesa y guarda la venta.
     * - Valida stock y cantidades.
     * - Actualiza stock en Producto.
     * - Calcula precioUnitario, ivaRate e ivaMonto por DetalleVenta.
     * - Calcula subtotal, iva total y total de la venta.
     * - Persiste la venta con sus detalles y devuelve la entidad guardada (con id).
     *
     * Lanza IllegalArgumentException en caso de validación fallida.
     */
    @Transactional
    public Venta procesarYGuardarVenta(Venta venta) {
        venta.setFecha(LocalDateTime.now());

        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La venta no contiene productos.");
        }

        // Validación de stock y cantidades
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle.getProducto() == null || detalle.getProducto().getId() == null) {
                throw new IllegalArgumentException("Producto inválido en detalle.");
            }
            Producto producto = productoRepository.findById(detalle.getProducto().getId()).orElse(null);
            if (producto == null) {
                throw new IllegalArgumentException("Producto con id " + detalle.getProducto().getId() + " no encontrado.");
            }
            int qty = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
            if (qty < 1) {
                throw new IllegalArgumentException("La cantidad del producto '" + producto.getNombre() + "' debe ser al menos 1.");
            }
            int available = producto.getCantidad() == null ? 0 : producto.getCantidad();
            if (qty > available) {
                throw new IllegalArgumentException("No hay suficiente stock del producto '" + producto.getNombre() + "'. Disponible: " + available);
            }
        }

        // Procesamiento
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;

        List<DetalleVenta> detallesPersist = new ArrayList<>();

        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId()).orElseThrow();

            // Actualizar stock
            int currentStock = producto.getCantidad() == null ? 0 : producto.getCantidad();
            int qty = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
            producto.setCantidad(currentStock - qty);
            productoRepository.save(producto);

            // Determinar precioUnitario (Detalle puede venir con precioUnitario, si no usamos producto.precio)
            BigDecimal precioUnitario;
            if (detalle.getPrecioUnitario() != null) {
                precioUnitario = detalle.getPrecioUnitario();
            } else {
                precioUnitario = producto.getPrecio() == null ? BigDecimal.ZERO : BigDecimal.valueOf(producto.getPrecio());
            }

            // Subtotal de la línea
            BigDecimal lineaSubtotal = precioUnitario.multiply(BigDecimal.valueOf(qty));

            // IVA aplicado: tomar producto.getIva() (BigDecimal) si está; si es null -> 0
            BigDecimal ivaRate = producto.getIva() == null ? BigDecimal.ZERO : producto.getIva();

            // Calcular monto de IVA de la línea (redondeo a 2 decimales)
            BigDecimal ivaLinea = lineaSubtotal.multiply(ivaRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Setear valores en DetalleVenta y relación con venta
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setIvaRate(ivaRate);
            detalle.setIvaMonto(ivaLinea);
            detalle.setVenta(venta);

            subtotal = subtotal.add(lineaSubtotal);
            totalIva = totalIva.add(ivaLinea);

            detallesPersist.add(detalle);
        }

        BigDecimal subtotalRounded = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaRounded = totalIva.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalRounded.add(ivaRounded).setScale(2, RoundingMode.HALF_UP);

        venta.setSubtotal(subtotalRounded);
        venta.setIva(ivaRounded);
        venta.setTotal(total);
        venta.setDetalles(detallesPersist);

        // Persistir la venta
        Venta saved = ventaRepository.save(venta);
        return saved;
    }

    /**
     * Método legacy compat: mantiene la antigua firma registrarVenta(Venta) que retornaba
     * null en caso de éxito o mensaje con error.
     * Internamente delega a procesarYGuardarVenta y captura excepciones para devolver mensajes.
     */
    @Transactional
    public String registrarVenta(Venta venta) {
        try {
            Venta saved = procesarYGuardarVenta(venta);
            // exitoso
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            // no exponer detalles sensibles, pero devolver algo útil
            return "Error inesperado al procesar la venta: " + e.getMessage();
        }
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
            ventas = ventas.stream().filter(v -> v.getDetalles().stream().anyMatch(d -> d.getProducto() != null && productoId.equals(d.getProducto().getId()))).collect(Collectors.toList());
        }
        // Filtrado por método de pago
        if (metodoPago != null && !metodoPago.isEmpty()) {
            ventas = ventas.stream().filter(v -> metodoPago.equalsIgnoreCase(v.getMetodoPago())).collect(Collectors.toList());
        }
        return ventas;
    }

    // Conteo de ventas del día usando LocalDateTime
    public long countVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
        return ventaRepository.countByFechaBetween(inicio, fin);
    }

    // Ventas del día de un empleado
    public long countVentasDelDiaEmpleado(Long empleadoId) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
        return ventaRepository.countByUsuarioIdAndFechaBetween(empleadoId, inicio, fin);
    }

    // Ventas totales de un empleado
    public long countVentasTotalesEmpleado(Long empleadoId) {
        return ventaRepository.countByUsuarioId(empleadoId);
    }

    // NUEVO: resumen general de ventas para dashboard y web service
    public Map<String, Object> getResumenVentas() {
        Map<String, Object> resumen = new HashMap<>();
        // Total de ventas
        resumen.put("totalVentas", ventaRepository.count());
        // Monto total de ventas
        BigDecimal montoTotal = ventaRepository.findAll()
                .stream()
                .map(v -> v.getTotal() == null ? BigDecimal.ZERO : v.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resumen.put("montoTotal", montoTotal);
        // Fecha actual en formato ISO8601 (string compatible con frontend y formateo)
        OffsetDateTime fechaActual = OffsetDateTime.now(ZoneOffset.UTC);
        resumen.put("fecha", fechaActual.toString());

        return resumen;
    }
}