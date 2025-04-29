TRUNCATE TABLE aktoer, personident, po_person, gr_personopplysninger, behandling, fagsak, tilkjent_ytelse, andel_tilkjent_ytelse, person_resultat, vilkar_resultat, barnehagebarn CASCADE;

-- er lagt inn 2 ganger, én gang for 4 dager siden og én gang i dag
INSERT INTO aktoer(aktoer_id) VALUES ('1234');
-- lagt inn for 2 dager siden
INSERT INTO aktoer(aktoer_id) VALUES ('2345');
-- lagt inn i går
INSERT INTO aktoer(aktoer_id) VALUES ('4567');

--Søker
INSERT INTO aktoer(aktoer_id) VALUES ('9876');

INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('12345678901', '1234', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('23456789012', '2345', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('45678901234', '4567', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('90123456789', '9876', true);

INSERT INTO fagsak(id, fk_aktoer_id) VALUES (1, '9876');

INSERT INTO behandling(id, fk_fagsak_id, behandling_type, aktivert_tid, aktiv, kategori, opprettet_aarsak) VALUES (1, 1, 'FØRSTEGANGSBEHANDLING', NOW(), true, 'NASJONAL', 'SØKNAD');

INSERT INTO gr_personopplysninger(id, fk_behandling_id) VALUES (1, 1);

INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (1, 1, 'BARN', '1234');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (2, 1, 'BARN', '2345');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (4, 1, 'BARN', '4567');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (9, 1, 'SØKER', '9876');

INSERT INTO vilkaarsvurdering(id, fk_behandling_id) VALUES (1, 1);

INSERT INTO person_resultat(id, fk_aktoer_id, fk_vilkaarsvurdering_id) VALUES (1,'1234', 1);
INSERT INTO person_resultat(id, fk_aktoer_id, fk_vilkaarsvurdering_id) VALUES (2, '2345', 1);
INSERT INTO person_resultat(id, fk_aktoer_id, fk_vilkaarsvurdering_id) VALUES (4, '4567', 1);

INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer) VALUES (1, 'BARNEHAGEPLASS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months', 'OPPFYLT', 1, 1, 30);
INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer) VALUES (2, 'BARNEHAGEPLASS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months','OPPFYLT', 2, 1, 30);
INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer) VALUES (4, 'BARNEHAGEPLASS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months','OPPFYLT', 4, 1, 30);

INSERT INTO tilkjent_ytelse(id, fk_behandling_id, opprettet_dato) VALUES (1, 1, CURRENT_TIMESTAMP);

INSERT INTO andel_tilkjent_ytelse(id, fk_behandling_id, tilkjent_ytelse_id, fk_aktoer_id, kalkulert_utbetalingsbelop, stonad_fom, stonad_tom, type, sats, prosent) VALUES (1, 1, 1, '1234', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months', 'ORDINÆR_KONTANTSTØTTE', 100, 100);
INSERT INTO andel_tilkjent_ytelse(id, fk_behandling_id, tilkjent_ytelse_id, fk_aktoer_id, kalkulert_utbetalingsbelop, stonad_fom, stonad_tom, type, sats, prosent) VALUES (2, 1, 1, '2345', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months', 'ORDINÆR_KONTANTSTØTTE', 100, 100);
INSERT INTO andel_tilkjent_ytelse(id, fk_behandling_id, tilkjent_ytelse_id, fk_aktoer_id, kalkulert_utbetalingsbelop, stonad_fom, stonad_tom, type, sats, prosent) VALUES (4, 1, 1, '4567', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months', 'ORDINÆR_KONTANTSTØTTE', 100, 100);

INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, endret_tid) VALUES ('a7de9709-7817-4f3e-a822-5d4ca49a6914', '12345678901', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months',30, 'Oslo', '1', '9876', CURRENT_TIMESTAMP - INTERVAL '4 days');
INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, endret_tid) VALUES ('e7de9709-7817-4f3e-a822-5d4ca49a6914', '12345678901', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months',30, 'Oslo', '1', '8769', CURRENT_TIMESTAMP);
INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, endret_tid) VALUES ('b7f7f13b-f145-420b-96cc-88a6a6da230f', '23456789012', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months',30, 'Oslo', '1', '6769', CURRENT_TIMESTAMP - INTERVAL '2 days');
INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, endret_tid) VALUES ('d7f7f13b-f145-420b-96cc-88a6a6da230f', '45678901234', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months',30, 'Oslo', '1', '7698', CURRENT_TIMESTAMP - INTERVAL '1 days');

