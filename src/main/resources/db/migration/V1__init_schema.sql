-- Users table matching User entity
-- userId → user_id (Hibernate converts camelCase to snake_case)
CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(255)
);

-- user_roles table for @ElementCollection List<String> roles in User entity
-- @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
CREATE TABLE user_roles (
                            user_id BIGINT REFERENCES users(user_id),
                            role VARCHAR(100)
);

-- Forms table matching Form entity
-- user_id is FK to users table
CREATE TABLE forms (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255),
                       description VARCHAR(255),
                       published BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP,
                       user_id BIGINT REFERENCES users(user_id)
);

-- Form sections table matching FormSection entity
CREATE TABLE form_sections (
                               id BIGSERIAL PRIMARY KEY,
                               section_title VARCHAR(255),
                               section_order INTEGER,
                               form_id BIGINT REFERENCES forms(id)
);

-- Form fields table matching FormFields entity
-- field_config is JSONB matching Map<String, Object> fieldConfig
CREATE TABLE form_fields (
                             id BIGSERIAL PRIMARY KEY,
                             field_type VARCHAR(100),
                             field_order INTEGER,
                             field_config JSONB,
                             section_id BIGINT REFERENCES form_sections(id)
);

-- Form responses table matching FormResponse entity
-- response_id matches private Long responseId
-- form_id is plain BIGINT — no FK because FormResponse entity has no @ManyToOne
CREATE TABLE form_responses (
                                response_id BIGSERIAL PRIMARY KEY,
                                form_id BIGINT,
                                response JSONB,
                                submitted_at TIMESTAMP
);

-- Refresh tokens table matching RefreshToken entity
-- expiry_date is TIMESTAMP matching Instant expiryDate
-- user_id is FK to users table matching @ManyToOne User user
CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                token VARCHAR(255),
                                expiry_date TIMESTAMP,
                                user_id BIGINT REFERENCES users(user_id)
);