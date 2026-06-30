CREATE TABLE loans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    principal_amount DECIMAL(19, 2) NOT NULL,
    annual_interest_rate DECIMAL(9, 4) NOT NULL,
    tenor_months INT NOT NULL,
    monthly_emi DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE loan_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    opening_balance DECIMAL(19, 2) NOT NULL,
    emi_amount DECIMAL(19, 2) NOT NULL,
    interest_component DECIMAL(19, 2) NOT NULL,
    principal_component DECIMAL(19, 2) NOT NULL,
    closing_balance DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_schedules_loan
        FOREIGN KEY (loan_id) REFERENCES loans (id),
    CONSTRAINT uk_loan_schedules_loan_installment
        UNIQUE (loan_id, installment_number)
);

CREATE INDEX idx_loan_schedules_loan_id
    ON loan_schedules (loan_id);

CREATE INDEX idx_loan_schedules_status
    ON loan_schedules (status);

CREATE TABLE loan_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    strategy_type VARCHAR(40),
    installment_number INT,
    amount DECIMAL(19, 2) NOT NULL,
    balance_before DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_transactions_loan
        FOREIGN KEY (loan_id) REFERENCES loans (id)
);

CREATE INDEX idx_loan_transactions_loan_id
    ON loan_transactions (loan_id);

CREATE INDEX idx_loan_transactions_type
    ON loan_transactions (transaction_type);
