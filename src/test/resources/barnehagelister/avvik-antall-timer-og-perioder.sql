TRUNCATE TABLE personident, po_person, gr_personopplysninger, behandling, fagsak, person_resultat, vilkar_resultat CASCADE;

--har forskjellig antall timer i barnehagebarn og vilkårresultat
INSERT INTO aktoer(aktoer_id) VALUES ('1234');
INSERT INTO aktoer(aktoer_id) VALUES ('2341');
--har forskjellig tom i barnehagebarn og vilkårresultat
INSERT INTO aktoer(aktoer_id) VALUES ('3412');
INSERT INTO aktoer(aktoer_id) VALUES ('4123');

INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('12345678901', '1234', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('23456789012', '2341', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('34567890123', '3412', true);
INSERT INTO personident (foedselsnummer, fk_aktoer_id, aktiv) VALUES ('45678901234', '4123', true);

INSERT INTO fagsak(id, fk_aktoer_id) VALUES (1, '4123');

INSERT INTO behandling(id, fk_fagsak_id, behandling_type, aktivert_tid) VALUES (1, 1, 'FØRSTEGANGSBEHANDLING', NOW());

INSERT INTO gr_personopplysninger(id, fk_behandling_id) VALUES (1, 1);

INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (1, 1, 'BARN', '1234');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (2, 1, 'BARN', '2341');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (3, 1, 'BARN', '3412');
INSERT INTO po_person(id, fk_gr_personopplysninger_id, type, fk_aktoer_id ) VALUES (4, 1, 'SØKER', '4123');

INSERT INTO person_resultat(id, fk_aktoer_id) VALUES (1,'1234');
INSERT INTO person_resultat(id, fk_aktoer_id) VALUES (2, '2341');
INSERT INTO person_resultat(id, fk_aktoer_id) VALUES (3, '3412');

INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer) VALUES (1, 'BARNEHAGEPLASS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months', 'OPPFYLT', 1, 1, 20);
INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer) VALUES (2, 'BARNEHAGEPLASS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 months','OPPFYLT', 2, 1, 30);
INSERT INTO vilkar_resultat(id, vilkar, periode_fom, periode_tom, resultat, fk_person_resultat_id, fk_behandling_id, antall_timer) VALUES (3, 'BARNEHAGEPLASS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '3 months','OPPFYLT', 2, 1, 30);

INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse) VALUES ('a7de9709-7817-4f3e-a822-5d4ca49a6914', '12345678901', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months',30, 'OSLO', '1', '9876');
INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse) VALUES ('c7f7f13b-f145-420b-96cc-88a6a6da230f', '23456789012', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months',30, 'OSLO', '1', '8769');
INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse) VALUES ('b7f7f13b-f145-420b-96cc-88a6a6da230f', '34567890123', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months',30, 'OSLO', '1', '7698');

