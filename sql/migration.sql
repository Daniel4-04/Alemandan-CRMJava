-- =============================================================================
-- PRODUCTION DEPLOYMENT MIGRATION SCRIPT
-- =============================================================================
-- Apply this migration BEFORE deploying the new version to production
-- This ensures database schema is compatible with the new code
--
-- Compatible with: MySQL 5.7+, MariaDB 10.2+
-- Database: crm_db (or your configured database name)
-- =============================================================================

USE crm_db; -- Change to your database name if different

-- =============================================================================
-- BACKUP RECOMMENDATION
-- =============================================================================
-- Before running this migration, create a backup:
-- mysqldump -u [user] -p crm_db > backup_before_migration_$(date +%Y%m%d_%H%M%S).sql
-- =============================================================================

-- =============================================================================
-- 1. PASSWORD RESET TOKEN ENHANCEMENTS
-- =============================================================================

-- Check if password_reset_token table exists
SELECT 'Checking password_reset_token table...' AS Step;

-- Make expiryDate nullable to support permanent (non-expiring) tokens
-- This allows tokens to never expire when security.password.reset.permanent=true
ALTER TABLE password_reset_token 
  MODIFY COLUMN expiry_date DATETIME NULL
  COMMENT 'Expiration date for token. NULL = permanent token (never expires)';

-- Add userId column to optionally link token to specific user
-- This is for future enhancements and audit trails
ALTER TABLE password_reset_token 
  ADD COLUMN IF NOT EXISTS user_id BIGINT NULL AFTER email
  COMMENT 'Optional: User ID this token belongs to';

-- Ensure createdAt has proper default timestamp
-- MySQL 5.7+ supports DEFAULT CURRENT_TIMESTAMP for DATETIME columns
-- This prevents '0000-00-00 00:00:00' errors
ALTER TABLE password_reset_token 
  MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
  COMMENT 'Timestamp when token was created';

SELECT 'Password reset token table updated successfully' AS Result;

-- =============================================================================
-- 2. BUYER INFORMATION FOR SALES (VENTA)
-- =============================================================================

-- Check if venta table exists
SELECT 'Checking venta table...' AS Step;

-- Add buyer cedula (ID number) - optional field
ALTER TABLE venta 
  ADD COLUMN IF NOT EXISTS comprador_cedula VARCHAR(50) NULL AFTER metodo_pago
  COMMENT 'Optional: Buyer identification number (cedula)';

-- Add buyer name - optional field
ALTER TABLE venta 
  ADD COLUMN IF NOT EXISTS comprador_nombre VARCHAR(255) NULL AFTER comprador_cedula
  COMMENT 'Optional: Buyer full name';

SELECT 'Venta table updated with buyer information fields' AS Result;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================

-- Verify password_reset_token schema
SELECT 'password_reset_token table structure:' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'password_reset_token'
ORDER BY ORDINAL_POSITION;

-- Verify venta schema (only new columns)
SELECT 'venta table - new columns:' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'venta'
  AND COLUMN_NAME IN ('comprador_cedula', 'comprador_nombre')
ORDER BY ORDINAL_POSITION;

-- =============================================================================
-- MIGRATION COMPLETE
-- =============================================================================
SELECT '✓ Migration completed successfully!' AS Status;
SELECT 'You can now deploy the new application version.' AS NextStep;

-- =============================================================================
-- ROLLBACK SCRIPT (if needed)
-- =============================================================================
-- If you need to rollback these changes, run:
/*
USE crm_db;

-- Rollback password_reset_token changes
ALTER TABLE password_reset_token 
  MODIFY COLUMN expiry_date DATETIME NOT NULL;
ALTER TABLE password_reset_token 
  DROP COLUMN IF EXISTS user_id;

-- Rollback venta changes
ALTER TABLE venta 
  DROP COLUMN IF EXISTS comprador_cedula;
ALTER TABLE venta 
  DROP COLUMN IF EXISTS comprador_nombre;

SELECT 'Rollback completed' AS Status;
*/
