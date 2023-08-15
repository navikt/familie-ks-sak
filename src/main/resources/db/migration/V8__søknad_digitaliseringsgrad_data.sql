ALTER TABLE behandling_soknadsinfo
    ADD COLUMN IF NOT EXISTS er_digital BOOLEAN NOT NULL,
    ADD COLUMN IF NOT EXISTS journalpost_id VARCHAR NOT NULL;

CREATE INDEX journalpost_id_idx ON behandling_soknadsinfo (journalpost_id);
