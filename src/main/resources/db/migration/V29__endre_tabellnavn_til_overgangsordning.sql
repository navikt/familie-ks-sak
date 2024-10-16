ALTER TABLE kompensasjon_andel
    RENAME TO overgangsordning_andel;

ALTER TABLE overgangsordning_andel
    RENAME CONSTRAINT kompensasjon_andel_pkey TO overgangsordning_andel_pkey;

ALTER TABLE overgangsordning_andel
    RENAME CONSTRAINT kompensasjon_andel_fk_behandling_id_fkey TO overgangsordning_andel_fk_behandling_id_fkey;

ALTER TABLE overgangsordning_andel
    RENAME CONSTRAINT kompensasjon_andel_fk_po_person_id_fkey TO overgangsordning_andel_fk_po_person_id_fkey;

ALTER INDEX kompensasjon_andel_fk_behandling_id_idx
    RENAME TO overgangsordning_andel_fk_behandling_id_idx;

ALTER INDEX kompensasjon_andel_fk_idx
    RENAME TO overgangsordning_andel_fk_idx;

ALTER SEQUENCE kompensasjon_andel_seq
    RENAME TO overgangsordning_andel_seq;
