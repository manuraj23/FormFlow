CREATE TABLE IF NOT EXISTS user_form_roles (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 user_id UUID NOT NULL,
                                 form_id UUID NOT NULL,

                                 role VARCHAR(50) NOT NULL,
                                 is_viewed BOOLEAN DEFAULT FALSE,

                                 CONSTRAINT fk_user
                                     FOREIGN KEY(user_id) REFERENCES users(user_id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_form
                                     FOREIGN KEY(form_id) REFERENCES forms(id)
                                         ON DELETE CASCADE
);