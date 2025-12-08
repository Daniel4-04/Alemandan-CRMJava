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

### Mail Configuration (SMTP)

Configure SMTP settings for email notifications:

```
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

**Important Notes:**

1. **Railway and PaaS SMTP Restrictions**: Many Platform-as-a-Service providers (including Railway) block outbound connections to common SMTP ports (25, 465, 587) to prevent spam. If you experience SMTP timeouts:
   
   - **Option A**: Use a transactional email API provider instead of SMTP:
     - SendGrid (https://sendgrid.com)
     - Mailgun (https://www.mailgun.com)
     - Amazon SES (https://aws.amazon.com/ses)
     - Postmark (https://postmarkapp.com)
   
   - **Option B**: Contact Railway support to whitelist SMTP access for your project
   
   - **Option C**: Configure the app to run without email (emails will be logged but not sent)

2. **Gmail App Passwords**: If using Gmail, you must use an App Password, not your regular password:
   - Go to Google Account settings → Security → 2-Step Verification → App passwords
   - Generate a new app password for "Mail"
   - Use this 16-character password as `SPRING_MAIL_PASSWORD`

3. **Async Email Handling**: The application sends emails asynchronously after database commits. SMTP failures are logged but do not crash the application or return 500 errors to users.

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
