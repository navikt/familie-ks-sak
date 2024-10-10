CREATE TABLE kompensasjon_andel
(
    id                                 BIGINT PRIMARY KEY,
    fk_behandling_id                   BIGINT REFERENCES behandling,
    fk_po_person_id                    BIGINT REFERENCES po_person,
    fom                                TIMESTAMP(3),
    tom                                TIMESTAMP(3),
    prosent                            NUMERIC,
    versjon                            BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av                       VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                          VARCHAR,
    endret_tid                         TIMESTAMP(3)
);

CREATE INDEX kompensasjon_andel_fk_behandling_id_idx
    ON kompensasjon_andel (fk_behandling_id);

CREATE INDEX kompensasjon_andel_fk_idx
    ON kompensasjon_andel (fk_po_person_id);

CREATE SEQUENCE kompensasjon_andel_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;