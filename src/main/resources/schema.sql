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

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role
        CHECK (role IN ('ROLE_ADMIN', 'ROLE_MARKETER')),

    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- Milestone 5: Audience & Campaign Domain Schema

CREATE TABLE IF NOT EXISTS segments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    rules JSON NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_segments_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_seg_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS campaigns (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    segment_id BIGINT UNSIGNED NOT NULL,
    message_template TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    personalization_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ai_summary TEXT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT chk_campaigns_status CHECK (status IN ('DRAFT', 'RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_campaigns_segment FOREIGN KEY (segment_id) REFERENCES segments(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_campaigns_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_camp_status (status),
    INDEX idx_camp_segment_id (segment_id),
    INDEX idx_camp_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Milestone 7: Bulk Ingestion Domain Schema

CREATE TABLE IF NOT EXISTS upload_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    total_rows INT UNSIGNED NOT NULL DEFAULT 0,
    success_count INT UNSIGNED NOT NULL DEFAULT 0,
    failure_count INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    error_details JSON NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_upload_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_upload_file_type CHECK (file_type IN ('CSV', 'XLSX')),
    CONSTRAINT chk_upload_status CHECK (status IN ('SUCCESS', 'PARTIAL_SUCCESS', 'FAILED')),
    INDEX idx_upload_user (uploaded_by),
    INDEX idx_upload_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Milestone 8 & 9: Campaign Delivery Records Schema

CREATE TABLE IF NOT EXISTS campaign_delivery_records (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500) NULL,
    processed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_deliv_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_deliv_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_campaign_customer UNIQUE (campaign_id, customer_id),
    CONSTRAINT chk_deliv_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    INDEX idx_deliv_cust_id (customer_id),
    INDEX idx_deliv_camp_status (campaign_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Milestone 10: AI Auditing Domain Schema

CREATE TABLE IF NOT EXISTS ai_segment_audits (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    prompt_text TEXT NOT NULL,
    generated_rules JSON NOT NULL,
    action_taken VARCHAR(20) NOT NULL,
    segment_id BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_ai_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_audit_segment FOREIGN KEY (segment_id) REFERENCES segments(id) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT chk_ai_audit_action CHECK (action_taken IN ('SAVED', 'DISCARDED')),
    INDEX idx_ai_audit_user (user_id),
    INDEX idx_ai_audit_segment (segment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


