CREATE TABLE consultations (
    appointment_id BIGINT PRIMARY KEY REFERENCES appointments(id),
    complaint VARCHAR(4000) NOT NULL DEFAULT '',
    medical_history VARCHAR(4000) NOT NULL DEFAULT '',
    allergies VARCHAR(4000) NOT NULL DEFAULT '',
    examination VARCHAR(4000) NOT NULL DEFAULT '',
    treatment VARCHAR(4000) NOT NULL DEFAULT '',
    follow_up VARCHAR(4000) NOT NULL DEFAULT '',
    follow_up_date DATE,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
