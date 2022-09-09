CREATE SEQUENCE task_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE task
(
    id            BIGINT       DEFAULT NEXTVAL('task_seq'::REGCLASS) PRIMARY KEY,
    payload       TEXT                                                 NOT NULL,
    status        VARCHAR(50)  DEFAULT 'UBEHANDLET'::CHARACTER VARYING NOT NULL,
    versjon       BIGINT       DEFAULT 0,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    type          VARCHAR(100)                                         NOT NULL,
    metadata      VARCHAR(4000),
    trigger_tid   TIMESTAMP    DEFAULT LOCALTIMESTAMP,
    avvikstype    VARCHAR(50)
);

ALTER SEQUENCE task_seq OWNED BY task.id;

CREATE INDEX task_status_idx
    ON task (status);

CREATE SEQUENCE task_logg_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE task_logg
(
    id            BIGINT       DEFAULT NEXTVAL('task_logg_seq'::REGCLASS) PRIMARY KEY,
    task_id       BIGINT REFERENCES task,
    type          VARCHAR(50)  NOT NULL,
    node          VARCHAR(100) NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    melding       TEXT,
    endret_av     VARCHAR(100) DEFAULT 'VL'::CHARACTER VARYING
);

CREATE INDEX task_logg_task_id_idx
    ON task_logg (task_id);

CREATE TABLE aktoer
(
    aktoer_id     VARCHAR PRIMARY KEY,
    versjon       BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

CREATE TABLE personident
(
    foedselsnummer VARCHAR PRIMARY KEY UNIQUE,
    fk_aktoer_id   VARCHAR REFERENCES aktoer ON UPDATE CASCADE,
    aktiv          BOOLEAN      DEFAULT FALSE                   NOT NULL,
    gjelder_til    TIMESTAMP(3),
    versjon        BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av   VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid  TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av      VARCHAR,
    endret_tid     TIMESTAMP(3)
);

CREATE UNIQUE INDEX uidx_personident_aktoer_id
    ON personident (fk_aktoer_id)
    WHERE (aktiv = TRUE);

CREATE UNIQUE INDEX uidx_personident_foedselsnummer_id
    ON personident (foedselsnummer);

CREATE INDEX personident_aktoer_id_alle_idx
    ON personident (fk_aktoer_id);

CREATE TABLE fagsak
(
    id            BIGINT PRIMARY KEY,
    versjon       BIGINT       DEFAULT 0,
    opprettet_av  VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR(512),
    endret_tid    TIMESTAMP(3),
    status        VARCHAR(50)  DEFAULT 'OPPRETTET'::CHARACTER VARYING,
    arkivert      BOOLEAN      DEFAULT FALSE NOT NULL,
    fk_aktoer_id  VARCHAR REFERENCES aktoer ON UPDATE CASCADE
);

CREATE INDEX fagsak_fk_idx
    ON fagsak (fk_aktoer_id);

CREATE UNIQUE INDEX uidx_fagsak_type_aktoer_ikke_arkivert
    ON fagsak (fk_aktoer_id)
    WHERE (arkivert = FALSE);

CREATE TABLE behandling
(
    id                          BIGINT PRIMARY KEY,
    fk_fagsak_id                BIGINT REFERENCES fagsak,
    versjon                     BIGINT       DEFAULT 0,
    opprettet_av                VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING,
    opprettet_tid               TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    endret_av                   VARCHAR(512),
    endret_tid                  TIMESTAMP(3),
    behandling_type             VARCHAR(50),
    aktiv                       BOOLEAN      DEFAULT TRUE,
    status                      VARCHAR(50)  DEFAULT 'OPPRETTET'::CHARACTER VARYING,
    kategori                    VARCHAR(50)  DEFAULT 'NATIONAL'::CHARACTER VARYING,
    underkategori               VARCHAR(50)  DEFAULT 'ORDINÆR'::CHARACTER VARYING,
    opprettet_aarsak            VARCHAR      DEFAULT 'MANUELL'::CHARACTER VARYING,
    skal_behandles_automatisk   BOOLEAN      DEFAULT FALSE,
    resultat                    VARCHAR      DEFAULT 'IKKE_VURDERT'::CHARACTER VARYING NOT NULL,
    overstyrt_endringstidspunkt TIMESTAMP(3)
);

CREATE INDEX behandling_fk_fagsak_id_idx
    ON behandling (fk_fagsak_id);

CREATE UNIQUE INDEX uidx_behandling_01
    ON behandling ((
                       CASE
                           WHEN aktiv = TRUE THEN fk_fagsak_id
                           ELSE NULL::BIGINT
                           END), (
                       CASE
                           WHEN aktiv = TRUE THEN aktiv
                           ELSE NULL::BOOLEAN
                           END));

CREATE TABLE arbeidsfordeling_pa_behandling
(
    id                     BIGINT PRIMARY KEY,
    fk_behandling_id       BIGINT REFERENCES behandling,
    behandlende_enhet_id   VARCHAR NOT NULL,
    behandlende_enhet_navn VARCHAR NOT NULL,
    manuelt_overstyrt      BOOLEAN NOT NULL
);

CREATE TABLE gr_personopplysninger
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES behandling,
    versjon          BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av     VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av        VARCHAR(512),
    endret_tid       TIMESTAMP(3),
    aktiv            BOOLEAN      DEFAULT TRUE                    NOT NULL
);

CREATE INDEX gr_personopplysninger_fk_behandling_id_idx
    ON gr_personopplysninger (fk_behandling_id);

CREATE UNIQUE INDEX uidx_gr_personopplysninger_01
    ON gr_personopplysninger ((
                                  CASE
                                      WHEN aktiv = TRUE THEN fk_behandling_id
                                      ELSE NULL::BIGINT
                                      END), (
                                  CASE
                                      WHEN aktiv = TRUE THEN aktiv
                                      ELSE NULL::BOOLEAN
                                      END));

CREATE TABLE po_person
(
    id                          BIGINT PRIMARY KEY,
    fk_gr_personopplysninger_id BIGINT REFERENCES gr_personopplysninger,
    type                        VARCHAR(10)                                  NOT NULL,
    opprettet_av                VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid               TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP       NOT NULL,
    endret_av                   VARCHAR(512),
    versjon                     BIGINT       DEFAULT 0                       NOT NULL,
    endret_tid                  TIMESTAMP(3),
    foedselsdato                TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    fk_aktoer_id                VARCHAR(50) REFERENCES aktoer ON UPDATE CASCADE,
    navn                        VARCHAR      DEFAULT ''::CHARACTER VARYING,
    kjoenn                      VARCHAR      DEFAULT 'UKJENT'::CHARACTER VARYING,
    maalform                    VARCHAR(2)   DEFAULT 'NB'::CHARACTER VARYING NOT NULL
);

CREATE INDEX po_person_fk_gr_personopplysninger_id_idx
    ON po_person (fk_gr_personopplysninger_id);

CREATE INDEX po_person_fk_idx
    ON po_person (fk_aktoer_id);

CREATE TABLE vedtak
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES behandling,
    versjon          BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av     VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    vedtaksdato      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    endret_av        VARCHAR(512),
    endret_tid       TIMESTAMP(3),
    aktiv            BOOLEAN      DEFAULT TRUE,
    stonad_brev_pdf  BYTEA
);

CREATE INDEX behandling_vedtak_fk_behandling_id_idx
    ON vedtak (fk_behandling_id);

CREATE UNIQUE INDEX uidx_behandling_vedtak_01
    ON vedtak ((
                   CASE
                       WHEN aktiv = TRUE THEN fk_behandling_id
                       ELSE NULL::BIGINT
                       END), (
                   CASE
                       WHEN aktiv = TRUE THEN aktiv
                       ELSE NULL::BOOLEAN
                       END));

CREATE TABLE logg
(
    id               BIGINT PRIMARY KEY,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    fk_behandling_id BIGINT REFERENCES behandling,
    type             VARCHAR                                      NOT NULL,
    tittel           VARCHAR                                      NOT NULL,
    rolle            VARCHAR                                      NOT NULL,
    tekst            TEXT                                         NOT NULL
);

CREATE INDEX logg_fk_behandling_id_idx
    ON logg (fk_behandling_id);

CREATE TABLE gr_soknad
(
    id               BIGINT PRIMARY KEY,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    fk_behandling_id BIGINT REFERENCES behandling,
    soknad           TEXT                                         NOT NULL,
    aktiv            BOOLEAN      DEFAULT TRUE                    NOT NULL
);

CREATE INDEX gr_soknad_fk_behandling_id_idx
    ON gr_soknad (fk_behandling_id);

CREATE UNIQUE INDEX uidx_gr_soknad_01
    ON gr_soknad ((
                      CASE
                          WHEN aktiv = TRUE THEN fk_behandling_id
                          ELSE NULL::BIGINT
                          END), (
                      CASE
                          WHEN aktiv = TRUE THEN aktiv
                          ELSE NULL::BOOLEAN
                          END));

CREATE TABLE tilkjent_ytelse
(
    id                 BIGINT PRIMARY KEY,
    fk_behandling_id   BIGINT REFERENCES behandling,
    stonad_fom         TIMESTAMP,
    stonad_tom         TIMESTAMP,
    opprettet_dato     TIMESTAMP                           NOT NULL,
    opphor_fom         TIMESTAMP,
    utbetalingsoppdrag TEXT,
    endret_dato        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX beregning_resultat_fk_behandling_id_idx
    ON tilkjent_ytelse (fk_behandling_id);

CREATE INDEX tilkjent_ytelse_utbetalingsoppdrag_not_null_idx
    ON tilkjent_ytelse (utbetalingsoppdrag)
    WHERE (utbetalingsoppdrag IS NOT NULL);

CREATE TABLE andel_tilkjent_ytelse
(
    id                              BIGINT PRIMARY KEY,
    fk_behandling_id                BIGINT REFERENCES behandling,
    versjon                         BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av                    VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    stonad_fom                      TIMESTAMP(3)                                 NOT NULL,
    stonad_tom                      TIMESTAMP(3)                                 NOT NULL,
    type                            VARCHAR(50)                                  NOT NULL,
    kalkulert_utbetalingsbelop      NUMERIC,
    endret_av                       VARCHAR(512),
    endret_tid                      TIMESTAMP(3),
    tilkjent_ytelse_id              BIGINT REFERENCES tilkjent_ytelse ON DELETE CASCADE,
    periode_offset                  BIGINT,
    forrige_periode_offset          BIGINT,
    kilde_behandling_id             BIGINT REFERENCES behandling,
    prosent                         NUMERIC                                      NOT NULL,
    sats                            BIGINT                                       NOT NULL,
    fk_aktoer_id                    VARCHAR REFERENCES aktoer ON UPDATE CASCADE,
    nasjonalt_periodebelop          NUMERIC,
    differanseberegnet_periodebelop NUMERIC
);

CREATE INDEX andel_tilkjent_ytelse_fk_behandling_id_idx
    ON andel_tilkjent_ytelse (fk_behandling_id);

CREATE INDEX andel_tilkjent_ytelse_fk_idx
    ON andel_tilkjent_ytelse (kilde_behandling_id);

CREATE INDEX andel_tilkjent_ytelse_fk_tilkjent_idx
    ON andel_tilkjent_ytelse (tilkjent_ytelse_id);

CREATE INDEX andel_tilkjent_ytelse_fk_aktoer_idx
    ON andel_tilkjent_ytelse (fk_aktoer_id);

CREATE INDEX aty_type_idx
    ON andel_tilkjent_ytelse (type);

CREATE TABLE vilkaarsvurdering
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES behandling,
    aktiv            BOOLEAN      DEFAULT TRUE                    NOT NULL,
    versjon          BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3),
    samlet_resultat  VARCHAR,
    ytelse_personer  TEXT         DEFAULT ''::TEXT
);

CREATE INDEX vilkaarsvurdering_fk_idx
    ON vilkaarsvurdering (fk_behandling_id);

CREATE TABLE person_resultat
(
    id                      BIGINT PRIMARY KEY,
    fk_vilkaarsvurdering_id BIGINT REFERENCES vilkaarsvurdering,
    versjon                 BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3),
    fk_aktoer_id            VARCHAR REFERENCES aktoer ON UPDATE CASCADE
);

CREATE TABLE vilkar_resultat
(
    id                                 BIGINT PRIMARY KEY,
    vilkar                             VARCHAR(50)                                  NOT NULL,
    resultat                           VARCHAR(50)                                  NOT NULL,
    regel_input                        TEXT,
    regel_output                       TEXT,
    versjon                            BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av                       VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                          VARCHAR(512),
    endret_tid                         TIMESTAMP(3),
    fk_person_resultat_id              BIGINT REFERENCES person_resultat,
    begrunnelse                        TEXT,
    periode_fom                        TIMESTAMP(3) DEFAULT NULL::TIMESTAMP WITHOUT TIME ZONE,
    periode_tom                        TIMESTAMP(3) DEFAULT NULL::TIMESTAMP WITHOUT TIME ZONE,
    fk_behandling_id                   BIGINT REFERENCES behandling,
    evaluering_aarsak                  TEXT         DEFAULT ''::TEXT,
    er_automatisk_vurdert              BOOLEAN      DEFAULT FALSE                   NOT NULL,
    er_eksplisitt_avslag_paa_soknad    BOOLEAN,
    vedtak_begrunnelse_spesifikasjoner TEXT         DEFAULT ''::TEXT,
    vurderes_etter                     VARCHAR,
    utdypende_vilkarsvurderinger       VARCHAR
);

CREATE INDEX vilkar_resultat_fk_idx
    ON vilkar_resultat (fk_behandling_id);

CREATE INDEX vilkar_resultat_fk_personr_idx
    ON vilkar_resultat (fk_person_resultat_id);

CREATE INDEX person_resultat_fk_idx
    ON person_resultat (fk_vilkaarsvurdering_id);

CREATE INDEX person_resultat_fk_aktoer_idx
    ON person_resultat (fk_aktoer_id);

CREATE TABLE oppgave
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES behandling,
    gsak_id          VARCHAR   NOT NULL,
    type             VARCHAR   NOT NULL,
    ferdigstilt      BOOLEAN   NOT NULL,
    opprettet_tid    TIMESTAMP NOT NULL
);

CREATE INDEX oppgave_fk_idx
    ON oppgave (fk_behandling_id);

CREATE TABLE totrinnskontroll
(
    id                 BIGINT PRIMARY KEY,
    fk_behandling_id   BIGINT REFERENCES behandling,
    versjon            BIGINT       DEFAULT 0                           NOT NULL,
    opprettet_av       VARCHAR      DEFAULT 'VL'::CHARACTER VARYING     NOT NULL,
    opprettet_tid      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP              NOT NULL,
    endret_av          VARCHAR,
    endret_tid         TIMESTAMP(3),
    aktiv              BOOLEAN      DEFAULT TRUE                        NOT NULL,
    saksbehandler      VARCHAR                                          NOT NULL,
    beslutter          VARCHAR,
    godkjent           BOOLEAN      DEFAULT TRUE,
    saksbehandler_id   VARCHAR      DEFAULT 'ukjent'::CHARACTER VARYING NOT NULL,
    beslutter_id       VARCHAR,
    kontrollerte_sider TEXT         DEFAULT ''::TEXT
);

CREATE INDEX totrinnskontroll_fk_behandling_id_idx
    ON totrinnskontroll (fk_behandling_id);

CREATE UNIQUE INDEX uidx_totrinnskontroll_01
    ON totrinnskontroll ((
                             CASE
                                 WHEN aktiv = TRUE THEN fk_behandling_id
                                 ELSE NULL::BIGINT
                                 END), (
                             CASE
                                 WHEN aktiv = TRUE THEN aktiv
                                 ELSE NULL::BOOLEAN
                                 END));


CREATE TABLE po_bostedsadresse
(
    id                BIGINT PRIMARY KEY,
    type              VARCHAR(20)                                  NOT NULL,
    bostedskommune    VARCHAR,
    husnummer         VARCHAR,
    husbokstav        VARCHAR,
    bruksenhetsnummer VARCHAR,
    adressenavn       VARCHAR,
    kommunenummer     VARCHAR,
    tilleggsnavn      VARCHAR,
    postnummer        VARCHAR,
    opprettet_av      VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP       NOT NULL,
    endret_av         VARCHAR,
    versjon           BIGINT       DEFAULT 0                       NOT NULL,
    endret_tid        TIMESTAMP(3),
    matrikkel_id      BIGINT,
    fom               DATE,
    tom               DATE,
    fk_po_person_id   BIGINT REFERENCES po_person
);


CREATE INDEX po_bostedsadresse_fk_idx
    ON po_bostedsadresse (fk_po_person_id);


CREATE TABLE journalpost
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES behandling,
    journalpost_id   VARCHAR                                      NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    type             VARCHAR
);


CREATE INDEX journalpost_fk_behandling_id_idx
    ON journalpost (fk_behandling_id);


CREATE TABLE po_statsborgerskap
(
    id              BIGINT PRIMARY KEY,
    fk_po_person_id BIGINT REFERENCES po_person,
    landkode        VARCHAR(3)   DEFAULT 'XUK'::CHARACTER VARYING    NOT NULL,
    fom             DATE,
    tom             DATE,
    opprettet_av    VARCHAR      DEFAULT 'VL'::CHARACTER VARYING     NOT NULL,
    opprettet_tid   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP              NOT NULL,
    endret_av       VARCHAR,
    endret_tid      TIMESTAMP(3),
    versjon         BIGINT       DEFAULT 0                           NOT NULL,
    medlemskap      VARCHAR      DEFAULT 'UKJENT'::CHARACTER VARYING NOT NULL
);


CREATE INDEX po_statsborgerskap_fk_idx
    ON po_statsborgerskap (fk_po_person_id);


CREATE TABLE po_opphold
(
    id              BIGINT PRIMARY KEY,
    fk_po_person_id BIGINT REFERENCES po_person,
    type            VARCHAR                                      NOT NULL,
    fom             DATE,
    tom             DATE,
    opprettet_av    VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av       VARCHAR,
    endret_tid      TIMESTAMP(3),
    versjon         BIGINT       DEFAULT 0                       NOT NULL
);


CREATE INDEX po_opphold_fk_idx
    ON po_opphold (fk_po_person_id);

CREATE TABLE po_arbeidsforhold
(
    id                BIGINT PRIMARY KEY,
    fk_po_person_id   BIGINT REFERENCES po_person,
    arbeidsgiver_id   VARCHAR,
    arbeidsgiver_type VARCHAR,
    fom               DATE,
    tom               DATE,
    opprettet_av      VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3),
    versjon           BIGINT       DEFAULT 0                       NOT NULL
);

CREATE INDEX po_arbeidsforhold_fk_idx
    ON po_arbeidsforhold (fk_po_person_id);

CREATE TABLE po_bostedsadresseperiode
(
    id              BIGINT PRIMARY KEY,
    fk_po_person_id BIGINT REFERENCES po_person,
    fom             DATE,
    tom             DATE,
    opprettet_av    VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av       VARCHAR,
    endret_tid      TIMESTAMP(3),
    versjon         BIGINT       DEFAULT 0                       NOT NULL
);

CREATE INDEX po_bostedsadresseperiode_fk_idx
    ON po_bostedsadresseperiode (fk_po_person_id);

CREATE TABLE behandling_steg_tilstand
(
    id                     BIGINT PRIMARY KEY,
    fk_behandling_id       BIGINT REFERENCES behandling,
    behandling_steg        VARCHAR                                               NOT NULL,
    behandling_steg_status VARCHAR      DEFAULT 'IKKE_UTFØRT'::CHARACTER VARYING NOT NULL,
    versjon                BIGINT       DEFAULT 0                                NOT NULL,
    opprettet_av           VARCHAR      DEFAULT 'VL'::CHARACTER VARYING          NOT NULL,
    opprettet_tid          TIMESTAMP(3) DEFAULT LOCALTIMESTAMP                   NOT NULL,
    endret_av              VARCHAR,
    endret_tid             TIMESTAMP(3)
);

CREATE INDEX behandling_steg_tilstand_fk_idx
    ON behandling_steg_tilstand (fk_behandling_id);

CREATE TABLE annen_vurdering
(
    id                    BIGINT PRIMARY KEY,
    fk_person_resultat_id BIGINT REFERENCES person_resultat,
    resultat              VARCHAR                                      NOT NULL,
    type                  VARCHAR                                      NOT NULL,
    begrunnelse           TEXT,
    versjon               BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av          VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid         TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av             VARCHAR,
    endret_tid            TIMESTAMP(3)
);

CREATE INDEX annen_vurdering_fk_idx
    ON annen_vurdering (fk_person_resultat_id);

CREATE TABLE okonomi_simulering_mottaker
(
    id               BIGINT PRIMARY KEY,
    mottaker_nummer  VARCHAR(50),
    mottaker_type    VARCHAR(50),
    opprettet_av     VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av        VARCHAR(512),
    endret_tid       TIMESTAMP(3),
    versjon          BIGINT       DEFAULT 0,
    fk_behandling_id BIGINT REFERENCES behandling
);

CREATE INDEX okonomi_simulering_mottaker_fk_idx
    ON okonomi_simulering_mottaker (fk_behandling_id);

CREATE TABLE okonomi_simulering_postering
(
    id                                BIGINT PRIMARY KEY,
    fk_okonomi_simulering_mottaker_id BIGINT REFERENCES okonomi_simulering_mottaker ON DELETE CASCADE,
    fag_omraade_kode                  VARCHAR(50),
    fom                               TIMESTAMP(3),
    tom                               TIMESTAMP(3),
    betaling_type                     VARCHAR(50),
    belop                             BIGINT,
    postering_type                    VARCHAR(50),
    forfallsdato                      TIMESTAMP(3),
    uten_inntrekk                     BOOLEAN,
    opprettet_av                      VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                     TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                         VARCHAR(512),
    endret_tid                        TIMESTAMP(3),
    versjon                           BIGINT       DEFAULT 0
);

CREATE INDEX vedtak_simulering_postering_fk_vedtak_simulering_mottaker_i_idx
    ON okonomi_simulering_postering (fk_okonomi_simulering_mottaker_id);

CREATE TABLE tilbakekreving
(
    id                           BIGINT PRIMARY KEY,
    valg                         VARCHAR                                   NOT NULL,
    varsel                       TEXT,
    begrunnelse                  TEXT                                      NOT NULL,
    tilbakekrevingsbehandling_id TEXT,
    opprettet_av                 VARCHAR   DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                TIMESTAMP DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                    VARCHAR,
    endret_tid                   TIMESTAMP(3),
    versjon                      BIGINT    DEFAULT 0,
    fk_behandling_id             BIGINT REFERENCES behandling
);

CREATE INDEX tilbakekreving_fk_idx
    ON tilbakekreving (fk_behandling_id);

CREATE TABLE vedtaksperiode
(
    id            BIGINT PRIMARY KEY,
    fk_vedtak_id  BIGINT REFERENCES vedtak,
    fom           TIMESTAMP,
    tom           TIMESTAMP,
    type          VARCHAR                                   NOT NULL,
    opprettet_av  VARCHAR   DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid TIMESTAMP DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    versjon       BIGINT    DEFAULT 0
);

CREATE INDEX vedtaksperiode_fk_vedtak_id_idx
    ON vedtaksperiode (fk_vedtak_id);

CREATE TABLE vedtaksbegrunnelse
(
    id                               BIGINT PRIMARY KEY,
    fk_vedtaksperiode_id             BIGINT REFERENCES vedtaksperiode ON DELETE CASCADE,
    vedtak_begrunnelse_spesifikasjon VARCHAR NOT NULL
);

CREATE INDEX vedtaksbegrunnelse_fk_vedtaksperiode_id_idx
    ON vedtaksbegrunnelse (fk_vedtaksperiode_id);

CREATE TABLE vedtaksbegrunnelse_fritekst
(
    id                   BIGINT PRIMARY KEY,
    fk_vedtaksperiode_id BIGINT REFERENCES vedtaksperiode ON DELETE CASCADE,
    fritekst             TEXT DEFAULT ''::TEXT NOT NULL
);

CREATE INDEX vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_idx
    ON vedtaksbegrunnelse_fritekst (fk_vedtaksperiode_id);

CREATE TABLE po_sivilstand
(
    id              BIGINT PRIMARY KEY,
    fk_po_person_id BIGINT REFERENCES po_person,
    fom             DATE,
    type            VARCHAR                                      NOT NULL,
    opprettet_av    VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av       VARCHAR,
    endret_tid      TIMESTAMP(3),
    versjon         BIGINT       DEFAULT 0                       NOT NULL
);

CREATE INDEX po_sivilstand_fk_idx
    ON po_sivilstand (fk_po_person_id);


CREATE TABLE endret_utbetaling_andel
(
    id                                 BIGINT PRIMARY KEY,
    fk_behandling_id                   BIGINT REFERENCES behandling,
    fk_po_person_id                    BIGINT REFERENCES po_person,
    fom                                TIMESTAMP(3),
    tom                                TIMESTAMP(3),
    prosent                            NUMERIC,
    aarsak                             VARCHAR,
    begrunnelse                        TEXT,
    versjon                            BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av                       VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                          VARCHAR,
    endret_tid                         TIMESTAMP(3),
    vedtak_begrunnelse_spesifikasjoner TEXT         DEFAULT ''::TEXT,
    avtaletidspunkt_delt_bosted        TIMESTAMP(3),
    soknadstidspunkt                   TIMESTAMP(3)
);

CREATE INDEX endret_utbetaling_andel_fk_behandling_id_idx
    ON endret_utbetaling_andel (fk_behandling_id);

CREATE INDEX endret_utbetaling_andel_fk_idx
    ON endret_utbetaling_andel (fk_po_person_id);

CREATE TABLE andel_til_endret_andel
(
    fk_andel_tilkjent_ytelse_id   BIGINT NOT NULL
        REFERENCES andel_tilkjent_ytelse
            ON DELETE CASCADE,
    fk_endret_utbetaling_andel_id BIGINT NOT NULL
        REFERENCES endret_utbetaling_andel,
    PRIMARY KEY (fk_andel_tilkjent_ytelse_id, fk_endret_utbetaling_andel_id)
);

CREATE TABLE sett_paa_vent
(
    id                BIGINT PRIMARY KEY,
    fk_behandling_id  BIGINT REFERENCES behandling,
    versjon           BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av      VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    frist             TIMESTAMP(3)                                 NOT NULL,
    aktiv             BOOLEAN      DEFAULT FALSE                   NOT NULL,
    aarsak            VARCHAR                                      NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3),
    tid_tatt_av_vent  TIMESTAMP(3),
    tid_satt_paa_vent TIMESTAMP(3) DEFAULT NOW()                   NOT NULL
);

CREATE INDEX sett_paa_vent_fk_behandling_id_idx
    ON sett_paa_vent (fk_behandling_id);

CREATE UNIQUE INDEX uidx_sett_paa_vent_aktiv
    ON sett_paa_vent (fk_behandling_id, aktiv)
    WHERE (aktiv = TRUE);

CREATE TABLE po_doedsfall
(
    id                   BIGINT PRIMARY KEY,
    fk_po_person_id      BIGINT REFERENCES po_person,
    versjon              BIGINT       DEFAULT 0                       NOT NULL,
    doedsfall_dato       TIMESTAMP(3)                                 NOT NULL,
    doedsfall_adresse    VARCHAR,
    doedsfall_postnummer VARCHAR,
    doedsfall_poststed   VARCHAR,
    opprettet_av         VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid        TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av            VARCHAR,
    endret_tid           TIMESTAMP(3)
);

CREATE INDEX po_doedsfall_fk_po_person_id_idx
    ON po_doedsfall (fk_po_person_id);

CREATE TABLE behandling_soknadsinfo
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES behandling,
    mottatt_dato     TIMESTAMP(3)                                 NOT NULL,
    versjon          BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);

CREATE INDEX behandling_soknadsinfo_fk_behandling_id_idx
    ON behandling_soknadsinfo (fk_behandling_id);

CREATE TABLE kompetanse
(
    id                              BIGINT PRIMARY KEY,
    fk_behandling_id                BIGINT REFERENCES behandling,
    fom                             TIMESTAMP(3),
    tom                             TIMESTAMP(3),
    versjon                         BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av                    VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                       VARCHAR,
    endret_tid                      TIMESTAMP(3),
    soekers_aktivitet               VARCHAR,
    annen_forelderes_aktivitet      VARCHAR,
    annen_forelderes_aktivitetsland VARCHAR,
    barnets_bostedsland             VARCHAR,
    resultat                        VARCHAR,
    sokers_aktivitetsland           TEXT
);

CREATE INDEX kompetanse_fk_behandling_id_idx
    ON kompetanse (fk_behandling_id);

CREATE TABLE aktoer_til_kompetanse
(
    fk_kompetanse_id BIGINT REFERENCES kompetanse,
    fk_aktoer_id     VARCHAR REFERENCES aktoer ON UPDATE CASCADE,
    PRIMARY KEY (fk_kompetanse_id, fk_aktoer_id)
);

CREATE TABLE valutakurs
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES behandling,
    fom              TIMESTAMP(3),
    tom              TIMESTAMP(3),
    valutakursdato   TIMESTAMP(3) DEFAULT NULL::TIMESTAMP WITHOUT TIME ZONE,
    valutakode       VARCHAR,
    kurs             NUMERIC,
    versjon          BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);

CREATE INDEX valutakurs_fk_behandling_id_idx
    ON valutakurs (fk_behandling_id);

CREATE TABLE aktoer_til_valutakurs
(
    fk_valutakurs_id BIGINT REFERENCES valutakurs,
    fk_aktoer_id     VARCHAR REFERENCES aktoer ON UPDATE CASCADE,
    PRIMARY KEY (fk_valutakurs_id, fk_aktoer_id)
);

CREATE TABLE utenlandsk_periodebeloep
(
    id                         BIGINT PRIMARY KEY,
    fk_behandling_id           BIGINT REFERENCES behandling,
    fom                        TIMESTAMP(3),
    tom                        TIMESTAMP(3),
    intervall                  VARCHAR,
    valutakode                 VARCHAR,
    beloep                     NUMERIC,
    versjon                    BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av               VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid              TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                  VARCHAR,
    endret_tid                 TIMESTAMP(3),
    utbetalingsland            VARCHAR,
    kalkulert_maanedlig_beloep NUMERIC
);

CREATE INDEX utenlandsk_periodebeloep_fk_behandling_id_idx
    ON utenlandsk_periodebeloep (fk_behandling_id);

CREATE TABLE aktoer_til_utenlandsk_periodebeloep
(
    fk_utenlandsk_periodebeloep_id BIGINT REFERENCES utenlandsk_periodebeloep,
    fk_aktoer_id                   VARCHAR REFERENCES aktoer ON UPDATE CASCADE,
    PRIMARY KEY (fk_utenlandsk_periodebeloep_id, fk_aktoer_id)
);

CREATE TABLE verge
(
    id               BIGINT PRIMARY KEY,
    ident            VARCHAR,
    fk_behandling_id BIGINT REFERENCES behandling,
    versjon          BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av     VARCHAR(20)  DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av        VARCHAR(20),
    endret_tid       TIMESTAMP(3)
);

CREATE UNIQUE INDEX uidx_verge_behandling_id
    ON verge (fk_behandling_id);

CREATE TABLE korrigert_etterbetaling
(
    id               BIGINT PRIMARY KEY,
    aarsak           VARCHAR                                      NOT NULL,
    begrunnelse      VARCHAR,
    belop            BIGINT                                       NOT NULL,
    aktiv            BOOLEAN                                      NOT NULL,
    fk_behandling_id BIGINT REFERENCES behandling,
    versjon          BIGINT       DEFAULT 0                       NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);

CREATE UNIQUE INDEX uidx_korrigert_etterbetaling_fk_behandling_id_aktiv
    ON korrigert_etterbetaling (fk_behandling_id)
    WHERE (aktiv = TRUE);

CREATE INDEX korrigert_etterbetaling_fk_behandling_id_idx
    ON korrigert_etterbetaling (fk_behandling_id);