ALTER TABLE overgangsordning_andel
    ADD COLUMN fk_aktoer_id VARCHAR REFERENCES aktoer (aktoer_id) ON UPDATE CASCADE;

UPDATE overgangsordning_andel oa
SET fk_aktoer_id = p.fk_aktoer_id
FROM po_person p
WHERE p.id = oa.fk_po_person_id;

CREATE INDEX overgangsordning_andel_fk_aktoer_id_idx ON overgangsordning_andel (fk_aktoer_id);

DROP INDEX IF EXISTS overgangsordning_andel_fk_idx;

ALTER TABLE overgangsordning_andel
    DROP COLUMN fk_po_person_id;
