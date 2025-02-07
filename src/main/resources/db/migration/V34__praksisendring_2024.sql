CREATE TABLE praksisendring_2024
(
    id                                 BIGINT PRIMARY KEY,
    fk_fagsak_id                       BIGINT REFERENCES fagsak NOT NULL,
    fk_po_person_id                    BIGINT REFERENCES po_person NOT NULL,
    fk_aktoer_id                       VARCHAR REFERENCES AKTOER (AKTOER_ID) NOT NULL,
    barnehagestart                     TIMESTAMP(3) not null ,
    versjon                            BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av                       VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                          VARCHAR,
    endret_tid                         TIMESTAMP(3)
);

CREATE INDEX praksisendring_2024_fk_fagsak_id_idx
    ON praksisendring_2024 (fk_fagsak_id);

CREATE SEQUENCE praksisendring_2024_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;