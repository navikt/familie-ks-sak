CREATE TABLE BARNEHAGEBARN
(
    ID              UUID PRIMARY KEY,
    IDENT           VARCHAR     NOT NULL,
    FOM             DATE        NOT NULL,
    TOM             DATE,
    ANTALL_TIMER_I_BARNEHAGE NUMERIC,
    ENDRINGSTYPE    VARCHAR NOT NULL,
    KOMMUNE_NAVN    VARCHAR NOT NULL,
    KOMMUNE_NR      VARCHAR NOT NULL,
     -- Base entitet felter
    versjon          BIGINT       DEFAULT 0                 NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP    NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);

CREATE TABLE BARNEHAGELISTE_MOTTATT_ARKIV
(
    ID          UUID PRIMARY KEY,
    MELDING_ID  VARCHAR      NOT NULL,
    MELDING     TEXT,
    MOTTATT_TID TIMESTAMP(3) NOT NULL,
    -- Base entitet felter
    versjon          BIGINT       DEFAULT 0                 NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP    NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);