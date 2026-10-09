CREATE TABLE daily_checks (
    check_date DATE PRIMARY KEY,
    sterilization_status VARCHAR(20) NOT NULL CHECK (sterilization_status IN ('PENDING', 'DONE', 'NOT_REQUIRED')),
    lab_status VARCHAR(20) NOT NULL CHECK (lab_status IN ('PENDING', 'DONE', 'NOT_REQUIRED')),
    remarks VARCHAR(2000) NOT NULL DEFAULT '',
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
