ALTER TABLE behandling
    ADD COLUMN aktivert_tid TIMESTAMP DEFAULT localtimestamp NOT NULL;