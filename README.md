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

> **Nota:** El nombre del repositorio en GitHub mantiene "CRMJava" por razones históricas, pero el proyecto ahora es un sistema POS.

### 2. Configurar la base de datos

Edita el archivo de configuración de Spring Boot para tu base de datos (por ejemplo, `src/main/resources/application.properties`).  
Ejemplo para MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/alemandan_pos
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_CONTRASEÑA
spring.jpa.hibernate.ddl-auto=update
```

> **Nota:** El nombre de la base de datos puede ser cualquiera de tu elección (ej: `alemandan_pos`, `crm_db`, etc.). Asegúrate de actualizar el archivo `application.properties` con el nombre correcto de tu base de datos.

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

## Deploy to Render

This application is ready to be deployed on [Render.com](https://render.com) using Docker or a JAR file.

### Quick Deploy Steps

1. **Connect your repository to Render:**
   - Sign up or log in to [Render.com](https://render.com)
   - Click "New +" and select "Web Service"
   - Connect your GitHub account and select this repository

2. **Configure the Web Service:**
   - **Name:** alemandan-pos (or your preferred name)
   - **Environment:** Docker
   - **Dockerfile Path:** `./Dockerfile` (auto-detected)
   - **Build Command:** (leave empty - Docker handles it)
   - **Start Command:** (leave empty - Dockerfile ENTRYPOINT handles it)

3. **Set Environment Variables:**
   Required environment variables (add in Render Dashboard):
   ```
   SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/crm_db?useSSL=false&serverTimezone=UTC
   SPRING_DATASOURCE_USERNAME=your-db-username
   SPRING_DATASOURCE_PASSWORD=your-db-password
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect
   ```
   
   Optional (for email features):
   ```
   SPRING_MAIL_HOST=smtp.gmail.com
   SPRING_MAIL_PORT=587
   SPRING_MAIL_USERNAME=your-email@gmail.com
   SPRING_MAIL_PASSWORD=your-app-password
   ```

4. **Deploy:**
   - Click "Create Web Service"
   - Render will automatically build and deploy your application
   - Your app will be available at `https://your-service-name.onrender.com`

### Using render.yaml (Infrastructure as Code)

This repository includes a `render.yaml` file for easy deployment:

1. In Render Dashboard, go to "Blueprint" > "New Blueprint Instance"
2. Connect your repository
3. Render will read `render.yaml` and set up the service automatically
4. Configure the required environment variables in the Render Dashboard

### Local Docker Testing

Test the Docker build locally before deploying:

```bash
# Build the Docker image
docker build -t alemandan-pos .

# Run the container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/crm_db?useSSL=false&serverTimezone=UTC \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=yourpassword \
  alemandan-pos

# Access the app at http://localhost:8080
```

### Notes

- The application uses **port 8080** by default but respects the `PORT` environment variable (Render auto-provides this)
- Database: Currently configured for MySQL. You can use an external MySQL database or switch to PostgreSQL
- File uploads: Consider using cloud storage (AWS S3, Cloudinary) for production deployments
- The Dockerfile uses a multi-stage build for optimized image size

---

## Créditos y agradecimientos

- Proyecto académico para demostración de desarrollo web con Java Spring Boot

---

