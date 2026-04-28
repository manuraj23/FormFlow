ALTER TABLE form_responses
    ADD COLUMN IF NOT EXISTS editable_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_edited_at TIMESTAMP;