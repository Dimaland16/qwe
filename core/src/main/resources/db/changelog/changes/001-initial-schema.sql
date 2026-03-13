CREATE TABLE currency_rates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(3) NOT NULL,
    source_id VARCHAR(10) NOT NULL,
    rate NUMERIC(19, 4) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_currency_source UNIQUE (code, source_id)
    );