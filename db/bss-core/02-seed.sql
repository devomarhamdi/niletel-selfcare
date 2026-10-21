-- =====================================================================
-- bss-core  |  database bss_core  |  PostgreSQL 16
-- Requirement D-04: EXACTLY the fixed test data of Interface Contract section 4.1.
--
-- Run as the service user, on a database where 01-ddl.sql already ran (empty tables):
--   psql -h localhost -U bss_app -d bss_core -v ON_ERROR_STOP=1 -f db/bss-core/02-seed.sql
-- The whole file is one transaction: it loads completely or not at all.
-- =====================================================================

BEGIN;

-- 1. Tariff plans (3 rows)
INSERT INTO tariff_plan (tariff_code, tariff_name, rate_per_min_piasters, active_flag) VALUES
    ('PRE01', 'Nile Prepaid', 19, 'Y'),
    ('PRE02', 'Nile Youth',   14, 'Y'),
    ('OLD99', 'Classic 2015', 25, 'N');

-- 2. Subscribers (4 rows) — PINs hashed with SHA-256 hex (rule 7)
INSERT INTO subscriber (msisdn, full_name, pin_hash, status, balance_piasters, activation_dt, tariff_code) VALUES
    ('01012345678', 'Mona Adel',     encode(sha256(convert_to('1234', 'UTF8')), 'hex'), 'A', 6750, DATE '2025-03-01', 'PRE01'),
    ('01123456789', 'Karim Hassan',  encode(sha256(convert_to('4321', 'UTF8')), 'hex'), 'A',  500, DATE '2025-06-15', 'PRE02'),
    ('01234567890', 'Salma Nabil',   encode(sha256(convert_to('1111', 'UTF8')), 'hex'), 'S', 2500, DATE '2024-01-20', 'PRE01'),
    ('01512345678', 'Youssef Fathy', encode(sha256(convert_to('2222', 'UTF8')), 'hex'), 'A',    0, DATE '2016-08-12', 'OLD99');

-- 3. Operators (2 rows) — password hashes with SHA-256 hex
INSERT INTO operator (username, password_hash, full_name, role_code, active) VALUES
    ('operator1',    encode(sha256(convert_to('Operator-1', 'UTF8')), 'hex'), 'Hany Samir',       'OPR', TRUE),
    ('selfcare-svc', encode(sha256(convert_to('Svc-2026',    'UTF8')), 'hex'), 'Self-Care Service', 'SYS', TRUE);

-- 4. Ledger (7 rows) — exact IDs and timestamps from the contract
INSERT INTO ledger (txn_id, msisdn, txn_ts, txn_type, amount_piasters, balance_after, ref_no, reason) VALUES
    (50201, '01012345678', TIMESTAMP '2026-10-05 10:15:02', 'R', 5000, 5000, '',                   'VOUCHER RECHARGE'),
    (50203, '01234567890', TIMESTAMP '2026-10-06 09:30:00', 'R', 2500, 2500, '',                   'VOUCHER RECHARGE'),
    (50205, '01123456789', TIMESTAMP '2026-10-06 11:20:00', 'R',  500,  500, '',                   'VOUCHER RECHARGE'),
    (50207, '01012345678', TIMESTAMP '2026-10-07 18:40:11', 'R', 2500, 7500, '',                   'VOUCHER RECHARGE'),
    (50212, '01012345678', TIMESTAMP '2026-10-08 21:03:30', 'D',  750, 6750, 'SC-20261008-000004', 'BUNDLE Social Day'),
    (50215, '01012345678', TIMESTAMP '2026-10-09 09:05:47', 'D', 4500, 2250, 'SC-20261009-000007', 'BUNDLE Talk 300'),
    (50216, '01012345678', TIMESTAMP '2026-10-09 09:05:48', 'F', 4500, 6750, 'SC-20261009-000007', 'REFUND');

-- 5. Vouchers (19 rows)
-- 5 unused with their own values
INSERT INTO voucher (voucher_code, value_piasters, created_by_operator, created_at) VALUES
    ('10000000000001',  1000, 'operator1', TIMESTAMP '2026-10-01 08:00:00'),
    ('10000000000002',  2500, 'operator1', TIMESTAMP '2026-10-01 08:00:00'),
    ('10000000000003',  5000, 'operator1', TIMESTAMP '2026-10-01 08:00:00'),
    ('10000000000004', 10000, 'operator1', TIMESTAMP '2026-10-01 08:00:00'),
    ('10000000000005',  5000, 'operator1', TIMESTAMP '2026-10-01 08:00:00');

-- 10 unused of 5000: 20000000000001 to 20000000000010
INSERT INTO voucher (voucher_code, value_piasters, created_by_operator, created_at)
SELECT (20000000000000 + n)::text, 5000, 'operator1', TIMESTAMP '2026-10-01 08:00:00'
  FROM generate_series(1, 10) AS n;

-- 4 used cards (matching the ledger recharge timestamps)
INSERT INTO voucher (voucher_code, value_piasters, used_by_msisdn, used_at, created_by_operator, created_at) VALUES
    ('10000000000006', 2500, '01234567890', TIMESTAMP '2026-10-06 09:30:00', 'operator1', TIMESTAMP '2026-10-01 08:00:00'),
    ('10000000000007',  500, '01123456789', TIMESTAMP '2026-10-06 11:20:00', 'operator1', TIMESTAMP '2026-10-01 08:00:00'),
    ('10000000000008', 5000, '01012345678', TIMESTAMP '2026-10-05 10:15:02', 'operator1', TIMESTAMP '2026-10-01 08:00:00'),
    ('10000000000009', 2500, '01012345678', TIMESTAMP '2026-10-07 18:40:11', 'operator1', TIMESTAMP '2026-10-01 08:00:00');

-- 6. Reset the identity counter (next ledger id = 50231)
ALTER TABLE ledger ALTER COLUMN txn_id RESTART WITH 50231;

COMMIT;