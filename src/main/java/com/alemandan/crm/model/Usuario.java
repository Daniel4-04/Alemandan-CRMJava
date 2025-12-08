package com.alemandan.crm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String rol; // "ADMIN" o "EMPLEADO"

    /**
     * Mapeamos explícitamente a TINYINT(1) para evitar problemas con BIT(1) y JDBC/Hibernate.
     * Esto hace que Hibernate genere la columna como TINYINT(1) cuando uses ddl-auto=create/update,
     * y evita que el driver JDBC devuelva byte[] en vez de Boolean.
     */
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activo = (Boolean) true;

    /**
     * Ruta pública relativa a la imagen de perfil (ej: /uploads/users/user_1_12345.jpg).
     */
    @Column(name = "image_path")
    private String imagePath;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}