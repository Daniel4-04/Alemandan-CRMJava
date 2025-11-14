package com.alemandan.crm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Entidad Producto actualizada:
 * - Se añadió el campo 'iva' (porcentaje) opcional. Null o 0 => exento.
 * - Se mantienen los campos existentes tal como estaban (precio sigue siendo Double
 *   para evitar cambios en el resto del código). Si prefieres cambiar precio a BigDecimal
 *   lo puedo hacer también (requiere pequeños ajustes en otros lugares).
 */
@Entity
@Table(name = "producto")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;

    @NotNull
    private Integer cantidad;

    @NotNull
    private Double precio;

    // NUEVO: Estado de producto para activar/inactivar
    @Column(nullable = false)
    private Boolean activo = true;

    // NUEVO: Unidad de medida (ej. kg, unidad, lt)
    private String unidadMedida;

    // NUEVO: Ruta pública/relativa de la imagen principal
    private String imagePath;

    // NUEVO: relación con categoría (opcional)
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    // NUEVO: porcentaje de IVA (ej. 19.00). Null o 0 => exento.
    // precision 5, scale 2 soporta 100.00 como máximo (suficiente para porcentajes)
    @Column(name = "iva", precision = 5, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public BigDecimal getIva() { return iva == null ? BigDecimal.ZERO : iva; }
    public void setIva(BigDecimal iva) {
        if (iva == null) {
            this.iva = BigDecimal.ZERO;
        } else {
            this.iva = iva;
        }
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", cantidad=" + cantidad +
                ", precio=" + precio +
                ", activo=" + activo +
                ", unidadMedida='" + unidadMedida + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", categoria=" + (categoria != null ? categoria.getId() : null) +
                ", iva=" + iva +
                '}';
    }
}