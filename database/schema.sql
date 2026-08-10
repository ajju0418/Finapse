-- =============================================================
-- FINAPSE — Database Schema
-- MySQL 8.0+
-- =============================================================

CREATE DATABASE IF NOT EXISTS finapse CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE finapse;

-- -------------------------------------------------------------
-- USERS
-- -------------------------------------------------------------
CREATE TABLE users (
    id         CHAR(36)     NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- CATEGORIES
-- -------------------------------------------------------------
CREATE TABLE categories (
    id           CHAR(36)     NOT NULL,
    name         VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_name (name)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- MERCHANTS
-- -------------------------------------------------------------
CREATE TABLE merchants (
    id               CHAR(36)     NOT NULL,
    name             VARCHAR(150) NOT NULL,
    normalized_name  VARCHAR(150) NOT NULL,
    category_id      CHAR(36)     NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_merchants_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- ACCOUNTS  (bank accounts)
-- -------------------------------------------------------------
CREATE TABLE accounts (
    id                CHAR(36)     NOT NULL,
    user_id           CHAR(36)     NOT NULL,
    name              VARCHAR(150) NOT NULL,
    institution_name  VARCHAR(150) NULL,
    account_type      ENUM('BANK') NOT NULL DEFAULT 'BANK',
    last_four_digits  VARCHAR(4)   NULL,
    currency          CHAR(3)      NOT NULL DEFAULT 'INR',
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- CARDS  (credit cards)
-- -------------------------------------------------------------
CREATE TABLE cards (
    id                 CHAR(36)       NOT NULL,
    user_id            CHAR(36)       NOT NULL,
    name               VARCHAR(150)   NOT NULL,
    issuer             VARCHAR(100)   NULL,
    last_four_digits   VARCHAR(4)     NULL,
    credit_limit       DECIMAL(15,2)  NULL,
    billing_cycle_day  TINYINT        NULL,
    payment_due_day    TINYINT        NULL,
    is_active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         DATETIME       NOT NULL,
    updated_at         DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- STATEMENTS
-- -------------------------------------------------------------
CREATE TABLE statements (
    id                  CHAR(36)                                                                    NOT NULL,
    user_id             CHAR(36)                                                                    NOT NULL,
    account_id          CHAR(36)                                                                    NULL,
    card_id             CHAR(36)                                                                    NULL,
    statement_type      ENUM('BANK','CREDIT_CARD')                                                  NOT NULL,
    original_file_name  VARCHAR(255)                                                                NOT NULL,
    file_hash           CHAR(64)                                                                    NOT NULL,
    transaction_count   INT                                                                         NOT NULL DEFAULT 0,
    import_status       ENUM('UPLOADED','PROCESSING','REVIEW_REQUIRED','COMPLETED','FAILED','CANCELLED') NOT NULL DEFAULT 'UPLOADED',
    period_start        DATE                                                                        NULL,
    period_end          DATE                                                                        NULL,
    uploaded_at         DATETIME                                                                    NOT NULL,
    processed_at        DATETIME                                                                    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_statements_user    FOREIGN KEY (user_id)    REFERENCES users    (id),
    CONSTRAINT fk_statements_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_statements_card    FOREIGN KEY (card_id)    REFERENCES cards    (id),
    -- A statement belongs to exactly one source
    CONSTRAINT chk_statements_source CHECK (
        (account_id IS NOT NULL AND card_id IS NULL) OR
        (account_id IS NULL     AND card_id IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE INDEX idx_statements_user_id      ON statements (user_id);
CREATE INDEX idx_statements_account_id   ON statements (account_id);
CREATE INDEX idx_statements_card_id      ON statements (card_id);
CREATE INDEX idx_statements_file_hash    ON statements (file_hash);
CREATE INDEX idx_statements_import_status ON statements (import_status);

-- -------------------------------------------------------------
-- TRANSACTIONS
-- -------------------------------------------------------------
CREATE TABLE transactions (
    id                    CHAR(36)      NOT NULL,
    statement_id          CHAR(36)      NOT NULL,
    account_id            CHAR(36)      NULL,
    card_id               CHAR(36)      NULL,
    merchant_id           CHAR(36)      NULL,
    category_id           CHAR(36)      NULL,
    transaction_date      DATE          NOT NULL,
    posted_date           DATE          NULL,
    description           VARCHAR(500)  NOT NULL,
    amount                DECIMAL(15,2) NOT NULL,
    direction             ENUM('DEBIT','CREDIT') NOT NULL,
    transaction_type      ENUM('EXPENSE','INCOME','TRANSFER','CREDIT_CARD_PAYMENT','CASHBACK','REFUND','FEE','INTEREST','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    cashback_amount       DECIMAL(15,2) NULL,
    transaction_hash      CHAR(64)      NULL,
    reconciliation_status ENUM('UNMATCHED','MATCHED','REVIEW_REQUIRED','CONFIRMED_DUPLICATE','CONFIRMED_TRANSFER','CONFIRMED_CARD_PAYMENT') NOT NULL DEFAULT 'UNMATCHED',
    source_row_number     INT           NULL,
    created_at            DATETIME      NOT NULL,
    updated_at            DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_statement FOREIGN KEY (statement_id) REFERENCES statements   (id),
    CONSTRAINT fk_transactions_account   FOREIGN KEY (account_id)   REFERENCES accounts     (id),
    CONSTRAINT fk_transactions_card      FOREIGN KEY (card_id)      REFERENCES cards        (id),
    CONSTRAINT fk_transactions_merchant  FOREIGN KEY (merchant_id)  REFERENCES merchants    (id),
    CONSTRAINT fk_transactions_category  FOREIGN KEY (category_id)  REFERENCES categories   (id),
    -- A transaction belongs to exactly one financial source
    CONSTRAINT chk_transactions_source CHECK (
        (account_id IS NOT NULL AND card_id IS NULL) OR
        (account_id IS NULL     AND card_id IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE INDEX idx_transactions_statement_id          ON transactions (statement_id);
CREATE INDEX idx_transactions_account_id            ON transactions (account_id);
CREATE INDEX idx_transactions_card_id               ON transactions (card_id);
CREATE INDEX idx_transactions_transaction_date      ON transactions (transaction_date);
CREATE INDEX idx_transactions_transaction_type      ON transactions (transaction_type);
CREATE INDEX idx_transactions_category_id           ON transactions (category_id);
CREATE INDEX idx_transactions_merchant_id           ON transactions (merchant_id);
CREATE INDEX idx_transactions_transaction_hash      ON transactions (transaction_hash);
CREATE INDEX idx_transactions_reconciliation_status ON transactions (reconciliation_status);

-- -------------------------------------------------------------
-- TRANSACTION LINKS
-- -------------------------------------------------------------
CREATE TABLE transaction_links (
    id                    CHAR(36)      NOT NULL,
    source_transaction_id CHAR(36)      NOT NULL,
    target_transaction_id CHAR(36)      NOT NULL,
    link_type             ENUM('CREDIT_CARD_PAYMENT','TRANSFER','REFUND','DUPLICATE','CASHBACK') NOT NULL,
    confidence_score      DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    status                ENUM('SUGGESTED','REVIEW_REQUIRED','CONFIRMED','REJECTED') NOT NULL DEFAULT 'SUGGESTED',
    reason                VARCHAR(500)  NULL,
    created_at            DATETIME      NOT NULL,
    reviewed_at           DATETIME      NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tlinks_source FOREIGN KEY (source_transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_tlinks_target FOREIGN KEY (target_transaction_id) REFERENCES transactions (id),
    -- A transaction cannot link to itself
    CONSTRAINT chk_tlinks_no_self_link CHECK (source_transaction_id <> target_transaction_id)
) ENGINE=InnoDB;

CREATE INDEX idx_tlinks_source_transaction_id ON transaction_links (source_transaction_id);
CREATE INDEX idx_tlinks_target_transaction_id ON transaction_links (target_transaction_id);
CREATE INDEX idx_tlinks_status                ON transaction_links (status);
CREATE INDEX idx_tlinks_link_type             ON transaction_links (link_type);

-- -------------------------------------------------------------
-- RECONCILIATION REVIEWS
-- -------------------------------------------------------------
CREATE TABLE reconciliation_reviews (
    id                   CHAR(36)      NOT NULL,
    transaction_link_id  CHAR(36)      NOT NULL,
    review_type          ENUM('POSSIBLE_DUPLICATE','POSSIBLE_CARD_PAYMENT','POSSIBLE_TRANSFER','POSSIBLE_REFUND','POSSIBLE_CASHBACK') NOT NULL,
    status               ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    system_reason        VARCHAR(1000) NULL,
    user_decision        VARCHAR(100)  NULL,
    created_at           DATETIME      NOT NULL,
    reviewed_at          DATETIME      NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_link FOREIGN KEY (transaction_link_id) REFERENCES transaction_links (id)
) ENGINE=InnoDB;

CREATE INDEX idx_reviews_status      ON reconciliation_reviews (status);
CREATE INDEX idx_reviews_review_type ON reconciliation_reviews (review_type);
