-- add field_logic to form_fields
ALTER TABLE form_fields
    ADD COLUMN IF NOT EXISTS field_logic JSONB;

-- add proper FK constraint to form_responses.form_id
-- only if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_form_responses_form'
        AND table_name = 'form_responses'
    ) THEN
ALTER TABLE form_responses
    ADD CONSTRAINT fk_form_responses_form
        FOREIGN KEY (form_id) REFERENCES forms(id);
END IF;
END $$;