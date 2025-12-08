-- Migration script for password reset token and buyer information features
-- This script is compatible with MySQL 5.7+ and MariaDB 10.2+
-- Apply manually before deploying the new version

-- =============================================================================
-- 1. Modify password_reset_token table to support permanent tokens
-- =============================================================================

-- Make expiryDate nullable to support permanent tokens
ALTER TABLE password_reset_token 
  MODIFY COLUMN expiry_date DATETIME NULL;

-- Add userId column (optional, can be null)
ALTER TABLE password_reset_token 
  ADD COLUMN user_id BIGINT NULL AFTER email;

-- Ensure createdAt has proper default (if not already set)
-- Note: MySQL 5.7+ supports DEFAULT CURRENT_TIMESTAMP for DATETIME
ALTER TABLE password_reset_token 
  MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- =============================================================================
-- 2. Add buyer information columns to venta table
-- =============================================================================

-- Add columns for buyer cedula and nombre (optional fields)
ALTER TABLE venta 
  ADD COLUMN comprador_cedula VARCHAR(50) NULL AFTER metodo_pago,
  ADD COLUMN comprador_nombre VARCHAR(255) NULL AFTER comprador_cedula;

-- =============================================================================
-- Migration complete
-- =============================================================================

-- Verify changes
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('password_reset_token', 'venta')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
