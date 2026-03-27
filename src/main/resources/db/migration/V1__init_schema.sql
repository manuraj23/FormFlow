-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255)
    );

-- Create forms table
CREATE TABLE IF NOT EXISTS forms (
     id BIGSERIAL PRIMARY KEY,
     title VARCHAR(255),
    description VARCHAR(255),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    user_id BIGINT REFERENCES users(id)
    );

-- Create form_sections table
CREATE TABLE IF NOT EXISTS form_sections (
    id BIGSERIAL PRIMARY KEY,
    section_title VARCHAR(255),
    section_order INTEGER,
    form_id BIGINT REFERENCES forms(id)
    );

-- Create form_fields table
CREATE TABLE IF NOT EXISTS form_fields (
    id BIGSERIAL PRIMARY KEY,
    field_type VARCHAR(100),
    field_order INTEGER,
    field_config JSONB,
    section_id BIGINT REFERENCES form_sections(id)
    );

-- Create form_responses table
CREATE TABLE IF NOT EXISTS form_responses (
                                              response_id BIGSERIAL PRIMARY KEY,
                                              form_id BIGINT,
                                              response JSONB,
                                              submitted_at TIMESTAMP
);