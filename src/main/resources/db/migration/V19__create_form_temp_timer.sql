CREATE TABLE IF NOT EXISTS form_temp_timer (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 form_id UUID NOT NULL,

                                 duration INTEGER,

                                 start_time TIMESTAMP,

                                 temp_user_id UUID NOT NULL,

                                 CONSTRAINT fk_form_temp_timer_form
                                     FOREIGN KEY (form_id)
                                         REFERENCES form(id)
                                         ON DELETE CASCADE
);