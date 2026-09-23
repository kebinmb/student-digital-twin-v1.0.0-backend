-- V16__create_or_booklet_inventory.sql
-- Subsystem: COA Physical Official Receipt (O.R.) Booklet Inventory & PGCA Chart of Accounts Mapping

CREATE TABLE IF NOT EXISTS or_booklets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booklet_code VARCHAR(50) NOT NULL UNIQUE,
    start_or_number VARCHAR(30) NOT NULL,
    end_or_number VARCHAR(30) NOT NULL,
    current_or_number VARCHAR(30) NOT NULL,
    assigned_cashier_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_or_booklet_cashier (assigned_cashier_id),
    CONSTRAINT fk_or_booklet_cashier FOREIGN KEY (assigned_cashier_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS voided_official_receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    or_number VARCHAR(30) NOT NULL UNIQUE,
    booklet_id BIGINT NOT NULL,
    voided_by_cashier_id BIGINT NOT NULL,
    void_reason TEXT NOT NULL,
    voided_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_voided_or (or_number),
    CONSTRAINT fk_void_or_booklet FOREIGN KEY (booklet_id) REFERENCES or_booklets(id) ON DELETE CASCADE,
    CONSTRAINT fk_void_or_cashier FOREIGN KEY (voided_by_cashier_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
