package com.alemandan.crm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application startup component that ensures required directories exist.
 * Creates the uploads directory if it doesn't exist on application startup.
 */
@Component
public class ApplicationStartup implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartup.class);

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    @Override
    public void run(ApplicationArguments args) {
        ensureUploadDirectoryExists();
    }

    /**
     * Creates the uploads directory if it doesn't exist.
     * Also creates subdirectories for different upload types.
     */
    private void ensureUploadDirectoryExists() {
        try {
            Path uploadsPath = Paths.get(uploadsDir);
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
                logger.info("Created uploads directory: {}", uploadsPath.toAbsolutePath());
            } else {
                logger.info("Uploads directory exists: {}", uploadsPath.toAbsolutePath());
            }

            // Create subdirectory for products
            Path productsPath = uploadsPath.resolve("products");
            if (!Files.exists(productsPath)) {
                Files.createDirectories(productsPath);
                logger.info("Created products uploads directory: {}", productsPath.toAbsolutePath());
            }

        } catch (Exception e) {
            logger.error("Failed to create uploads directory: {}", uploadsDir, e);
            // Don't throw exception - let app start, but uploads may fail
        }
    }
}
