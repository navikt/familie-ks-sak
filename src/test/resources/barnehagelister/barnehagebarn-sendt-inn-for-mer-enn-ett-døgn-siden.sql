TRUNCATE TABLE barnehagebarn CASCADE;

INSERT INTO barnehagebarn(id, ident, fom, tom, antall_timer_i_barnehage, kommune_navn, kommune_nr, arkiv_referanse, opprettet_tid) VALUES ('b1de9709-7817-4f3e-a822-5d4ca49a6914', '12345678901', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 months', 45, 'Gamle Oslo', '0301', '1111', LOCALTIMESTAMP - INTERVAL '2 days');
