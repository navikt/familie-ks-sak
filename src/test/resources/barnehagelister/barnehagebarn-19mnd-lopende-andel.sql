TRUNCATE TABLE aktoer, personident, po_person, gr_personopplysninger, behandling, fagsak, tilkjent_ytelse, andel_tilkjent_ytelse, person_resultat, vilkar_resultat, barnehagebarn CASCADE;

-- Barn
INSERT INTO aktoer(aktoer_id) VALUES ('3456');

-- Søker
INSERT INTO aktoer(aktoer_id) VALUES ('9988');

INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('34567890123', '3456', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('99887766554', '9988', true);

INSERT INTO fagsak(id, fk_aktoer_id, status, arkivert) VALUES (10, '9988', 'LØPENDE', false);

INSERT INTO behandling(id, fk_fagsak_id, behandling_type, aktivert_tid, aktiv, kategori, opprettet_aarsak)
VALUES (10, 10, 'FØRSTEGANGSBEHANDLING', NOW(), true, 'NASJONAL', 'SØKNAD');

INSERT INTO gr_personopplysninger(id, fk_behandling_id) VALUES (10, 10);

INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (10, 10, 'BARN', '3456');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (11, 10, 'SØKER', '9988');

INSERT INTO vilkaarsvurdering(id, fk_behandling_id) VALUES (10, 10);

INSERT INTO person_resultat(id, fk_aktoer_id, fk_vilkaarsvurdering_id) VALUES (10, '3456', 10);

INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer)
VALUES (10, 'BARNEHAGEPLASS', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE, 'OPPFYLT', 10, 10, 30);

INSERT INTO tilkjent_ytelse(id, fk_behandling_id, opprettet_dato, utbetalingsoppdrag) VALUES (10, 10, CURRENT_TIMESTAMP, '{}');

INSERT INTO andel_tilkjent_ytelse(id, fk_behandling_id, tilkjent_ytelse_id, fk_aktoer_id, kalkulert_utbetalingsbelop,stonad_fom, stonad_tom, type, sats, prosent
) VALUES (10, 10, 10, '3456', 100,CURRENT_TIMESTAMP - INTERVAL '3 month', DATE_TRUNC('month', CURRENT_DATE), 'ORDINÆR_KONTANTSTØTTE', 100, 100);

-- Barnehagebarn
INSERT INTO barnehagebarn(
    id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, endret_tid
) VALUES (
             'c7de9709-7817-4f3e-a822-5d4ca49a6914', '34567890123',
             CURRENT_DATE - INTERVAL '1 month',
             CURRENT_DATE + INTERVAL '1 month',
             30, 'Oslo', '1', '5555', CURRENT_TIMESTAMP
         );