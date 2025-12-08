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

## Password Reset Feature

The application includes a secure password reset flow that allows users to recover their accounts via email.

### How it Works

1. **Request Reset:** User enters their email on the login page
2. **Token Generation:** System generates a secure UUID token with configurable expiration (default: 60 minutes)
3. **Email Delivery:** User receives an email with a password reset link
4. **Password Update:** User clicks the link, enters a new password, and the token is marked as used

### Configuration

#### Required Environment Variables

Set these in your environment or `.env` file (see `.env.template` for examples):

```properties
# Application base URL for email links
# Development: http://localhost:8080
# Production: https://yourdomain.com
APP_BASE_URL=https://yourdomain.com

# Token expiration time in minutes (default: 60)
PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES=60

# Minimum password length (default: 8 characters)
PASSWORD_RESET_MIN_PASSWORD_LENGTH=8
```

#### Email Configuration

The password reset feature requires email to be configured. The application supports two methods:

**Option A: SendGrid API (Recommended for production)**
```properties
SENDGRID_API_KEY=SG.your_api_key_here
SENDER_EMAIL=noreply@yourdomain.com
```

**Option B: SMTP (for development or alternative providers)**
```properties
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_app_password
```

> **Note:** Railway blocks SMTP ports (25, 465, 587). Use SendGrid API for Railway deployments.

### Security Features

- **Token Expiration:** Tokens automatically expire after the configured time (default: 60 minutes)
- **Single Use:** Each token can only be used once to reset a password
- **Password Strength:** Passwords must:
  - Be at least 8 characters long (configurable)
  - Contain at least one letter
  - Contain at least one number
- **Token Validation:** All tokens are validated for existence, expiration, and usage status
- **Clean URLs:** Uses UUIDs in URLs instead of exposing user information

### User Experience

1. User clicks "¿Olvidaste tu contraseña?" on the login page
2. Enters their registered email address
3. Receives an email with a secure link (valid for 60 minutes by default)
4. Clicks the link and is taken to a password reset form
5. Enters and confirms their new password
6. Password is updated and they can log in immediately

### Error Messages

The system provides clear, user-friendly error messages:
- "No existe usuario con ese correo" - Email not found
- "El enlace es inválido o ha expirado" - Token expired or invalid
- "Este enlace ya fue utilizado" - Token already used
- "Las contraseñas no coinciden" - Password mismatch
- "La contraseña debe tener al menos X caracteres" - Password too short
- "La contraseña debe contener al menos una letra" - Missing letters
- "La contraseña debe contener al menos un número" - Missing numbers

### Database Schema

The password reset feature uses the `PasswordResetToken` table:

```sql
CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    expiry_date DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);
```

### Railway Deployment

When deploying to Railway, ensure you set the `APP_BASE_URL` environment variable:

1. Go to your Railway project
2. Navigate to Variables
3. Add: `APP_BASE_URL=https://your-railway-app.up.railway.app`
4. Also configure email settings (use SendGrid for Railway since SMTP is blocked)

---

## Deploy to Railway

Railway is a cloud platform that makes it easy to deploy Spring Boot applications with minimal configuration. This project includes all necessary files for Railway deployment.

### Prerequisites

1. A [Railway account](https://railway.app/) (free tier available)
2. Your GitHub repository connected to Railway
3. Railway CLI installed (optional): `npm i -g @railway/cli`

### Quick Deployment Steps

#### Option 1: Using Railway Dashboard (Recommended for beginners)

1. **Connect Repository:**
   - Go to [Railway Dashboard](https://railway.app/dashboard)
   - Click "New Project" → "Deploy from GitHub repo"
   - Select the `Daniel4-04/Alemandan-CRMJava` repository

2. **Add MySQL Database:**
   - In your Railway project, click "New" → "Database" → "Add MySQL"
   - Railway will automatically create a `DATABASE_URL` environment variable
   - Note: Railway's filesystem is **ephemeral** (temporary). For persistent file uploads, configure AWS S3 (see `.env.template`).

3. **Configure Build & Start Commands:**
   - Go to your service settings → "Settings" tab
   - Set **Build Command**: `mvn clean package -DskipTests`
   - Set **Start Command**: `java -Dserver.port=$PORT -jar target/*.jar`
   - Railway will automatically set the `PORT` environment variable

4. **Set Environment Variables:**
   - Go to "Variables" tab in your service
   - Map Railway's MySQL `DATABASE_URL` to Spring properties:
     - Copy the values from Railway's MySQL plugin environment variables
     - Set `SPRING_DATASOURCE_URL` to the JDBC URL (format: `jdbc:mysql://host:port/database?useSSL=false&serverTimezone=UTC`)
     - Set `SPRING_DATASOURCE_USERNAME` to MySQL user
     - Set `SPRING_DATASOURCE_PASSWORD` to MySQL password
   - Add other required variables from `.env.template`:
     - `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` (for email functionality)
     - `JWT_SECRET` (if using JWT authentication)
     - AWS S3 credentials (for persistent file storage)
   - **Important:** Ensure `server.port=${PORT:8080}` is set in `src/main/resources/application.properties`

5. **Deploy:**
   - Railway will automatically deploy when you push to your main branch
   - Or click "Deploy" button in the dashboard to trigger manual deployment

6. **Verify Deployment:**
   - Once deployed, click on your service to get the public URL
   - Test health endpoint: `https://your-app.railway.app/actuator/health`
   - Access your application: `https://your-app.railway.app/`

#### Option 2: Using Railway CLI

1. **Initialize Railway Project:**
   ```bash
   ./deploy-railway.sh init
   ```

2. **Add MySQL in Railway Dashboard:**
   - Go to your project and add the MySQL database plugin

3. **Configure Environment Variables:**
   ```bash
   # Copy template and edit with your values
   cp .env.template .env
   # Edit .env file with your actual credentials
   
   # Set variables in Railway
   ./deploy-railway.sh env
   ```

4. **Deploy:**
   ```bash
   ./deploy-railway.sh deploy
   ```

5. **View Logs:**
   ```bash
   ./deploy-railway.sh logs
   ```

### Important Notes

#### Server Port Configuration
**CRITICAL:** Railway requires your application to bind to the port specified in the `PORT` environment variable. Ensure your `src/main/resources/application.properties` includes:

```properties
server.port=${PORT:8080}
```

If this line is missing, add it to your `application.properties` file before deploying.

#### Database Configuration
- Railway provides managed MySQL databases with automatic backups
- The `DATABASE_URL` from Railway needs to be mapped to Spring's datasource properties
- For phpMyAdmin users: Railway MySQL works similarly to local phpMyAdmin setup
- Railway automatically handles SSL/TLS for database connections

#### File Storage
- Railway's filesystem is **ephemeral** - uploaded files will be lost on each deployment
- For persistent file storage, configure AWS S3:
  - Add S3 credentials to environment variables (see `.env.template`)
  - Update your file upload logic to use S3 instead of local filesystem
  - The `uploads` directory will only work temporarily

#### Environment Variables
- Never commit `.env` files with real secrets to your repository
- Use `.env.template` as a reference for required variables
- Set all sensitive values in Railway's Variables section
- Railway automatically encrypts environment variables

#### Health Checks
- Railway can monitor your app using health endpoints
- Spring Boot Actuator provides `/actuator/health` endpoint
- Configure custom health checks in Railway settings if needed

### Troubleshooting

- **Build fails:** Check Maven logs in Railway dashboard. Ensure Java 17 is available.
- **App crashes on startup:** Verify database connection and all required environment variables are set
- **Port binding errors:** Ensure `server.port=${PORT:8080}` is in `application.properties`
- **Database connection fails:** Double-check JDBC URL format and credentials
- **File uploads disappear:** Migrate to S3 for persistent storage

### Additional Resources

- [Railway Documentation](https://docs.railway.app/)
- [Railway CLI Reference](https://docs.railway.app/develop/cli)
- [Spring Boot on Railway](https://docs.railway.app/guides/spring-boot)

---

## Créditos y agradecimientos

- Proyecto académico para demostración de desarrollo web con Java Spring Boot

---

