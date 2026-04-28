CREATE TABLE IF NOT EXISTS form_timer (
                            id UUID PRIMARY KEY,

                            user_id UUID NOT NULL,
                            form_id UUID NOT NULL,

                            duration INTEGER,

                            start_time TIMESTAMP,

                            CONSTRAINT fk_form_timer_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_form_timer_form
                                FOREIGN KEY (form_id)
                                    REFERENCES form(id)
                                    ON DELETE CASCADE
);