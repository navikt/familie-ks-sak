TRUNCATE TABLE aktoer, personident, po_person, gr_personopplysninger, behandling, fagsak, tilkjent_ytelse, andel_tilkjent_ytelse, person_resultat, vilkar_resultat, barnehagebarn CASCADE;

--Barnehagebarn uten fagsak eller noe data som tilsier at barnet har en behandling og eksisterer i KS fra før
INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, endret_tid) VALUES ('f7de9709-7817-4f3e-a822-5d4ca49a6914', '98765432109', CURRENT_DATE, CURRENT_DATE + INTERVAL '2 months', 30, 'Oslo', '1', '8769', CURRENT_TIMESTAMP);
