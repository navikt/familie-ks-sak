CREATE TABLE barnehage_bydel
(
    id                   BIGINT       NOT NULL PRIMARY KEY,
    barnehage_org_nummer VARCHAR      NOT NULL UNIQUE,
    bydel_org_nummer     VARCHAR      NOT NULL,
    versjon              BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av         VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid        TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av            VARCHAR,
    endret_tid           TIMESTAMP(3)
);

CREATE SEQUENCE barnehage_bydel_seq INCREMENT BY 50 START WITH 1 NO CYCLE;
