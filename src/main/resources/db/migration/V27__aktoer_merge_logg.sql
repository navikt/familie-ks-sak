CREATE TABLE AKTOER_MERGE_LOGG
(
    id                  BIGSERIAL PRIMARY KEY,
    fk_fagsak_id        BIGINT NOT NULL,
    historisk_aktoer_id VARCHAR(50),
    ny_aktoer_id        VARCHAR(50),
    merge_tid           TIMESTAMP WITHOUT TIME ZONE
);

CREATE SEQUENCE AKTOER_MERGE_LOGG_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;