ALTER TABLE patients ADD COLUMN sex VARCHAR(10) CHECK (sex IN ('MALE', 'FEMALE', 'OTHER'));
ALTER TABLE patients ADD COLUMN weight_kg NUMERIC(6, 2) CHECK (weight_kg > 0);
ALTER TABLE patients ADD COLUMN height_cm NUMERIC(6, 2) CHECK (height_cm > 0);
