-- Keep the database constraint aligned with URL validation (max 2048 chars).
ALTER TABLE url_mapping MODIFY COLUMN original_url VARCHAR(2048) NOT NULL;
