-- Run once, as the PostgreSQL administrator:
--   psql -h localhost -U postgres -v bss_pw=Pw1 -v sc_pw=Pw2 -f 00-create-databases.sql
-- The passwords are given on the command line, so they are not in this file.
CREATE ROLE bss_app      LOGIN PASSWORD :'bss_pw';
CREATE ROLE selfcare_app LOGIN PASSWORD :'sc_pw';
CREATE DATABASE bss_core OWNER bss_app;
CREATE DATABASE selfcare OWNER selfcare_app;
REVOKE ALL ON DATABASE bss_core FROM PUBLIC;
REVOKE ALL ON DATABASE selfcare FROM PUBLIC;
