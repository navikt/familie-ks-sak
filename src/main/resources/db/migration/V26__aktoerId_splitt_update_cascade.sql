-- personident
ALTER TABLE personident DROP CONSTRAINT personident_fk_aktoer_id_fkey;

ALTER TABLE personident
    ADD CONSTRAINT personident_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- person_resultat
ALTER TABLE person_resultat DROP CONSTRAINT person_resultat_fk_aktoer_id_fkey;

ALTER TABLE person_resultat
    ADD CONSTRAINT person_resultat_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- po_person
ALTER TABLE po_person DROP CONSTRAINT po_person_fk_aktoer_id_fkey;

ALTER TABLE po_person
    ADD CONSTRAINT po_person_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- fagsak
ALTER TABLE fagsak DROP CONSTRAINT fagsak_fk_aktoer_id_fkey;

ALTER TABLE fagsak
    ADD CONSTRAINT fagsak_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- andel_tilkjent_ytelse
ALTER TABLE andel_tilkjent_ytelse DROP CONSTRAINT andel_tilkjent_ytelse_fk_aktoer_id_fkey;

ALTER TABLE andel_tilkjent_ytelse
    ADD CONSTRAINT andel_tilkjent_ytelse_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- aktoer_til_kompetanse
ALTER TABLE aktoer_til_kompetanse DROP CONSTRAINT aktoer_til_kompetanse_fk_aktoer_id_fkey;

ALTER TABLE aktoer_til_kompetanse
    ADD CONSTRAINT aktoer_til_kompetanse_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- aktoer_til_utenlandsk_periodebeloep
ALTER TABLE aktoer_til_utenlandsk_periodebeloep DROP CONSTRAINT aktoer_til_utenlandsk_periodebeloep_fk_aktoer_id_fkey;

ALTER TABLE aktoer_til_utenlandsk_periodebeloep
    ADD CONSTRAINT aktoer_til_utenlandsk_periodebeloep_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- aktoer_til_valutakurs
ALTER TABLE aktoer_til_valutakurs DROP CONSTRAINT aktoer_til_valutakurs_fk_aktoer_id_fkey;

ALTER TABLE aktoer_til_valutakurs
    ADD CONSTRAINT aktoer_til_valutakurs_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;
