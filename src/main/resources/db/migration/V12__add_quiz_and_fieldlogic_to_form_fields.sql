-- Add quiz configuration support
ALTER TABLE form_fields
    ADD COLUMN IF NOT EXISTS quiz_config JSONB;

-- Add field logic configuration support
ALTER TABLE form_fields
    ADD COLUMN IF NOT EXISTS field_logic JSONB;