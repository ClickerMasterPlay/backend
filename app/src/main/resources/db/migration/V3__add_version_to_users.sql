-- Add version column to users table for optimistic locking
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;