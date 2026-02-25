CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.users_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    is_superuser BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login TIMESTAMPTZ NULL,
    last_seen TIMESTAMPTZ NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS auth.items_itemcategory (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    color VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS auth.items_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    reported_counts INTEGER NOT NULL DEFAULT 0,
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    image BYTEA,
    reporter_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_items_item_reporter FOREIGN KEY (reporter_id) REFERENCES auth.users_user (id),
    CONSTRAINT fk_items_item_category FOREIGN KEY (category_id) REFERENCES auth.items_itemcategory (id)
);

CREATE TABLE IF NOT EXISTS auth.comment (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    item_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    CONSTRAINT fk_comment_item FOREIGN KEY (item_id) REFERENCES auth.items_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES auth.users_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES auth.comment (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS auth.comment_report (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    cause TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_comment_report_comment FOREIGN KEY (comment_id) REFERENCES auth.comment (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_report_reporter FOREIGN KEY (reporter_id) REFERENCES auth.users_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_report_comment_reporter UNIQUE (comment_id, reporter_id)
);

CREATE TABLE IF NOT EXISTS auth.item_report (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    cause TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_item_report_item FOREIGN KEY (item_id) REFERENCES auth.items_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_report_reporter FOREIGN KEY (reporter_id) REFERENCES auth.users_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_item_report_item_reporter UNIQUE (item_id, reporter_id)
);

CREATE TABLE IF NOT EXISTS auth.otp (
    owner BIGINT PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_otp_owner FOREIGN KEY (owner) REFERENCES auth.users_user (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auth.map_tile (
    id BIGSERIAL PRIMARY KEY,
    longitude DOUBLE PRECISION NOT NULL,
    latitude DOUBLE PRECISION NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_item_category_id ON auth.items_item (category_id);
CREATE INDEX IF NOT EXISTS idx_items_item_reporter_id ON auth.items_item (reporter_id);
CREATE INDEX IF NOT EXISTS idx_comment_item_id ON auth.comment (item_id);
CREATE INDEX IF NOT EXISTS idx_comment_parent_id ON auth.comment (parent_id);
CREATE INDEX IF NOT EXISTS idx_comment_report_comment_id ON auth.comment_report (comment_id);
CREATE INDEX IF NOT EXISTS idx_item_report_item_id ON auth.item_report (item_id);
