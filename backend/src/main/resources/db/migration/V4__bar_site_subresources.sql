CREATE TABLE IF NOT EXISTS bar_site_accounts (
    id BIGSERIAL PRIMARY KEY,
    bar_asset_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    owner VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_bar_site_accounts_asset
        FOREIGN KEY (bar_asset_id) REFERENCES bar_assets(id)
);

CREATE INDEX IF NOT EXISTS idx_bar_site_accounts_asset_id
    ON bar_site_accounts(bar_asset_id);

CREATE TABLE IF NOT EXISTS bar_site_sops (
    id BIGSERIAL PRIMARY KEY,
    bar_asset_id BIGINT NOT NULL UNIQUE,
    reset_url VARCHAR(500),
    unlock_url VARCHAR(500),
    estimated_time VARCHAR(100),
    contacts_json TEXT,
    required_docs_json TEXT,
    faqs_json TEXT,
    history_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_bar_site_sops_asset
        FOREIGN KEY (bar_asset_id) REFERENCES bar_assets(id)
);

CREATE TABLE IF NOT EXISTS bar_site_attachments (
    id BIGSERIAL PRIMARY KEY,
    bar_asset_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    size VARCHAR(50),
    content_type VARCHAR(100),
    url VARCHAR(500),
    uploaded_by VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bar_site_attachments_asset
        FOREIGN KEY (bar_asset_id) REFERENCES bar_assets(id)
);

CREATE INDEX IF NOT EXISTS idx_bar_site_attachments_asset_id
    ON bar_site_attachments(bar_asset_id);

CREATE TABLE IF NOT EXISTS bar_site_verifications (
    id BIGSERIAL PRIMARY KEY,
    bar_asset_id BIGINT NOT NULL,
    verified_by VARCHAR(100) NOT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    message VARCHAR(500),
    CONSTRAINT fk_bar_site_verifications_asset
        FOREIGN KEY (bar_asset_id) REFERENCES bar_assets(id)
);

CREATE INDEX IF NOT EXISTS idx_bar_site_verifications_asset_id
    ON bar_site_verifications(bar_asset_id);
