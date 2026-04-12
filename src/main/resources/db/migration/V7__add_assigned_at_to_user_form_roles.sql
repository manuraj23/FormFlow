ALTER TABLE user_form_roles
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP;