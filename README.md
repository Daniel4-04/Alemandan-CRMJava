# Alemandan POS

## Descripción general

**Alemandan POS** es una aplicación web para gestión comercial y ventas pensada para empresas y equipos de trabajo. Permite administrar empleados, proveedores, productos, ventas y mucho más, todo desde un entorno seguro y moderno.  
El sistema está construido con **Spring Boot**, **Thymeleaf**, **Bootstrap** y utiliza una arquitectura MVC (Modelo-Vista-Controlador) siguiendo buenas prácticas de desarrollo.

### Funcionalidades principales

- **Gestión de empleados:** registro, consulta y administración.
- **Gestión de productos y proveedores:** alta, baja, modificación y consulta.
- **Registro y control de ventas:** cada empleado puede registrar sus ventas y ver el historial.
- **Dashboard para administrador y empleados:** estadísticas en tiempo real, resumen de ventas, métricas clave.
- **Web Service REST:** expone un endpoint `/api/ventas/resumen` que entrega un resumen global de ventas en formato JSON, útil para integraciones externas o reportes.

---

## Descripción del proyecto

Este proyecto académico busca demostrar el desarrollo de un sistema POS web robusto usando tecnologías Java y Spring Boot.  
Incluye:

- Autenticación y autorización por roles (admin y empleado)
- Seguridad con Spring Security
- Uso de Thymeleaf para vistas dinámicas
- Consumo y exposición de Web Services REST
- Persistencia con JPA/Hibernate y base de datos relacional

---

## ¿Cómo clonar y ejecutar el proyecto?

### 1. Clonar el repositorio

```bash
git clone https://github.com/Daniel4-04/Alemandan-CRMJava.git
cd Alemandan-CRMJava
```

### 2. Configurar la base de datos

Edita el archivo de configuración de Spring Boot para tu base de datos (por ejemplo, `src/main/resources/application.properties`).  
Ejemplo para MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/alemandan_pos
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_CONTRASEÑA
spring.jpa.hibernate.ddl-auto=update
```

> **Asegúrate de tener tu base de datos creada y configurada.**

### 3. Compilar y ejecutar

Si usas **Maven**:

```bash
mvn clean install
mvn spring-boot:run
```

El sistema arrancará en [http://localhost:8080/](http://localhost:8080/)

---

## 4. Acceso y pruebas

- Accede a la app en tu navegador: [http://localhost:8080/](http://localhost:8080/)
- Puedes probar el Web Service de resumen de ventas en: [http://localhost:8080/api/ventas/resumen](http://localhost:8080/api/ventas/resumen)
- Los dashboards y módulos requieren autenticación (usuarios de prueba puedes crearlos en la app o poblar la BD manualmente).

---

## 5. Estructura básica de carpetas

- `/src/main/java/com/alemandan/crm/controller` → Controladores MVC y REST
- `/src/main/java/com/alemandan/crm/service` → Servicios de negocio
- `/src/main/java/com/alemandan/crm/model` → Entidades JPA
- `/src/main/resources/templates` → Vistas Thymeleaf
- `/src/main/resources/static` → Recursos estáticos (CSS, JS, imágenes)

---

## Créditos y agradecimientos

- Proyecto académico para demostración de desarrollo web con Java Spring Boot

---

