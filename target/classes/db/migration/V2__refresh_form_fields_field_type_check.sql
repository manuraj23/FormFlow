ALTER TABLE form_fields
    DROP CONSTRAINT IF EXISTS form_fields_field_type_check;

ALTER TABLE form_fields
    ADD CONSTRAINT form_fields_field_type_check
        CHECK (field_type IN (
            'TEXT',
            'EMAIL',
            'TEXTAREA',
            'NUMBER',
            'PHONE',
            'DATE',
            'TIME',
            'DROPDOWN',
            'RADIO',
            'CHECKBOX',
            'MULTI_SELECT',
            'FILE',
            'RATING'
        ));

