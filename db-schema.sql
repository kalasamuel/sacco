-- Kimwanyi SACCO Management System database schema and initial admin seed

DROP DATABASE IF EXISTS `sacco`;
CREATE DATABASE IF NOT EXISTS `sacco`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE `sacco`;

CREATE TABLE `user_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `first_name` VARCHAR(255) NOT NULL,
  `last_name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `role` VARCHAR(255) NOT NULL,
  `enabled` BOOLEAN NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_accounts_username` (`username`),
  UNIQUE KEY `uk_user_accounts_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `members` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_account_id` BIGINT NOT NULL,
  `membership_number` VARCHAR(255) NOT NULL,
  `national_id` VARCHAR(255) NOT NULL,
  `phone_number` VARCHAR(30) DEFAULT NULL,
  `status` VARCHAR(30) NOT NULL,
  `joined_at` DATE NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_members_user_account_id` (`user_account_id`),
  UNIQUE KEY `uk_members_membership_number` (`membership_number`),
  UNIQUE KEY `uk_members_national_id` (`national_id`),
  CONSTRAINT `fk_members_user_account` FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `savings_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `member_id` BIGINT NOT NULL,
  `account_number` VARCHAR(255) NOT NULL,
  `balance` DECIMAL(19,2) NOT NULL,
  `minimum_balance` DECIMAL(19,2) NOT NULL,
  `version` BIGINT DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_savings_accounts_member_id` (`member_id`),
  UNIQUE KEY `uk_savings_accounts_account_number` (`account_number`),
  CONSTRAINT `fk_savings_accounts_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `savings_transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `savings_account_id` BIGINT NOT NULL,
  `reference` VARCHAR(255) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `amount` DECIMAL(19,2) NOT NULL,
  `balance_before` DECIMAL(19,2) NOT NULL,
  `balance_after` DECIMAL(19,2) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `performed_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_savings_transactions_reference` (`reference`),
  CONSTRAINT `fk_savings_transactions_account` FOREIGN KEY (`savings_account_id`) REFERENCES `savings_accounts` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_savings_transactions_performed_by` FOREIGN KEY (`performed_by`) REFERENCES `user_accounts` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `internal_transfers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reference` VARCHAR(255) NOT NULL,
  `sender_account_id` BIGINT NOT NULL,
  `receiver_account_id` BIGINT NOT NULL,
  `amount` DECIMAL(19,2) NOT NULL,
  `status` VARCHAR(255) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `initiated_by` BIGINT NOT NULL,
  `failure_reason` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `completed_at` DATETIME(6) DEFAULT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_internal_transfers_reference` (`reference`),
  CONSTRAINT `fk_internal_transfers_sender_account` FOREIGN KEY (`sender_account_id`) REFERENCES `savings_accounts` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_internal_transfers_receiver_account` FOREIGN KEY (`receiver_account_id`) REFERENCES `savings_accounts` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_internal_transfers_initiated_by` FOREIGN KEY (`initiated_by`) REFERENCES `user_accounts` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `loans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `member_id` BIGINT NOT NULL,
  `principal` DECIMAL(19,2) NOT NULL,
  `interest_rate` DECIMAL(5,2) NOT NULL,
  `interest_amount` DECIMAL(19,2) NOT NULL,
  `total_repayable` DECIMAL(19,2) NOT NULL,
  `amount_repaid` DECIMAL(19,2) NOT NULL,
  `outstanding_balance` DECIMAL(19,2) NOT NULL,
  `status` VARCHAR(255) NOT NULL,
  `purpose` VARCHAR(500) NOT NULL,
  `application_date` DATE NOT NULL,
  `decision_date` DATE DEFAULT NULL,
  `due_date` DATE DEFAULT NULL,
  `decided_by` BIGINT DEFAULT NULL,
  `rejection_reason` VARCHAR(500) DEFAULT NULL,
  `version` BIGINT DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_loans_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_loans_decided_by` FOREIGN KEY (`decided_by`) REFERENCES `user_accounts` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `loan_repayments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `loan_id` BIGINT NOT NULL,
  `reference` VARCHAR(255) NOT NULL,
  `amount` DECIMAL(19,2) NOT NULL,
  `balance_before` DECIMAL(19,2) NOT NULL,
  `balance_after` DECIMAL(19,2) NOT NULL,
  `payment_method` VARCHAR(255) NOT NULL,
  `received_by` BIGINT DEFAULT NULL,
  `payment_date` DATE NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loan_repayments_reference` (`reference`),
  CONSTRAINT `fk_loan_repayments_loan` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_loan_repayments_received_by` FOREIGN KEY (`received_by`) REFERENCES `user_accounts` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reference` VARCHAR(255) NOT NULL,
  `user_account_id` BIGINT NOT NULL,
  `amount` DECIMAL(19,2) NOT NULL,
  `payment_method` VARCHAR(255) NOT NULL,
  `mobile_money_provider` VARCHAR(255) DEFAULT NULL,
  `phone_number` VARCHAR(255) DEFAULT NULL,
  `card_last_four` VARCHAR(4) DEFAULT NULL,
  `status` VARCHAR(255) NOT NULL,
  `external_reference` VARCHAR(255) DEFAULT NULL,
  `failure_reason` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `completed_at` DATETIME(6) DEFAULT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_reference` (`reference`),
  UNIQUE KEY `uk_payments_external_reference` (`external_reference`),
  CONSTRAINT `fk_payments_user_account` FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `audit_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_account_id` BIGINT DEFAULT NULL,
  `action` VARCHAR(255) NOT NULL,
  `entity_type` VARCHAR(100) NOT NULL,
  `entity_id` BIGINT DEFAULT NULL,
  `description` VARCHAR(1000) DEFAULT NULL,
  `old_value` TEXT DEFAULT NULL,
  `new_value` TEXT DEFAULT NULL,
  `ip_address` VARCHAR(45) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_audit_logs_user_account` FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `email_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_account_id` BIGINT DEFAULT NULL,
  `recipient_email` VARCHAR(255) NOT NULL,
  `subject` VARCHAR(200) NOT NULL,
  `message` TEXT NOT NULL,
  `status` VARCHAR(255) NOT NULL,
  `failure_reason` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `sent_at` DATETIME(6) DEFAULT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_email_logs_user_account` FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_account_id` BIGINT NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `message` VARCHAR(1000) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `read_status` BOOLEAN NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `read_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_notifications_user_account` FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed a default administrator account. Password is bcrypt hashed for 'admin'.
INSERT INTO `user_accounts` (`username`, `password_hash`, `first_name`, `last_name`, `email`, `role`, `enabled`, `created_at`, `updated_at`)
VALUES (
  'admin',
  '$2b$10$.Nu1B0ovSeBL5HNUWq5JJ.1IXqiNlE1l7I3Rwl6u.1QciP/Rm/Imu',
  'System',
  'Administrator',
  'admin@kimwanyisacco.com',
  'ADMIN',
  true,
  NOW(6),
  NOW(6)
);
