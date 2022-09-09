CREATE SEQUENCE task_seq INCREMENT BY 50;
CREATE SEQUENCE task_logg_seq INCREMENT BY 50;

create table task
(
    id            bigint       default nextval('task_seq'::regclass)   not null
        constraint task_pkey
            primary key,
    payload       text                                                 not null,
    status        varchar(50)  default 'UBEHANDLET'::character varying not null,
    versjon       bigint       default 0,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP,
    type          varchar(100)                                         not null,
    metadata      varchar(4000),
    trigger_tid   timestamp    default LOCALTIMESTAMP,
    avvikstype    varchar(50)
);

create index task_status_idx
    on task (status);

create table task_logg
(
    id            bigint       default nextval('task_logg_seq'::regclass) not null
        constraint task_logg_pkey
            primary key,
    task_id       bigint                                                  not null
        constraint task_logg_task_id_fkey
            references task,
    type          varchar(50)                                             not null,
    node          varchar(100)                                            not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP,
    melding       text,
    endret_av     varchar(100) default 'VL'::character varying
);


create index task_logg_task_id_idx
    on task_logg (task_id);



create table arbeidsfordeling_pa_behandling
(
    id                     bigint  not null
        primary key,
    fk_behandling_id       bigint  not null
        unique,
    behandlende_enhet_id   varchar not null,
    behandlende_enhet_navn varchar not null,
    manuelt_overstyrt      boolean not null
);




create table aktoer
(
    aktoer_id     varchar                                      not null
        primary key,
    versjon       bigint       default 0                       not null,
    opprettet_av  varchar      default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av     varchar,
    endret_tid    timestamp(3)
);


create table personident
(
    fk_aktoer_id   varchar                                      not null
        constraint fk_personident
            references aktoer
            on update cascade,
    foedselsnummer varchar                                      not null
        primary key
        unique,
    aktiv          boolean      default false                   not null,
    gjelder_til    timestamp(3),
    versjon        bigint       default 0                       not null,
    opprettet_av   varchar      default 'VL'::character varying not null,
    opprettet_tid  timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av      varchar,
    endret_tid     timestamp(3)
);



create unique index uidx_personident_aktoer_id
    on personident (fk_aktoer_id)
    where (aktiv = true);

create unique index uidx_personident_foedselsnummer_id
    on personident (foedselsnummer);

create index personident_aktoer_id_alle_idx
    on personident (fk_aktoer_id);



create table fagsak
(
    id                bigint                                           not null
        primary key,
    versjon           bigint       default 0,
    opprettet_av      varchar(512) default 'VL'::character varying,
    opprettet_tid     timestamp(3) default LOCALTIMESTAMP,
    endret_av         varchar(512),
    endret_tid        timestamp(3),
    status            varchar(50)  default 'OPPRETTET'::character varying,
    arkivert          boolean      default false                       not null,
    fk_aktoer_id      varchar
        constraint fagsak
            references aktoer
            on update cascade
);


create index fagsak_fk_idx
    on fagsak (fk_aktoer_id);


create unique index uidx_fagsak_type_aktoer_ikke_arkivert
    on fagsak (fk_aktoer_id)
    where (arkivert = false);


create table behandling
(
    id                          bigint                                                 not null
        primary key,
    fk_fagsak_id                bigint
        references fagsak,
    versjon                     bigint       default 0,
    opprettet_av                varchar(512) default 'VL'::character varying,
    opprettet_tid               timestamp(3) default LOCALTIMESTAMP,
    endret_av                   varchar(512),
    endret_tid                  timestamp(3),
    behandling_type             varchar(50),
    aktiv                       boolean      default true,
    status                      varchar(50)  default 'OPPRETTET'::character varying,
    kategori                    varchar(50)  default 'NATIONAL'::character varying,
    underkategori               varchar(50)  default 'ORDINÆR'::character varying,
    opprettet_aarsak            varchar      default 'MANUELL'::character varying,
    skal_behandles_automatisk   boolean      default false,
    resultat                    varchar      default 'IKKE_VURDERT'::character varying not null,
    overstyrt_endringstidspunkt timestamp(3)
);


create index behandling_fk_fagsak_id_idx
    on behandling (fk_fagsak_id);

create unique index uidx_behandling_01
    on behandling ((
                   CASE
                   WHEN aktiv = true THEN fk_fagsak_id
                   ELSE NULL::bigint
                   END), (
                       CASE
                           WHEN aktiv = true THEN aktiv
                           ELSE NULL::boolean
                           END));



create table gr_personopplysninger
(
    id               bigint                                       not null
        primary key,
    fk_behandling_id bigint                                       not null
        references behandling,
    versjon          bigint       default 0                       not null,
    opprettet_av     varchar(512) default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av        varchar(512),
    endret_tid       timestamp(3),
    aktiv            boolean      default true                    not null
);


create index gr_personopplysninger_fk_behandling_id_idx
    on gr_personopplysninger (fk_behandling_id);

create unique index uidx_gr_personopplysninger_01
    on gr_personopplysninger ((
                              CASE
                              WHEN aktiv = true THEN fk_behandling_id
                              ELSE NULL::bigint
                              END), (
                                  CASE
                                      WHEN aktiv = true THEN aktiv
                                      ELSE NULL::boolean
                                      END));


create table po_person
(
    id                          bigint                                       not null
        primary key,
    fk_gr_personopplysninger_id bigint                                       not null
        references gr_personopplysninger,
    type                        varchar(10)                                  not null,
    opprettet_av                varchar(512) default 'VL'::character varying not null,
    opprettet_tid               timestamp(3) default CURRENT_TIMESTAMP       not null,
    endret_av                   varchar(512),
    versjon                     bigint       default 0                       not null,
    endret_tid                  timestamp(3),
    foedselsdato                timestamp(3) default CURRENT_TIMESTAMP,
    fk_aktoer_id                varchar(50)
        constraint fk_po_person
            references aktoer
            on update cascade,
    navn                        varchar      default ''::character varying,
    kjoenn                      varchar      default 'UKJENT'::character varying,
    maalform                    varchar(2)   default 'NB'::character varying not null
);


create index po_person_fk_gr_personopplysninger_id_idx
    on po_person (fk_gr_personopplysninger_id);

create index po_person_fk_idx
    on po_person (fk_aktoer_id);


create table vedtak
(
    id               bigint                                       not null
        constraint behandling_vedtak_pkey
            primary key,
    fk_behandling_id bigint                                       not null
        constraint behandling_vedtak_fk_behandling_id_fkey
            references behandling,
    versjon          bigint       default 0                       not null,
    opprettet_av     varchar(512) default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    vedtaksdato      timestamp(3) default LOCALTIMESTAMP,
    endret_av        varchar(512),
    endret_tid       timestamp(3),
    aktiv            boolean      default true,
    stonad_brev_pdf  bytea
);


create index behandling_vedtak_fk_behandling_id_idx
    on vedtak (fk_behandling_id);

create unique index uidx_behandling_vedtak_01
    on vedtak ((
               CASE
               WHEN aktiv = true THEN fk_behandling_id
               ELSE NULL::bigint
               END), (
                   CASE
                       WHEN aktiv = true THEN aktiv
                       ELSE NULL::boolean
                       END));



create table logg
(
    id               bigint                                       not null
        primary key,
    opprettet_av     varchar      default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    fk_behandling_id bigint                                       not null
        references behandling,
    type             varchar                                      not null,
    tittel           varchar                                      not null,
    rolle            varchar                                      not null,
    tekst            text                                         not null
);


create index logg_fk_behandling_id_idx
    on logg (fk_behandling_id);


create table gr_soknad
(
    id               bigint                                       not null
        primary key,
    opprettet_av     varchar      default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    fk_behandling_id bigint                                       not null
        references behandling,
    soknad           text                                         not null,
    aktiv            boolean      default true                    not null
);


create index gr_soknad_fk_behandling_id_idx
    on gr_soknad (fk_behandling_id);

create unique index uidx_gr_soknad_01
    on gr_soknad ((
                  CASE
                  WHEN aktiv = true THEN fk_behandling_id
                  ELSE NULL::bigint
                  END), (
                      CASE
                          WHEN aktiv = true THEN aktiv
                          ELSE NULL::boolean
                          END));



create table tilkjent_ytelse
(
    id                 bigint                              not null
        constraint beregning_resultat_pkey
            primary key,
    fk_behandling_id   bigint
        constraint beregning_resultat_fk_behandling_id_fkey
            references behandling,
    stonad_fom         timestamp,
    stonad_tom         timestamp,
    opprettet_dato     timestamp                           not null,
    opphor_fom         timestamp,
    utbetalingsoppdrag text,
    endret_dato        timestamp default CURRENT_TIMESTAMP not null
);


create index beregning_resultat_fk_behandling_id_idx
    on tilkjent_ytelse (fk_behandling_id);

create index tilkjent_ytelse_utbetalingsoppdrag_not_null_idx
    on tilkjent_ytelse (utbetalingsoppdrag)
    where (utbetalingsoppdrag IS NOT NULL);


create table andel_tilkjent_ytelse
(
    id                              bigint                                       not null
        primary key,
    fk_behandling_id                bigint                                       not null
        references behandling,
    versjon                         bigint       default 0                       not null,
    opprettet_av                    varchar(512) default 'VL'::character varying not null,
    opprettet_tid                   timestamp(3) default LOCALTIMESTAMP          not null,
    stonad_fom                      timestamp(3)                                 not null,
    stonad_tom                      timestamp(3)                                 not null,
    type                            varchar(50)                                  not null,
    kalkulert_utbetalingsbelop      numeric,
    endret_av                       varchar(512),
    endret_tid                      timestamp(3),
    tilkjent_ytelse_id              bigint
        references tilkjent_ytelse
            on delete cascade,
    periode_offset                  bigint,
    forrige_periode_offset          bigint,
    kilde_behandling_id             bigint
        references behandling,
    prosent                         numeric                                      not null,
    sats                            bigint                                       not null,
    fk_aktoer_id                    varchar
        constraint fk_andel_tilkjent_ytelse
            references aktoer
            on update cascade,
    nasjonalt_periodebelop          numeric,
    differanseberegnet_periodebelop numeric
);


create index andel_tilkjent_ytelse_fk_behandling_id_idx
    on andel_tilkjent_ytelse (fk_behandling_id);

create index andel_tilkjent_ytelse_fk_idx
    on andel_tilkjent_ytelse (kilde_behandling_id);

create index andel_tilkjent_ytelse_fk_tilkjent_idx
    on andel_tilkjent_ytelse (tilkjent_ytelse_id);

create index andel_tilkjent_ytelse_fk_aktoer_idx
    on andel_tilkjent_ytelse (fk_aktoer_id);

create index aty_type_idx
    on andel_tilkjent_ytelse (type);


create table vilkaarsvurdering
(
    id               bigint                                       not null
        constraint behandling_resultat_pkey
            primary key,
    fk_behandling_id bigint                                       not null
        constraint behandling_resultat_fk_behandling_id_fkey
            references behandling,
    aktiv            boolean      default true                    not null,
    versjon          bigint       default 0                       not null,
    opprettet_av     varchar      default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av        varchar,
    endret_tid       timestamp(3),
    samlet_resultat  varchar,
    ytelse_personer  text         default ''::text
);


create index vilkaarsvurdering_fk_idx
    on vilkaarsvurdering (fk_behandling_id);


create table person_resultat
(
    id                      bigint                                       not null
        constraint periode_resultat_pkey
            primary key,
    fk_vilkaarsvurdering_id bigint                                       not null
        constraint periode_resultat_fk_behandling_resultat_id_fkey
            references vilkaarsvurdering,
    versjon                 bigint       default 0                       not null,
    opprettet_av            varchar      default 'VL'::character varying not null,
    opprettet_tid           timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av               varchar,
    endret_tid              timestamp(3),
    fk_aktoer_id            varchar
        constraint fk_person_resultat
            references aktoer
            on update cascade
);


create table vilkar_resultat
(
    id                                 bigint                                       not null
        primary key,
    vilkar                             varchar(50)                                  not null,
    resultat                           varchar(50)                                  not null,
    regel_input                        text,
    regel_output                       text,
    versjon                            bigint       default 0                       not null,
    opprettet_av                       varchar(512) default 'VL'::character varying not null,
    opprettet_tid                      timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av                          varchar(512),
    endret_tid                         timestamp(3),
    fk_person_resultat_id              bigint
        references person_resultat,
    begrunnelse                        text,
    periode_fom                        timestamp(3) default NULL::timestamp without time zone,
    periode_tom                        timestamp(3) default NULL::timestamp without time zone,
    fk_behandling_id                   bigint                                       not null
        constraint fk_behandling_id_vilkar_resultat
            references behandling,
    evaluering_aarsak                  text         default ''::text,
    er_automatisk_vurdert              boolean      default false                   not null,
    er_eksplisitt_avslag_paa_soknad    boolean,
    vedtak_begrunnelse_spesifikasjoner text         default ''::text,
    vurderes_etter                     varchar,
    utdypende_vilkarsvurderinger       varchar
);


create index vilkar_resultat_fk_idx
    on vilkar_resultat (fk_behandling_id);

create index vilkar_resultat_fk_personr_idx
    on vilkar_resultat (fk_person_resultat_id);


create index person_resultat_fk_idx
    on person_resultat (fk_vilkaarsvurdering_id);

create index person_resultat_fk_aktoer_idx
    on person_resultat (fk_aktoer_id);


create table oppgave
(
    id               bigint    not null
        primary key,
    fk_behandling_id bigint    not null
        references behandling,
    gsak_id          varchar   not null,
    type             varchar   not null,
    ferdigstilt      boolean   not null,
    opprettet_tid    timestamp not null
);


create index oppgave_fk_idx
    on oppgave (fk_behandling_id);


create table totrinnskontroll
(
    id                 bigint                                           not null
        primary key,
    fk_behandling_id   bigint                                           not null
        references behandling,
    versjon            bigint       default 0                           not null,
    opprettet_av       varchar      default 'VL'::character varying     not null,
    opprettet_tid      timestamp(3) default LOCALTIMESTAMP              not null,
    endret_av          varchar,
    endret_tid         timestamp(3),
    aktiv              boolean      default true                        not null,
    saksbehandler      varchar                                          not null,
    beslutter          varchar,
    godkjent           boolean      default true,
    saksbehandler_id   varchar      default 'ukjent'::character varying not null,
    beslutter_id       varchar,
    kontrollerte_sider text         default ''::text
);


create index totrinnskontroll_fk_behandling_id_idx
    on totrinnskontroll (fk_behandling_id);

create unique index uidx_totrinnskontroll_01
    on totrinnskontroll ((
                         CASE
                         WHEN aktiv = true THEN fk_behandling_id
                         ELSE NULL::bigint
                         END), (
                             CASE
                                 WHEN aktiv = true THEN aktiv
                                 ELSE NULL::boolean
                                 END));


create table po_bostedsadresse
(
    id                bigint                                       not null
        primary key,
    type              varchar(20)                                  not null,
    bostedskommune    varchar,
    husnummer         varchar,
    husbokstav        varchar,
    bruksenhetsnummer varchar,
    adressenavn       varchar,
    kommunenummer     varchar,
    tilleggsnavn      varchar,
    postnummer        varchar,
    opprettet_av      varchar      default 'VL'::character varying not null,
    opprettet_tid     timestamp(3) default CURRENT_TIMESTAMP       not null,
    endret_av         varchar,
    versjon           bigint       default 0                       not null,
    endret_tid        timestamp(3),
    matrikkel_id      bigint,
    fom               date,
    tom               date,
    fk_po_person_id   bigint
        references po_person
);


create index po_bostedsadresse_fk_idx
    on po_bostedsadresse (fk_po_person_id);


create table journalpost
(
    id               bigint                                       not null
        primary key,
    fk_behandling_id bigint                                       not null
        references behandling,
    journalpost_id   varchar                                      not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    opprettet_av     varchar      default 'VL'::character varying not null,
    type             varchar
);


create index journalpost_fk_behandling_id_idx
    on journalpost (fk_behandling_id);


create table po_statsborgerskap
(
    id              bigint                                           not null
        primary key,
    fk_po_person_id bigint                                           not null
        references po_person,
    landkode        varchar(3)   default 'XUK'::character varying    not null,
    fom             date,
    tom             date,
    opprettet_av    varchar      default 'VL'::character varying     not null,
    opprettet_tid   timestamp(3) default LOCALTIMESTAMP              not null,
    endret_av       varchar,
    endret_tid      timestamp(3),
    versjon         bigint       default 0                           not null,
    medlemskap      varchar      default 'UKJENT'::character varying not null
);


create index po_statsborgerskap_fk_idx
    on po_statsborgerskap (fk_po_person_id);


create table po_opphold
(
    id              bigint                                       not null
        primary key,
    fk_po_person_id bigint                                       not null
        references po_person,
    type            varchar                                      not null,
    fom             date,
    tom             date,
    opprettet_av    varchar      default 'VL'::character varying not null,
    opprettet_tid   timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av       varchar,
    endret_tid      timestamp(3),
    versjon         bigint       default 0                       not null
);


create index po_opphold_fk_idx
    on po_opphold (fk_po_person_id);



create table po_arbeidsforhold
(
    id                bigint                                       not null
        primary key,
    fk_po_person_id   bigint                                       not null
        references po_person,
    arbeidsgiver_id   varchar,
    arbeidsgiver_type varchar,
    fom               date,
    tom               date,
    opprettet_av      varchar      default 'VL'::character varying not null,
    opprettet_tid     timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av         varchar,
    endret_tid        timestamp(3),
    versjon           bigint       default 0                       not null
);


create index po_arbeidsforhold_fk_idx
    on po_arbeidsforhold (fk_po_person_id);


create table po_bostedsadresseperiode
(
    id              bigint                                       not null
        primary key,
    fk_po_person_id bigint                                       not null
        references po_person,
    fom             date,
    tom             date,
    opprettet_av    varchar      default 'VL'::character varying not null,
    opprettet_tid   timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av       varchar,
    endret_tid      timestamp(3),
    versjon         bigint       default 0                       not null
);



create index po_bostedsadresseperiode_fk_idx
    on po_bostedsadresseperiode (fk_po_person_id);



create table behandling_steg_tilstand
(
    id                     bigint                                                not null
        primary key,
    fk_behandling_id       bigint                                                not null
        references behandling,
    behandling_steg        varchar                                               not null,
    behandling_steg_status varchar      default 'IKKE_UTFØRT'::character varying not null,
    versjon                bigint       default 0                                not null,
    opprettet_av           varchar      default 'VL'::character varying          not null,
    opprettet_tid          timestamp(3) default LOCALTIMESTAMP                   not null,
    endret_av              varchar,
    endret_tid             timestamp(3)
);


create index behandling_steg_tilstand_fk_idx
    on behandling_steg_tilstand (fk_behandling_id);


create table annen_vurdering
(
    id                    bigint                                       not null
        primary key,
    fk_person_resultat_id bigint                                       not null
        references person_resultat,
    resultat              varchar                                      not null,
    type                  varchar                                      not null,
    begrunnelse           text,
    versjon               bigint       default 0                       not null,
    opprettet_av          varchar      default 'VL'::character varying not null,
    opprettet_tid         timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av             varchar,
    endret_tid            timestamp(3)
);


create index annen_vurdering_fk_idx
    on annen_vurdering (fk_person_resultat_id);


create table okonomi_simulering_mottaker
(
    id               bigint                                       not null
        constraint vedtak_simulering_mottaker_pkey
            primary key,
    mottaker_nummer  varchar(50),
    mottaker_type    varchar(50),
    opprettet_av     varchar(512) default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av        varchar(512),
    endret_tid       timestamp(3),
    versjon          bigint       default 0,
    fk_behandling_id bigint
        references behandling
);


create index okonomi_simulering_mottaker_fk_idx
    on okonomi_simulering_mottaker (fk_behandling_id);


create table okonomi_simulering_postering
(
    id                                bigint                                       not null
        constraint vedtak_simulering_postering_pkey
            primary key,
    fk_okonomi_simulering_mottaker_id bigint
        constraint vedtak_simulering_postering_fk_vedtak_simulering_mottaker__fkey
            references okonomi_simulering_mottaker
            on delete cascade,
    fag_omraade_kode                  varchar(50),
    fom                               timestamp(3),
    tom                               timestamp(3),
    betaling_type                     varchar(50),
    belop                             bigint,
    postering_type                    varchar(50),
    forfallsdato                      timestamp(3),
    uten_inntrekk                     boolean,
    opprettet_av                      varchar(512) default 'VL'::character varying not null,
    opprettet_tid                     timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av                         varchar(512),
    endret_tid                        timestamp(3),
    versjon                           bigint       default 0
);


create index vedtak_simulering_postering_fk_vedtak_simulering_mottaker_i_idx
    on okonomi_simulering_postering (fk_okonomi_simulering_mottaker_id);


create table tilbakekreving
(
    id                           bigint                                    not null
        primary key,
    valg                         varchar                                   not null,
    varsel                       text,
    begrunnelse                  text                                      not null,
    tilbakekrevingsbehandling_id text,
    opprettet_av                 varchar   default 'VL'::character varying not null,
    opprettet_tid                timestamp default LOCALTIMESTAMP          not null,
    endret_av                    varchar,
    endret_tid                   timestamp(3),
    versjon                      bigint    default 0,
    fk_behandling_id             bigint
        references behandling
);


create index tilbakekreving_fk_idx
    on tilbakekreving (fk_behandling_id);


create table vedtaksperiode
(
    id            bigint                                    not null
        primary key,
    fk_vedtak_id  bigint
        references vedtak,
    fom           timestamp,
    tom           timestamp,
    type          varchar                                   not null,
    opprettet_av  varchar   default 'VL'::character varying not null,
    opprettet_tid timestamp default LOCALTIMESTAMP          not null,
    endret_av     varchar,
    endret_tid    timestamp(3),
    versjon       bigint    default 0
);


create index vedtaksperiode_fk_vedtak_id_idx
    on vedtaksperiode (fk_vedtak_id);


create table vedtaksbegrunnelse
(
    id                               bigint  not null
        primary key,
    fk_vedtaksperiode_id             bigint
        references vedtaksperiode
            on delete cascade,
    vedtak_begrunnelse_spesifikasjon varchar not null
);


create index vedtaksbegrunnelse_fk_vedtaksperiode_id_idx
    on vedtaksbegrunnelse (fk_vedtaksperiode_id);


create table vedtaksbegrunnelse_fritekst
(
    id                   bigint                not null
        primary key,
    fk_vedtaksperiode_id bigint
        references vedtaksperiode
            on delete cascade,
    fritekst             text default ''::text not null
);



create index vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_idx
    on vedtaksbegrunnelse_fritekst (fk_vedtaksperiode_id);


create table po_sivilstand
(
    id              bigint                                       not null
        primary key,
    fk_po_person_id bigint                                       not null
        references po_person,
    fom             date,
    type            varchar                                      not null,
    opprettet_av    varchar      default 'VL'::character varying not null,
    opprettet_tid   timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av       varchar,
    endret_tid      timestamp(3),
    versjon         bigint       default 0                       not null
);


create index po_sivilstand_fk_idx
    on po_sivilstand (fk_po_person_id);




create table endret_utbetaling_andel
(
    id                                 bigint                                       not null
        primary key,
    fk_behandling_id                   bigint                                       not null
        references behandling,
    fk_po_person_id                    bigint
        references po_person,
    fom                                timestamp(3),
    tom                                timestamp(3),
    prosent                            numeric,
    aarsak                             varchar,
    begrunnelse                        text,
    versjon                            bigint       default 0                       not null,
    opprettet_av                       varchar      default 'VL'::character varying not null,
    opprettet_tid                      timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av                          varchar,
    endret_tid                         timestamp(3),
    vedtak_begrunnelse_spesifikasjoner text         default ''::text,
    avtaletidspunkt_delt_bosted        timestamp(3),
    soknadstidspunkt                   timestamp(3)
);


create index endret_utbetaling_andel_fk_behandling_id_idx
    on endret_utbetaling_andel (fk_behandling_id);

create index endret_utbetaling_andel_fk_idx
    on endret_utbetaling_andel (fk_po_person_id);


create table andel_til_endret_andel
(
    fk_andel_tilkjent_ytelse_id   bigint not null
        references andel_tilkjent_ytelse
            on delete cascade,
    fk_endret_utbetaling_andel_id bigint not null
        references endret_utbetaling_andel,
    primary key (fk_andel_tilkjent_ytelse_id, fk_endret_utbetaling_andel_id)
);



create table sett_paa_vent
(
    id                bigint                                       not null
        primary key,
    fk_behandling_id  bigint                                       not null
        references behandling,
    versjon           bigint       default 0                       not null,
    opprettet_av      varchar      default 'VL'::character varying not null,
    opprettet_tid     timestamp(3) default LOCALTIMESTAMP          not null,
    frist             timestamp(3)                                 not null,
    aktiv             boolean      default false                   not null,
    aarsak            varchar                                      not null,
    endret_av         varchar,
    endret_tid        timestamp(3),
    tid_tatt_av_vent  timestamp(3),
    tid_satt_paa_vent timestamp(3) default now()                   not null
);


create index sett_paa_vent_fk_behandling_id_idx
    on sett_paa_vent (fk_behandling_id);

create unique index uidx_sett_paa_vent_aktiv
    on sett_paa_vent (fk_behandling_id, aktiv)
    where (aktiv = true);


create table po_doedsfall
(
    id                   bigint                                       not null
        primary key,
    fk_po_person_id      bigint                                       not null
        references po_person,
    versjon              bigint       default 0                       not null,
    doedsfall_dato       timestamp(3)                                 not null,
    doedsfall_adresse    varchar,
    doedsfall_postnummer varchar,
    doedsfall_poststed   varchar,
    opprettet_av         varchar      default 'VL'::character varying not null,
    opprettet_tid        timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av            varchar,
    endret_tid           timestamp(3)
);


create index po_doedsfall_fk_po_person_id_idx
    on po_doedsfall (fk_po_person_id);





create table behandling_soknadsinfo
(
    id               bigint                                       not null
        primary key,
    fk_behandling_id bigint                                       not null
        references behandling,
    mottatt_dato     timestamp(3)                                 not null,
    versjon          bigint       default 0                       not null,
    opprettet_av     varchar      default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av        varchar,
    endret_tid       timestamp(3)
);


create index behandling_soknadsinfo_fk_behandling_id_idx
    on behandling_soknadsinfo (fk_behandling_id);


create table kompetanse
(
    id                              bigint                                       not null
        primary key,
    fk_behandling_id                bigint                                       not null
        references behandling,
    fom                             timestamp(3),
    tom                             timestamp(3),
    versjon                         bigint       default 0                       not null,
    opprettet_av                    varchar      default 'VL'::character varying not null,
    opprettet_tid                   timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av                       varchar,
    endret_tid                      timestamp(3),
    soekers_aktivitet               varchar,
    annen_forelderes_aktivitet      varchar,
    annen_forelderes_aktivitetsland varchar,
    barnets_bostedsland             varchar,
    resultat                        varchar,
    sokers_aktivitetsland           text
);


create index kompetanse_fk_behandling_id_idx
    on kompetanse (fk_behandling_id);


create table aktoer_til_kompetanse
(
    fk_kompetanse_id bigint  not null
        references kompetanse,
    fk_aktoer_id     varchar not null
        references aktoer
            on update cascade,
    primary key (fk_kompetanse_id, fk_aktoer_id)
);


create table valutakurs
(
    id               bigint                                       not null
        primary key,
    fk_behandling_id bigint                                       not null
        references behandling,
    fom              timestamp(3),
    tom              timestamp(3),
    valutakursdato   timestamp(3) default NULL::timestamp without time zone,
    valutakode       varchar,
    kurs             numeric,
    versjon          bigint       default 0                       not null,
    opprettet_av     varchar      default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av        varchar,
    endret_tid       timestamp(3)
);


create index valutakurs_fk_behandling_id_idx
    on valutakurs (fk_behandling_id);


create table aktoer_til_valutakurs
(
    fk_valutakurs_id bigint  not null
        references valutakurs,
    fk_aktoer_id     varchar not null
        references aktoer
            on update cascade,
    primary key (fk_valutakurs_id, fk_aktoer_id)
);


create table utenlandsk_periodebeloep
(
    id                         bigint                                       not null
        primary key,
    fk_behandling_id           bigint                                       not null
        references behandling,
    fom                        timestamp(3),
    tom                        timestamp(3),
    intervall                  varchar,
    valutakode                 varchar,
    beloep                     numeric,
    versjon                    bigint       default 0                       not null,
    opprettet_av               varchar      default 'VL'::character varying not null,
    opprettet_tid              timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av                  varchar,
    endret_tid                 timestamp(3),
    utbetalingsland            varchar,
    kalkulert_maanedlig_beloep numeric
);



create index utenlandsk_periodebeloep_fk_behandling_id_idx
    on utenlandsk_periodebeloep (fk_behandling_id);


create table aktoer_til_utenlandsk_periodebeloep
(
    fk_utenlandsk_periodebeloep_id bigint  not null
        constraint aktoer_til_utenlandsk_periode_fk_utenlandsk_periodebeloep__fkey
            references utenlandsk_periodebeloep,
    fk_aktoer_id                   varchar not null
        references aktoer
            on update cascade,
    primary key (fk_utenlandsk_periodebeloep_id, fk_aktoer_id)
);



create table verge
(
    id               bigint                                       not null
        primary key,
    ident            varchar,
    fk_behandling_id bigint                                       not null
        references behandling,
    versjon          bigint       default 0                       not null,
    opprettet_av     varchar(20)  default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av        varchar(20),
    endret_tid       timestamp(3)
);



create unique index uidx_verge_behandling_id
    on verge (fk_behandling_id);


create table korrigert_etterbetaling
(
    id               bigint                                       not null
        primary key,
    aarsak           varchar                                      not null,
    begrunnelse      varchar,
    belop            bigint                                       not null,
    aktiv            boolean                                      not null,
    fk_behandling_id bigint                                       not null
        references behandling,
    versjon          bigint       default 0                       not null,
    opprettet_av     varchar      default 'VL'::character varying not null,
    opprettet_tid    timestamp(3) default LOCALTIMESTAMP          not null,
    endret_av        varchar,
    endret_tid       timestamp(3)
);


create unique index uidx_korrigert_etterbetaling_fk_behandling_id_aktiv
    on korrigert_etterbetaling (fk_behandling_id)
    where (aktiv = true);

create index korrigert_etterbetaling_fk_behandling_id_idx
    on korrigert_etterbetaling (fk_behandling_id);



