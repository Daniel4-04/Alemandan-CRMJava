# Railway Deployment Setup Guide

This document describes the required environment variables and configuration for deploying AlemandanPOS to Railway.

## Required Environment Variables

### Database Configuration

Railway MySQL service automatically provides `DATABASE_URL`. Map it to Spring Boot format:

```
SPRING_DATASOURCE_URL=jdbc:mysql://host:port/database?useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_mysql_username
SPRING_DATASOURCE_PASSWORD=your_mysql_password
```

**Note:** Extract host, port, database, username, and password from Railway's MySQL `DATABASE_URL` connection string.

### Mail Configuration

#### Option A: SendGrid API (Recommended for Railway)

**Why SendGrid?** Railway and many PaaS providers block SMTP ports (25, 465, 587) to prevent spam. SendGrid uses HTTP API which is not blocked.

1. **Create a SendGrid Account**:
   - Sign up at https://sendgrid.com (free tier: 100 emails/day)
   - Verify your email address

2. **Create an API Key**:
   - Go to Settings → API Keys
   - Click "Create API Key"
   - Name: `Railway-AlemandanPOS`
   - Permissions: "Full Access" or "Mail Send" only
   - Copy the API key (shown only once!)

3. **Verify Sender Identity**:
   - Go to Settings → Sender Authentication
   - Choose "Single Sender Verification" (easier) or "Domain Authentication" (more professional)
   - For single sender: verify the email address you'll send from
   - **Important**: Use the verified email as `SENDER_EMAIL`

4. **Configure Railway Environment Variables**:
   ```
   SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   SENDER_EMAIL=your-verified-email@domain.com
   ```

**How it works:**
- Application automatically uses SendGrid API when `SENDGRID_API_KEY` is set
- If SendGrid fails, falls back to SMTP (if configured)
- If both fail, errors are logged but app continues working

#### Option B: SMTP Configuration (Alternative)

If you prefer SMTP or SendGrid is not available:

```
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

**Important Notes:**

1. **Railway SMTP Restrictions**: Railway may block outbound SMTP connections. If you experience timeouts:
   - Use SendGrid API (Option A) instead
   - Contact Railway support to whitelist SMTP
   - Or run without email (emails logged but not sent)

2. **Gmail App Passwords**: If using Gmail, you must use an App Password:
   - Go to Google Account → Security → 2-Step Verification → App passwords
   - Generate password for "Mail"
   - Use the 16-character password as `SPRING_MAIL_PASSWORD`

3. **Async Email Handling**: Emails are sent asynchronously after database commits. Failures are logged but don't crash the app.

### Memory and Performance Configuration (Optional but Recommended)

Set JVM memory limits to prevent Out-Of-Memory (OOM) errors on Railway:

```
JAVA_TOOL_OPTIONS=-Xmx512m -Xms256m
```

**Alternative (Java 10+):** Use percentage-based memory allocation:

```
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0
```

**Explanation:**
- `-Xmx512m`: Maximum heap size (512 MB)
- `-Xms256m`: Initial heap size (256 MB)
- `-XX:MaxRAMPercentage=75.0`: Use 75% of available container memory
- `-XX:InitialRAMPercentage=50.0`: Start with 50% of available container memory

Choose memory settings based on your Railway plan:
- **Starter Plan** (512 MB RAM): Use `-Xmx400m` or `-XX:MaxRAMPercentage=70.0`
- **Developer Plan** (1 GB RAM): Use `-Xmx768m` or `-XX:MaxRAMPercentage=75.0`
- **Pro Plan** (2+ GB RAM): Use `-Xmx1536m` or `-XX:MaxRAMPercentage=75.0`

### Server Port (Automatically Configured)

Railway automatically provides the `PORT` environment variable. The application is configured to use it via:

```properties
server.port=${PORT:8080}
```

No manual configuration needed.

## Deployment Steps

1. **Create a Railway Project**
   - Connect your GitHub repository to Railway
   - Railway will auto-detect the Java/Maven project

2. **Add MySQL Service**
   - Add a MySQL database service in Railway
   - Copy the connection details to environment variables

3. **Configure Environment Variables**
   - Go to your Railway project settings
   - Add all required environment variables listed above
   - Save changes

4. **Deploy**
   - Railway will automatically build using Maven and the `Procfile`
   - Monitor deployment logs for any errors
   - Check application logs for SMTP connection issues

## Troubleshooting

### SMTP Connection Timeouts

If you see errors like:
```
Couldn't connect to smtp.gmail.com:587
```

**This is expected behavior when:**
- Railway blocks SMTP ports (common)
- SMTP credentials are incorrect
- Gmail blocks sign-in attempts (use App Passwords)

**The application handles this gracefully:**
- Users can still register and be approved
- Emails are logged but not sent
- No 500 errors are returned
- Consider switching to an email API provider (recommended for production)

### Out of Memory Errors

If you see OOM errors in logs:
- Reduce `-Xmx` value in `JAVA_TOOL_OPTIONS`
- Upgrade your Railway plan for more memory
- Enable lazy initialization (already configured in `application.properties`)

### Database Connection Issues

If the app cannot connect to MySQL:
- Verify `SPRING_DATASOURCE_URL` format is correct (must start with `jdbc:mysql://`)
- Check username and password are correctly extracted from Railway's `DATABASE_URL`
- Ensure MySQL service is running in Railway

## Testing the Deployment

After deployment, verify:

1. **Application starts:** Check Railway logs for "Started AlemandanCrmJavaApplication"
2. **Database connection:** Try logging in or viewing the dashboard
3. **Email handling:** Try registration/approval - should complete even if SMTP fails
4. **Error pages:** Navigate to a non-existent route to test error handling

### Testing Email Configuration

You can test the email configuration using the internal test endpoint:

```bash
# Test email sending with curl
curl -X POST "https://your-railway-app.railway.app/internal/test-mail?to=your-email@example.com"

# Or from localhost during development
curl -X POST "http://localhost:8080/internal/test-mail?to=your-email@example.com"
```

**Expected behavior:**
- If `SENDGRID_API_KEY` is configured: Email sends via SendGrid API
- If SendGrid fails or is not configured: Falls back to SMTP (if configured)
- Check Railway logs to see which provider was used and any error messages

**Logs to look for:**
- `Email sent successfully via SendGrid to: user@example.com (status: 202)`
- `SendGrid failed, falling back to SMTP for: user@example.com`
- `Email sent successfully via SMTP to: user@example.com`

## Additional Resources

- [Railway Documentation](https://docs.railway.app/)
- [Spring Boot Email Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
- [Railway MySQL Setup](https://docs.railway.app/databases/mysql)

## Support

For Railway-specific issues, consult:
- Railway Discord: https://discord.gg/railway
- Railway Documentation: https://docs.railway.app/

For application issues, check the GitHub repository issues page.
