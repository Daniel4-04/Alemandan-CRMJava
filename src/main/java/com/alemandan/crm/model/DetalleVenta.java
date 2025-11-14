package com.alemandan.crm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad DetalleVenta actualizada para soportar cálculo y persistencia de IVA por línea.
 *
 * Cambios principales:
 * - precioUnitario: Double -> BigDecimal (precisión financiera).
 * - ivaRate: porcentaje aplicado (ej. 19.00). BigDecimal, escala 2.
 * - ivaMonto: importe de IVA para la línea (precioUnitario * cantidad * ivaRate/100).
 *
 * Nota:
 * - Si tu esquema de BD se actualiza automáticamente (hibernate ddl-auto=update) no necesitas SQL manual.
 * - Si usas migraiones (Flyway/Liquibase) añade la ALTER TABLE correspondiente.
 */
@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;

    // Precio unitario al momento de la venta (se guarda para trazabilidad)
    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    // Tasa de IVA aplicada (porcentaje). Null o 0 => exento.
    @Column(name = "iva_rate", precision = 5, scale = 2)
    private BigDecimal ivaRate;

    // Monto de IVA aplicado en esta línea (precioUnitario * cantidad * ivaRate/100)
    @Column(name = "iva_monto", precision = 12, scale = 2)
    private BigDecimal ivaMonto;

    // Constructors
    public DetalleVenta() {}

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidad() { return cantidad == null ? 0 : cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario == null ? BigDecimal.ZERO : precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario == null ? BigDecimal.ZERO : precioUnitario; }

    public BigDecimal getIvaRate() { return ivaRate == null ? BigDecimal.ZERO : ivaRate; }
    public void setIvaRate(BigDecimal ivaRate) { this.ivaRate = ivaRate == null ? BigDecimal.ZERO : ivaRate; }

    public BigDecimal getIvaMonto() { return ivaMonto == null ? BigDecimal.ZERO : ivaMonto; }
    public void setIvaMonto(BigDecimal ivaMonto) { this.ivaMonto = ivaMonto == null ? BigDecimal.ZERO : ivaMonto; }

    /**
     * Utilitario: calcula y retorna el subtotal de la línea (precioUnitario * cantidad),
     * sin incluir IVA.
     */
    @Transient
    public BigDecimal getLineaSubtotal() {
        return getPrecioUnitario().multiply(BigDecimal.valueOf(getCantidad()));
    }

    /**
     * Utilitario: calcula el IVA de la línea a partir de precioUnitario, cantidad e ivaRate.
     * No modifica campos persistidos; sólo devuelve el valor calculado.
     */
    @Transient
    public BigDecimal calcularIvaLinea() {
        BigDecimal subtotal = getLineaSubtotal();
        if (getIvaRate().compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return subtotal.multiply(getIvaRate()).divide(BigDecimal.valueOf(100));
    }
}