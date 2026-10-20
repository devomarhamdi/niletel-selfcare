-- =====================================================================
-- bss-core  |  database bss_core  |  PostgreSQL 16
-- Requirement D-03: every rule the database can enforce.
--
-- Run as the owner of the database (no GRANT is needed):
--   psql -h localhost -U bss_app -d bss_core -f bss-core/01-ddl.sql
-- The whole script is one transaction: it runs completely or not at all.
-- Run on an EMPTY database only. Names are lower case and never quoted.
--
-- Conventions of the legacy interface that the schema follows:
--   * money is a whole number of piasters (C7), never a decimal;
--   * flags are 'Y'/'N', statuses and types are one letter (C8);
--   * "no value" in text columns is '' and not NULL (C9);
--   * timestamps are Cairo local time without a zone, to the second (C6);
--     set the TZ of PostgreSQL to Africa/Cairo (see Interface Contract, section 7);
--   * PINs and passwords are SHA-256 hex, 64 lower-case characters (rule 7, L-10).
--
-- The seed script (02-seed.sql) must, after it inserted the fixed ids:
--   ALTER TABLE ledger ALTER COLUMN txn_id RESTART WITH 50231;   -- "next ledger id" of the contract
-- =====================================================================

BEGIN;

-- Cairo local time for every new connection to this database (C6). Without it a container
-- (UTC) would write LOCALTIMESTAMP defaults two or three hours off. The setting applies from
-- the next connection on, so it does not matter for the rest of this script.
DO
$$
BEGIN
    EXECUTE format('ALTER DATABASE %I SET timezone TO %L', current_database(), 'Africa/Cairo');
END
$$;

-- ---------------------------------------------------------------------
-- tariff_plan: the price lists. Old plans are never deleted (they are
-- only closed with active_flag = 'N') because subscribers still use them.
-- Read through the singleton cache (L-08) by GET /tariffs (L-22).
-- ---------------------------------------------------------------------
CREATE TABLE tariff_plan (
    tariff_code           VARCHAR(5)  NOT NULL,
    tariff_name           VARCHAR(40) NOT NULL,
    rate_per_min_piasters INTEGER     NOT NULL,
    active_flag           CHAR(1)     NOT NULL DEFAULT 'Y',
    CONSTRAINT pk_tariff_plan        PRIMARY KEY (tariff_code),
    CONSTRAINT ck_tariff_plan_code   CHECK (tariff_code ~ '^[A-Z0-9]{5}$'),
    CONSTRAINT ck_tariff_plan_name   CHECK (char_length(btrim(tariff_name)) >= 1),
    CONSTRAINT ck_tariff_plan_rate   CHECK (rate_per_min_piasters >= 0),
    CONSTRAINT ck_tariff_plan_active CHECK (active_flag IN ('Y', 'N'))
);

-- ---------------------------------------------------------------------
-- subscriber: one prepaid line. bss-core owns the money and the identity.
-- The balance is changed only by the conditional UPDATEs of the DAOs
-- (see the hints in the Project Brief, section 9.3); the CHECK is the
-- last line of defence for rule 1 "a balance is never below zero".
-- ---------------------------------------------------------------------
CREATE TABLE subscriber (
    msisdn           VARCHAR(11) NOT NULL,
    full_name        VARCHAR(80) NOT NULL,
    pin_hash         CHAR(64)    NOT NULL,
    status           CHAR(1)     NOT NULL DEFAULT 'A',
    balance_piasters INTEGER     NOT NULL DEFAULT 0,
    activation_dt    DATE        NOT NULL DEFAULT CURRENT_DATE,
    tariff_code      VARCHAR(5)  NOT NULL,
    CONSTRAINT pk_subscriber          PRIMARY KEY (msisdn),
    CONSTRAINT fk_subscriber_tariff   FOREIGN KEY (tariff_code) REFERENCES tariff_plan (tariff_code),
    CONSTRAINT ck_subscriber_msisdn   CHECK (msisdn ~ '^01[0125][0-9]{8}$'),
    CONSTRAINT ck_subscriber_name     CHECK (char_length(btrim(full_name)) >= 1),
    CONSTRAINT ck_subscriber_pin_hash CHECK (pin_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_subscriber_status   CHECK (status IN ('A', 'S')),
    CONSTRAINT ck_subscriber_balance  CHECK (balance_piasters >= 0)
);

-- Back-office search by the start of the MSISDN (L-18): LIKE '0101%' cannot use
-- the primary key index when the database collation is not "C".
CREATE INDEX idx_subscriber_msisdn_prefix ON subscriber (msisdn varchar_pattern_ops);

-- ---------------------------------------------------------------------
-- operator: back-office accounts (role OPR) and system accounts (role SYS,
-- for example selfcare-svc). Needed by voucher.created_by_operator (L-11)
-- and by the operator login (L-15, L-17).
-- ---------------------------------------------------------------------
CREATE TABLE operator (
    username      VARCHAR(30) NOT NULL,
    password_hash CHAR(64)    NOT NULL,
    full_name     VARCHAR(80) NOT NULL,
    role_code     CHAR(3)     NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_operator               PRIMARY KEY (username),
    CONSTRAINT ck_operator_username      CHECK (char_length(username) >= 3),
    CONSTRAINT ck_operator_password_hash CHECK (password_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_operator_name          CHECK (char_length(btrim(full_name)) >= 1),
    CONSTRAINT ck_operator_role          CHECK (role_code IN ('OPR', 'SYS'))
);

-- ---------------------------------------------------------------------
-- ledger: the bank statement of every line. One row per balance change,
-- never changed and never deleted (see the trigger below).
--   txn_type        R recharge, D debit, F refund
--   amount_piasters always positive; the type gives the direction
--   balance_after   the balance (piasters) after this change. This copy of the
--                   balance is on purpose (D-05): it is the statement the
--                   customer reads, and report Q3 uses it to find mistakes.
--   ref_no          '' for a recharge; for D and F the reference number chosen
--                   by the caller. A refund carries the ref_no of its debit.
-- ---------------------------------------------------------------------
CREATE TABLE ledger (
    txn_id          BIGINT       GENERATED BY DEFAULT AS IDENTITY,  -- BY DEFAULT: the seed writes its own ids
    msisdn          VARCHAR(11)  NOT NULL,
    txn_ts          TIMESTAMP(0) NOT NULL DEFAULT LOCALTIMESTAMP(0),
    txn_type        CHAR(1)      NOT NULL,
    amount_piasters INTEGER      NOT NULL,
    balance_after   INTEGER      NOT NULL,
    ref_no          VARCHAR(20)  NOT NULL DEFAULT '',
    reason          VARCHAR(100) NOT NULL,
    CONSTRAINT pk_ledger            PRIMARY KEY (txn_id),
    CONSTRAINT fk_ledger_subscriber FOREIGN KEY (msisdn) REFERENCES subscriber (msisdn),
    -- target of the composite foreign key of sms_log: an SMS row can never name
    -- a ledger row of another line
    CONSTRAINT uq_ledger_txn_msisdn UNIQUE (txn_id, msisdn),
    CONSTRAINT ck_ledger_type       CHECK (txn_type IN ('R', 'D', 'F')),
    -- 10000000 is the largest debit the contract accepts (BAL001); a refund gives a debit back
    CONSTRAINT ck_ledger_amount     CHECK (amount_piasters BETWEEN 1 AND 10000000),
    CONSTRAINT ck_ledger_balance    CHECK (balance_after >= 0),
    CONSTRAINT ck_ledger_reason     CHECK (char_length(btrim(reason)) >= 1),
    CONSTRAINT ck_ledger_ref_no     CHECK (
        (txn_type = 'R' AND ref_no = '')
        OR (txn_type IN ('D', 'F') AND ref_no ~ '^[A-Z0-9-]{6,20}$')
    )
);

-- Rule 4 and TXN002 (L-24): the reference number of a debit is unique in the whole system.
CREATE UNIQUE INDEX uq_ledger_debit_ref  ON ledger (ref_no) WHERE txn_type = 'D';
-- Rule 4 and TXN003 (L-13): a debit is refunded at most once. Two refunds at the same
-- moment cannot both succeed: the second INSERT fails on this index.
CREATE UNIQUE INDEX uq_ledger_refund_ref ON ledger (ref_no) WHERE txn_type = 'F';
-- History of one line, newest first (L-14, N-19) and "newest row of every subscriber" (Q2).
CREATE INDEX idx_ledger_msisdn_ts ON ledger (msisdn, txn_ts DESC, txn_id DESC);

-- "Rows are never changed or deleted" (Project Brief, section 9.1).
CREATE FUNCTION fn_ledger_append_only() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'ledger rows are never changed or deleted (txn_id %)', OLD.txn_id
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$;

CREATE TRIGGER trg_ledger_append_only
    BEFORE UPDATE OR DELETE ON ledger
    FOR EACH ROW EXECUTE FUNCTION fn_ledger_append_only();

-- ---------------------------------------------------------------------
-- voucher: a scratch card. Used at most once (rule 3): the recharge runs
--   UPDATE voucher SET used_by_msisdn = ?, used_at = ?
--    WHERE voucher_code = ? AND used_by_msisdn IS NULL
-- and checks that one row changed (VCH002 otherwise) - L-11, L-12.
-- ---------------------------------------------------------------------
CREATE TABLE voucher (
    voucher_code        VARCHAR(14)  NOT NULL,
    value_piasters      INTEGER      NOT NULL,
    used_by_msisdn      VARCHAR(11),
    used_at             TIMESTAMP(0),
    created_by_operator VARCHAR(30)  NOT NULL,
    created_at          TIMESTAMP(0) NOT NULL DEFAULT LOCALTIMESTAMP(0),
    CONSTRAINT pk_voucher            PRIMARY KEY (voucher_code),
    CONSTRAINT fk_voucher_subscriber FOREIGN KEY (used_by_msisdn)      REFERENCES subscriber (msisdn),
    CONSTRAINT fk_voucher_operator   FOREIGN KEY (created_by_operator) REFERENCES operator (username),
    CONSTRAINT ck_voucher_code       CHECK (voucher_code ~ '^[0-9]{14}$'),
    CONSTRAINT ck_voucher_value      CHECK (value_piasters > 0),
    -- who and when are written together, or both stay empty
    CONSTRAINT ck_voucher_used       CHECK ((used_by_msisdn IS NULL) = (used_at IS NULL)),
    CONSTRAINT ck_voucher_used_after CHECK (used_at IS NULL OR used_at >= created_at)
);

-- ---------------------------------------------------------------------
-- sms_log: one row per SMS, written by the message-driven bean (L-16).
-- UNIQUE (txn_id) is the idempotency key of L-23: the same message delivered
-- twice writes one row (INSERT ... ON CONFLICT (txn_id) DO NOTHING).
-- The composite foreign key keeps msisdn and txn_id consistent, so that the
-- msisdn copied from the brief's list of SMS fields can never disagree with
-- the ledger row it belongs to.
-- ---------------------------------------------------------------------
CREATE TABLE sms_log (
    sms_id       BIGINT       GENERATED BY DEFAULT AS IDENTITY,
    msisdn       VARCHAR(11)  NOT NULL,
    txn_id       BIGINT       NOT NULL,
    message_text VARCHAR(200) NOT NULL,
    sent_at      TIMESTAMP(0) NOT NULL DEFAULT LOCALTIMESTAMP(0),
    CONSTRAINT pk_sms_log        PRIMARY KEY (sms_id),
    CONSTRAINT uq_sms_log_txn    UNIQUE (txn_id),
    CONSTRAINT fk_sms_log_ledger FOREIGN KEY (txn_id, msisdn) REFERENCES ledger (txn_id, msisdn),
    CONSTRAINT ck_sms_log_text   CHECK (char_length(btrim(message_text)) >= 1)
);

-- Back-office page "newest 50 SMS", with and without the filter by MSISDN (L-19).
CREATE INDEX idx_sms_log_sent        ON sms_log (sent_at DESC, sms_id DESC);
CREATE INDEX idx_sms_log_msisdn_sent ON sms_log (msisdn, sent_at DESC, sms_id DESC);

COMMIT;
