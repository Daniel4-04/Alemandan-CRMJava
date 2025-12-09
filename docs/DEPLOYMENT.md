# Deployment Guide

This document provides deployment instructions for various environments, with a focus on Railway PaaS.

## Table of Contents
- [Railway Deployment](#railway-deployment)
- [Docker Deployment](#docker-deployment)
- [Traditional Server Deployment](#traditional-server-deployment)
- [Common Issues and Solutions](#common-issues-and-solutions)

---

## Railway Deployment

Railway is a Platform as a Service (PaaS) that simplifies deployment. However, it has specific limitations you need to be aware of.

### Prerequisites

1. Railway account (free tier available)
2. MySQL database service in Railway
3. SendGrid account for emails (recommended)
4. This repository connected to Railway

### Environment Variables

Configure these in Railway's environment variables settings:

#### Required Variables

```bash
# Database Configuration (from Railway MySQL service)
SPRING_DATASOURCE_URL=jdbc:mysql://host:port/database?useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_mysql_username
SPRING_DATASOURCE_PASSWORD=your_mysql_password

# Application URL (use your Railway deployment URL)
APP_BASE_URL=https://your-app-name.up.railway.app

# Email Configuration (Option 1: SendGrid - RECOMMENDED)
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxx
SENDER_EMAIL=your-verified-email@domain.com
```

#### Optional Variables

```bash
# Memory Configuration (recommended for Railway)
JAVA_TOOL_OPTIONS=-Xmx512m -Xms256m

# Production Database Settings
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

# Password Reset Settings
PASSWORD_RESET_PERMANENT=false
PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES=30

# File Upload Directory
APP_UPLOADS_DIR=/app/uploads
```

### Railway-Specific Limitations

#### 1. Native Libraries for Chart Generation

**Problem:** Railway containers don't include `libfreetype.so.6` and other native libraries required for JFreeChart to render images.

**Solution Implemented:**
- Application runs in **headless mode** (`java.awt.headless=true` set in main method)
- Chart generation has **fallback logic**: if native libraries fail, charts are skipped but all tabular data is preserved
- PDF/Excel exports continue to work without visual charts

**Technical Details:**
```java
// In AlemandanCrmJavaApplication.java
System.setProperty("java.awt.headless", "true");

// In ReportService.java - chart generation with fallback
try {
    // Generate chart
} catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
    logger.warn("Chart skipped - native libraries not available");
    // Continue without chart
}
```

**If you need charts in production:**
- Deploy to a platform with full OS libraries (AWS EC2, DigitalOcean Droplet, etc.)
- Use Docker with a base image that includes libfreetype (`openjdk:21-jdk` includes it)
- Install native libraries in Railway (contact support for custom buildpacks)

#### 2. SMTP Port Blocking

**Problem:** Railway blocks outbound connections on ports 25, 465, and 587 to prevent spam.

**Solution Implemented:**
- **SendGrid HTTP API integration** (bypasses SMTP entirely)
- Automatic fallback from SendGrid → SMTP → fail gracefully
- All email failures are logged but don't crash the application

**To Enable Email Sending:**

1. **Create SendGrid Account** (100 free emails/day):
   - Visit https://sendgrid.com/pricing/ (free tier)
   - Verify your email address

2. **Create API Key**:
   - Go to Settings → API Keys → Create API Key
   - Choose "Full Access" or "Mail Send" permissions
   - Copy the key (shown only once!)

3. **Verify Sender Identity**:
   - Go to Settings → Sender Authentication
   - Use "Single Sender Verification" (easier)
   - Verify the email address you'll send from
   - **Important:** Use this verified email as `SENDER_EMAIL`

4. **Configure Railway**:
   ```bash
   SENDGRID_API_KEY=SG.your_api_key_here
   SENDER_EMAIL=your-verified-email@domain.com
   ```

**Email Behavior:**
- With SendGrid configured: Emails sent via HTTP API ✅
- SendGrid fails + SMTP configured: Falls back to SMTP (may fail on Railway) ⚠️
- Both fail: Logs error, user sees friendly message, app continues ✅

#### 3. MySQL Strict Mode and DDL Issues

**Problem:** Railway MySQL runs in strict mode. Hibernate's automatic DDL (`ddl-auto=update`) may fail when trying to create tables with default timestamp values like `'0000-00-00 00:00:00'`.

**Solution:**

For production, use `ddl-auto=validate` instead of `update`:

```properties
# In application-prod.properties
spring.jpa.hibernate.ddl-auto=validate
```

**Database Migration Strategy:**
1. **Initial Setup:** Run with `ddl-auto=update` locally to generate schema
2. **Export Schema:** `mysqldump -u root -p --no-data crm_db > schema.sql`
3. **Import to Railway:** Run schema.sql against Railway MySQL
4. **Production:** Use `ddl-auto=validate` (never auto-modify production schema)

**Alternative:** Use Flyway or Liquibase for versioned migrations.

#### 4. Ephemeral Filesystem

**Problem:** Railway's filesystem is ephemeral - uploaded files are lost on redeploy.

**Current Setup:** Files stored in `/app/uploads` (lost on redeploy)

**For Production:** Use cloud storage:
- AWS S3
- Cloudinary
- Backblaze B2
- Railway Volumes (persistent storage)

Configuration for AWS S3 is in `.env.template` - uncomment and configure to enable.

### Deployment Steps

1. **Create Railway Project**
   ```bash
   railway login
   railway init
   railway link
   ```

2. **Add MySQL Service**
   - In Railway dashboard: New → Database → MySQL
   - Copy connection details

3. **Configure Environment Variables**
   - Go to your service → Variables
   - Add all required variables listed above
   - Extract host/port/database from Railway's `DATABASE_URL`

4. **Deploy**
   ```bash
   git push origin main
   ```
   - Railway auto-detects Maven and uses `Procfile`
   - Build takes ~2-3 minutes
   - Check logs: `railway logs`

5. **Verify Deployment**
   - Check startup logs for email configuration warnings
   - Test login functionality
   - Try PDF export (should work without charts)
   - Test password reset email sending

### Monitoring and Logs

```bash
# View real-time logs
railway logs

# Filter for specific issues
railway logs | grep -i "error\|warn"

# Check email configuration
railway logs | grep -i "email\|sendgrid\|smtp"

# Check chart generation
railway logs | grep -i "chart\|native\|libfreetype"
```

---

## Docker Deployment

For environments where you have more control, Docker provides full library support.

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk

# Install native libraries for chart generation
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run application in headless mode (good practice)
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
```

### Build and Run

```bash
# Build application
mvn clean package -DskipTests

# Build Docker image
docker build -t alemandan-crm:latest .

# Run with environment variables
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/crm_db \
  -e SPRING_DATASOURCE_USERNAME=user \
  -e SPRING_DATASOURCE_PASSWORD=pass \
  -e SENDGRID_API_KEY=your_key \
  -e SENDER_EMAIL=your@email.com \
  -e APP_BASE_URL=https://your-domain.com \
  alemandan-crm:latest
```

**Advantages:**
- ✅ Full native library support (charts work perfectly)
- ✅ Consistent environment across dev/staging/prod
- ✅ Easy to deploy to AWS ECS, DigitalOcean, or any cloud

---

## Traditional Server Deployment

For deployment on a dedicated server (VPS, EC2, etc.):

### Prerequisites

```bash
# Install Java 21
sudo apt update
sudo apt install openjdk-21-jdk

# Install MySQL
sudo apt install mysql-server

# Install native libraries (for chart generation)
sudo apt install libfreetype6 fontconfig
```

### Build Application

```bash
# Clone repository
git clone https://github.com/Daniel4-04/Alemandan-CRMJava.git
cd Alemandan-CRMJava

# Build with Maven
./mvnw clean package -DskipTests

# JAR file created at: target/AlemandanPOSJava-*.jar
```

### Run as Service

Create systemd service file: `/etc/systemd/system/alemandan-crm.service`

```ini
[Unit]
Description=Alemandan CRM Java Application
After=network.target mysql.service

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/alemandan-crm
ExecStart=/usr/bin/java -jar /opt/alemandan-crm/app.jar
Restart=always
RestartSec=10

# Environment variables
Environment="SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/crm_db"
Environment="SPRING_DATASOURCE_USERNAME=crm_user"
Environment="SPRING_DATASOURCE_PASSWORD=secure_password"
Environment="APP_BASE_URL=https://your-domain.com"
Environment="SENDGRID_API_KEY=your_key"
Environment="SENDER_EMAIL=noreply@yourdomain.com"

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl enable alemandan-crm
sudo systemctl start alemandan-crm
sudo systemctl status alemandan-crm
```

### Nginx Reverse Proxy

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## Common Issues and Solutions

### Issue: PDF/Excel Exports Return 500 Error

**Symptoms:**
- HTTP 500 when clicking "Exportar PDF" or "Exportar Excel"
- Logs show: `UnsatisfiedLinkError: libfreetype.so.6: cannot open shared object file`

**Cause:** Native libraries not available (common on Railway)

**Solution:**
- ✅ **Already implemented**: Application runs in headless mode with fallback
- Charts are skipped, but all data exports correctly
- For full chart support: Deploy with Docker or install native libraries

**To verify it's working:**
```bash
# Check logs for fallback message
railway logs | grep "Chart skipped\|native libraries not available"
```

### Issue: Password Reset Emails Not Sending

**Symptoms:**
- User requests password reset
- No email received
- Logs show: `Email sent successfully` but inbox empty

**Diagnosis:**
```bash
# Check email configuration
railway logs | grep -i "email service configured"

# Check for SendGrid errors
railway logs | grep -i "sendgrid"

# Check for SMTP errors
railway logs | grep -i "smtp\|mail"
```

**Solutions:**

1. **No email configured:**
   ```
   WARNING: No email service configured!
   ```
   → Configure SendGrid API key and sender email

2. **SendGrid error 401 (Unauthorized):**
   ```
   SendGrid API returned error status 401
   ```
   → Check API key is correct

3. **SendGrid error 403 (Forbidden):**
   ```
   SendGrid API returned error status 403
   ```
   → Verify sender email in SendGrid dashboard

4. **SMTP timeout:**
   ```
   Failed to send email to: user@example.com. SMTP error: Connection timeout
   ```
   → Railway blocks SMTP, use SendGrid instead

### Issue: Database DDL Errors on Startup

**Symptoms:**
- Application fails to start on Railway
- Logs show: `Incorrect datetime value: '0000-00-00 00:00:00'`

**Cause:** MySQL strict mode + Hibernate auto-DDL

**Solution:**
1. Create schema manually (see `sql/` directory)
2. Use `spring.jpa.hibernate.ddl-auto=validate` in production
3. Or disable strict mode in MySQL (not recommended)

### Issue: Application Runs Out of Memory

**Symptoms:**
- Application crashes
- Logs show: `java.lang.OutOfMemoryError: Java heap space`

**Solutions:**

1. **Set memory limits:**
   ```bash
   JAVA_TOOL_OPTIONS=-Xmx512m -Xms256m
   ```

2. **Upgrade Railway plan** (if on Starter: 512MB → Developer: 1GB)

3. **Optimize queries** (reduce date ranges for large exports)

### Issue: Charts Not Appearing in PDFs

**Expected Behavior:** This is normal on Railway (by design)

**Explanation:**
- Railway doesn't include `libfreetype` and other native AWT libraries
- Application detects this and skips chart generation
- All tabular data is still exported correctly
- This is a **graceful degradation**, not a bug

**To get charts in production:**
- Deploy with Docker (includes native libraries)
- Deploy to a VPS with full OS libraries
- Use a Railway custom buildpack (advanced)

---

## Testing the Deployment

### Quick Health Check

```bash
# Test application is running
curl https://your-app.railway.app/login

# Should return HTTP 200 with login page
```

### Test Email Configuration

```bash
# Use internal test endpoint (if available)
curl -X POST "https://your-app.railway.app/internal/test-mail?to=your-email@example.com"

# Check logs for result
railway logs | tail -20
```

### Test PDF Export

1. Login as admin
2. Go to "Ventas" → "Reporte Avanzado"
3. Click "Exportar PDF"
4. Should download PDF with data (charts may be skipped on Railway)

### Verify Password Reset

1. Logout
2. Click "¿Olvidaste tu contraseña?"
3. Enter email
4. Check server logs for password reset link (logged for debugging)
5. Verify email was sent (check inbox)

---

## Security Considerations

### Production Checklist

- [ ] Change default admin password
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (never use `update` in prod)
- [ ] Use strong database password
- [ ] Enable HTTPS (Railway provides this automatically)
- [ ] Set `PASSWORD_RESET_PERMANENT=false` for security
- [ ] Secure server logs (contains password reset tokens for debugging)
- [ ] Configure rate limiting for password reset endpoint
- [ ] Regular database backups (Railway provides automatic backups)

### Environment Variable Security

**Never commit to Git:**
- Database passwords
- SendGrid API keys
- SMTP credentials

**Use Railway's environment variables** for all secrets.

---

## Support and Resources

- **Railway Documentation**: https://docs.railway.app/
- **SendGrid Documentation**: https://docs.sendgrid.com/
- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/
- **GitHub Issues**: https://github.com/Daniel4-04/Alemandan-CRMJava/issues

For Railway-specific issues:
- Railway Discord: https://discord.gg/railway
- Railway Support: support@railway.app

---

## Changelog

### Recent Improvements (This PR)

1. **Chart Generation Fallback**
   - Added headless mode enforcement
   - Implemented graceful fallback when native libraries unavailable
   - Charts skipped but data exports continue

2. **Email Configuration Validation**
   - Startup validation with clear warnings
   - SendGrid API integration with SMTP fallback
   - Comprehensive error logging

3. **Production Configuration**
   - Created application-prod.properties with safe defaults
   - Documented MySQL strict mode issues
   - Memory optimization settings

4. **Documentation**
   - This comprehensive deployment guide
   - Railway-specific troubleshooting
   - Email configuration instructions
