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

The application includes a secure password reset flow that allows users to recover their accounts via email with support for both permanent and expirable tokens.

### How it Works

1. **Request Reset:** User enters their email on the login page
2. **Token Generation:** System generates a secure UUID token that can be configured as permanent (never expires) or expirable
3. **Email Delivery:** User receives an email with a password reset link containing the token
4. **Password Update:** User clicks the link, enters a new password meeting security requirements, and the token is marked as used
5. **One-time Use:** Tokens can only be used once, even if permanent

### Configuration

#### Password Reset Settings

```properties
# Permanent tokens (no expiration) by default
# Set to false to enable token expiration
security.password.reset.permanent=true

# Token expiration time in minutes (only used if permanent=false, default: 60)
security.password.reset.token.expiration-minutes=60

# Minimum password length (default: 8 characters)
security.password.reset.min-password-length=8

# Application base URL for email links (REQUIRED)
# Development: http://localhost:8080
# Production: https://yourdomain.com
app.base.url=https://yourdomain.com
```

#### Email Configuration

The password reset feature requires email to be configured. The application supports two methods:

**Option A: SendGrid API (Recommended for production)**
```properties
sendgrid.api.key=SG.your_api_key_here
sendgrid.sender.email=noreply@yourdomain.com
```

**Option B: SMTP (for development or alternative providers)**
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

> **Note:** Railway blocks SMTP ports (25, 465, 587). Use SendGrid API for Railway deployments.

### Security Features

- **Token Configuration:** Support for both permanent (never expires) and expirable tokens
- **Single Use:** Each token can only be used once to reset a password, even if permanent
- **Password Strength:** Passwords must:
  - Be at least 8 characters long (configurable)
  - Contain at least one letter
  - Contain at least one number
- **Token Validation:** All tokens are validated for existence, expiration (if applicable), and usage status
- **Clean URLs:** Uses UUIDs in URLs instead of exposing user information
- **Token Revocation:** Ability to revoke all tokens for a specific email address

### ⚠️ Security Warning: Permanent Tokens

**Important:** By default, password reset tokens are configured as **permanent** (never expire). While this provides convenience, it comes with security trade-offs:

**Risks:**
- If a reset email is intercepted, the token remains valid indefinitely until used
- Tokens in email archives or forwarded messages remain exploitable
- No automatic cleanup of unused tokens

**Recommendations for Production:**
- Consider setting `PASSWORD_RESET_PERMANENT=false` to enable expiration
- Set a reasonable expiration time (e.g., `PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES=30`)
- Implement periodic cleanup of old unused tokens
- Monitor and alert on password reset activity
- Consider implementing rate limiting on password reset requests

**Current Configuration:**
```properties
# Default: Permanent tokens (security.password.reset.permanent=true)
# For better security, set to false and configure expiration time
PASSWORD_RESET_PERMANENT=false
PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES=30  # 30 minutes recommended
```

The permanent token feature was implemented by specific request for this deployment. For production environments, we strongly recommend enabling token expiration.

### User Experience

1. User clicks "¿Olvidaste tu contraseña?" on the login page
2. Enters their registered email address
3. Receives an email with a secure link (permanent by default, or configurable expiration)
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

The password reset feature uses the `password_reset_token` table:

```sql
CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiry_date DATETIME NULL,  -- NULL for permanent tokens
    used BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_token (token),
    INDEX idx_email (email)
);
```

---

## File Uploads Configuration

The application supports uploading product images with configurable storage location.

### Configuration

```properties
# Directory for uploaded files (configurable for production)
app.uploads-dir=uploads  # Default: ./uploads
# For production: /app/uploads or cloud storage path

# Multipart file upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=20MB
```

### Features

- **Automatic Directory Creation:** Upload directory is created on application startup if it doesn't exist
- **Configurable Location:** Use `app.uploads-dir` to set custom upload directory
- **File Validation:** Supports standard image formats with size limits
- **URL Mapping:** Files are accessible via `/uploads/**` URL pattern

### Important Notes for Railway Deployment

⚠️ **Railway filesystem is ephemeral** - files uploaded to local filesystem are lost when the container restarts or redeploys.

For persistent file storage on Railway, consider:
1. Using AWS S3 or similar cloud storage (recommended)
2. Using Railway volumes (limited availability)
3. Accepting that product images will be lost on redeployment

---

## Buyer Information in Sales

The POS system now supports optional buyer information (name and ID) for each sale.

### Features

- **Optional Fields:** Cashier can optionally enter buyer's name and ID (cédula) during sale
- **Receipt Integration:** Buyer information is displayed on the PDF receipt when provided
- **Database Storage:** Buyer data is persisted with each sale for future reference

### Database Schema

Added fields to the `venta` table:

```sql
ALTER TABLE venta 
  ADD COLUMN comprador_cedula VARCHAR(50) NULL,
  ADD COLUMN comprador_nombre VARCHAR(255) NULL;
```

### Usage

1. When creating a sale in the POS interface, cashier can fill in:
   - **Nombre del comprador** (Buyer's name) - Optional
   - **Cédula** (ID number) - Optional
2. Information appears on the generated receipt PDF
3. Both fields are optional - sales can be completed without buyer information

---

## Railway Deployment

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
   - **Database Configuration** - Map Railway's MySQL `DATABASE_URL` to Spring properties:
     - Copy the values from Railway's MySQL plugin environment variables
     - Set `SPRING_DATASOURCE_URL` to the JDBC URL (format: `jdbc:mysql://host:port/database?useSSL=false&serverTimezone=UTC`)
     - Set `SPRING_DATASOURCE_USERNAME` to MySQL user
     - Set `SPRING_DATASOURCE_PASSWORD` to MySQL password
   - **Email Configuration (REQUIRED for password reset):**
     - Railway blocks SMTP ports (25, 465, 587), so you MUST use SendGrid API
     - Sign up for free at [SendGrid](https://sendgrid.com) (100 emails/day free tier)
     - Create an API key with "Mail Send" permission
     - Verify your sender email address at SendGrid
     - Set these environment variables in Railway:
       - `SENDGRID_API_KEY=SG.your_sendgrid_api_key_here`
       - `SENDER_EMAIL=your-verified-email@domain.com`
     - See [RAILWAY_SETUP.md](docs/RAILWAY_SETUP.md) for detailed SendGrid configuration
   - **Application Base URL (REQUIRED for password reset emails):**
     - `APP_BASE_URL=https://alemandan-crmjava-production.up.railway.app`
     - This is used to generate password reset links in emails
     - Replace with your actual Railway public URL if different
   - **Password Reset Configuration (Optional):**
     - `PASSWORD_RESET_PERMANENT=true` (default: tokens never expire)
     - `PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES=60` (only used if PERMANENT=false)
     - `PASSWORD_RESET_MIN_PASSWORD_LENGTH=8` (minimum password length)
   - **File Upload Configuration:**
     - `APP_UPLOADS_DIR=/app/uploads` (for temporary file storage)
     - Note: Files will be lost on redeployment. For persistent storage, configure AWS S3
   - **Other Optional Variables:**
     - `JWT_SECRET` (if using JWT authentication)
     - AWS S3 credentials (for persistent file storage - see `.env.template`)
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

#### General Issues
- **Build fails:** Check Maven logs in Railway dashboard. Ensure Java 17 is available.
- **App crashes on startup:** Verify database connection and all required environment variables are set
- **Port binding errors:** Ensure `server.port=${PORT:8080}` is in `application.properties`
- **Database connection fails:** Double-check JDBC URL format and credentials
- **File uploads disappear:** Migrate to S3 for persistent storage

#### Password Reset Email Issues
- **Emails not being sent:**
  - ✓ Check that `SENDGRID_API_KEY` and `SENDER_EMAIL` are set in Railway environment variables
  - ✓ Verify your sender email is verified in SendGrid dashboard
  - ✓ Check Railway logs for email sending errors: `railway logs`
  - ✓ Do NOT use SMTP configuration on Railway - ports 25, 465, 587 are blocked
  - ✓ Test SendGrid connectivity using the `/internal/test-mail` endpoint (see docs/RAILWAY_SETUP.md)

- **Password reset links don't work:**
  - ✓ Verify `APP_BASE_URL` is set to your actual Railway URL (e.g., `https://alemandan-crmjava-production.up.railway.app`)
  - ✓ Check that the URL in the email matches your Railway public URL
  - ✓ Ensure the static file `/password-reset.html` is accessible at your domain
  - ✓ Check browser console for JavaScript errors on the password reset page

#### PDF/Excel Export Issues  
- **Exports fail with HTTP 500:**
  - ✓ Check Railway logs for specific error messages: `railway logs | grep -i "export\|pdf\|excel"`
  - ✓ Verify sufficient memory is allocated to your Railway service (upgrade plan if needed)
  - ✓ Ensure the database connection is stable (exports query the database)
  - ✓ Test with a small date range first to rule out data volume issues
  - ✓ Check for missing fonts or graphics libraries (iText PDF dependencies should be in pom.xml)

- **PDF works but Excel fails:**
  - ✓ Verify Apache POI dependencies are included in pom.xml
  - ✓ Check for OutOfMemoryError in logs (Excel files can be memory-intensive)
  - ✓ Try exporting a smaller dataset first

#### Production Debugging
- **View real-time logs:** `railway logs --follow` or use Railway Dashboard → Deployments → View Logs
- **Check environment variables:** Railway Dashboard → Variables tab
- **Test endpoints:** Use the Railway public URL + endpoint path
- **Database issues:** Use Railway Dashboard → MySQL tab → Connect to verify database is running

### Additional Resources

- [Railway Documentation](https://docs.railway.app/)
- [Railway CLI Reference](https://docs.railway.app/develop/cli)
- [Spring Boot on Railway](https://docs.railway.app/guides/spring-boot)

---

## Créditos y agradecimientos

- Proyecto académico para demostración de desarrollo web con Java Spring Boot

---

