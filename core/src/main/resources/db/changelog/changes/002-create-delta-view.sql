CREATE OR REPLACE VIEW v_currency_delta AS
SELECT
    nbk.code AS currency_code,
    nbk.rate AS nbk_rate,
    xe.rate AS xe_rate,
    (nbk.rate - xe.rate) AS delta,
    nbk.updated_at AS nbk_updated_at,
    xe.updated_at AS xe_updated_at
FROM currency_rates nbk
         JOIN currency_rates xe ON nbk.code = xe.code
WHERE nbk.source_id = 'NBK' AND xe.source_id = 'XE';