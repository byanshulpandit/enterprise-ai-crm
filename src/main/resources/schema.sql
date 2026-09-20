-- Enterprise AI-CRM Platform (CS-CRM-2026)
-- Milestone 2: Customer Domain Schema

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NULL,
    city VARCHAR(100) NULL,
    country VARCHAR(100) NULL,
    total_spend DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    visit_count INT UNSIGNED NOT NULL DEFAULT 0,
    last_active_date DATE NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL DEFAULT NULL,
    CONSTRAINT uq_customers_email UNIQUE (email),
    INDEX idx_cust_del_spent (deleted_at, total_spend),
    INDEX idx_cust_del_city (deleted_at, city),
    INDEX idx_cust_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_tags (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT UNSIGNED NOT NULL,
    tag VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_cust_tags_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT uq_customer_tag UNIQUE (customer_id, tag),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
