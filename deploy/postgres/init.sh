#!/bin/bash
# Runs once, on the first start of an empty PostgreSQL volume (docker-entrypoint-initdb.d).
# Uses the same scripts as the manual setup, in the same order. /db is the repository's db/ folder.
set -euo pipefail

# 1. Roles and databases (run as the PostgreSQL administrator)
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres \
     -v bss_pw="$BSS_DB_PASSWORD" -v sc_pw="$SELFCARE_DB_PASSWORD" \
     -f /db/00-create-databases.sql

# 2. Tables and seed data, each as the user of that service
run_as() { # user database file
    if [ -s "$3" ]; then psql -v ON_ERROR_STOP=1 -U "$1" -d "$2" -f "$3"; fi
}
run_as bss_app      bss_core /db/bss-core/01-ddl.sql   "$BSS_DB_PASSWORD"
run_as bss_app      bss_core /db/bss-core/02-seed.sql  "$BSS_DB_PASSWORD"
run_as selfcare_app selfcare /db/selfcare/01-ddl.sql   "$SELFCARE_DB_PASSWORD"
run_as selfcare_app selfcare /db/selfcare/02-seed.sql  "$SELFCARE_DB_PASSWORD"
