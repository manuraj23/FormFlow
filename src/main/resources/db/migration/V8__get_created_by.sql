ALTER TABLE user_form_roles
    ADD COLUMN IF NOT EXISTS assigned_by UUID REFERENCES users(user_id);