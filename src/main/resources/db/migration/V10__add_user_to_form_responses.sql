ALTER TABLE form_responses ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(user_id);

