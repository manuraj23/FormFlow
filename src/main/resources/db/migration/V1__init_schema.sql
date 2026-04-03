-- Users table matching User entity
-- userId → user_id (Hibernate converts camelCase to snake_case)
CREATE TABLE users (
                       user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(255)
);

-- user_roles table for @ElementCollection List<String> roles in User entity
-- @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
CREATE TABLE user_roles (
                            user_id UUID REFERENCES users(user_id),
                            role VARCHAR(100)
);

-- Forms table matching Form entity
-- user_id is FK to users table
CREATE TABLE forms (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       title VARCHAR(255),
                       description VARCHAR(255),
                       published BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP,
                       user_id UUID REFERENCES users(user_id)
);

-- Form sections table matching FormSection entity
CREATE TABLE form_sections (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               section_title VARCHAR(255),
                               section_order INTEGER,
                               form_id UUID REFERENCES forms(id)
);

-- Form fields table matching FormFields entity
-- field_config is JSONB matching Map<String, Object> fieldConfig
CREATE TABLE form_fields (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             field_type VARCHAR(100),
                             field_order INTEGER,
                             field_config JSONB,
                             section_id UUID REFERENCES form_sections(id)
);

-- Form responses table matching FormResponse entity
-- response_id matches private UUID responseId
-- form_id is plain UUID — no FK because FormResponse entity has no @ManyToOne
CREATE TABLE form_responses (
                                response_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                form_id UUID,
                                response JSONB,
                                submitted_at TIMESTAMP
);

-- Refresh tokens table matching RefreshToken entity
-- expiry_date is TIMESTAMP matching Instant expiryDate
-- user_id is FK to users table matching @ManyToOne User user
CREATE TABLE refresh_token (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               token VARCHAR(255),
                               expiry_date TIMESTAMP,
                               user_id UUID REFERENCES users(user_id)
);