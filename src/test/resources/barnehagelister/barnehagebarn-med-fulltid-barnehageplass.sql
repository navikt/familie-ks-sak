TRUNCATE TABLE aktoer, personident, po_person, gr_personopplysninger, behandling, fagsak, tilkjent_ytelse, andel_tilkjent_ytelse, person_resultat, vilkar_resultat, barnehagebarn CASCADE;

--har forskjellig antall timer i barnehagebarn og vilkårresultat men har fulltid i begge tilfeller
INSERT INTO aktoer(aktoer_id) VALUES ('1234');

--Søker
INSERT INTO aktoer(aktoer_id) VALUES ('9876');

INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('12345678901', '1234', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('90123456789', '9876', true);

INSERT INTO fagsak(id, fk_aktoer_id) VALUES (1, '9876');

INSERT INTO behandling(id, fk_fagsak_id, behandling_type, aktivert_tid, aktiv, kategori, opprettet_aarsak) VALUES (1, 1, 'FØRSTEGANGSBEHANDLING', NOW(), true, 'NASJONAL', 'SØKNAD');

INSERT INTO gr_personopplysninger(id, fk_behandling_id) VALUES (1, 1);

INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (1, 1, 'BARN', '1234');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (9, 1, 'SØKER', '9876');

INSERT INTO vilkaarsvurdering(id, fk_behandling_id) VALUES (1, 1);

INSERT INTO person_resultat(id, fk_aktoer_id, fk_vilkaarsvurdering_id) VALUES (1, '1234', 1);

INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer) VALUES (1, 'BARNEHAGEPLASS', CURRENT_TIMESTAMP - INTERVAL '3 months', null, 'OPPFYLT', 1, 1, 45);

INSERT INTO tilkjent_ytelse(id, fk_behandling_id, opprettet_dato) VALUES (1, 1, CURRENT_TIMESTAMP);

INSERT INTO andel_tilkjent_ytelse(id, fk_behandling_id, tilkjent_ytelse_id, fk_aktoer_id, kalkulert_utbetalingsbelop, stonad_fom, stonad_tom, type, sats, prosent) VALUES (1, 1, 1, '1234', 100, CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP - INTERVAL '3 months', 'ORDINÆR_KONTANTSTØTTE', 100, 100);

INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, endret_tid) VALUES ('c7de9709-7817-4f3e-a822-5d4ca49a6914', '12345678901', CURRENT_DATE - INTERVAL '3 months', null, 50, 'Oslo', '1', '7698', CURRENT_TIMESTAMP);


