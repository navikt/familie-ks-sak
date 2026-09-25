DROP INDEX uidx_gr_personopplysninger_01;

CREATE UNIQUE INDEX uidx_gr_personopplysninger_01
    ON gr_personopplysninger (fk_behandling_id);

ALTER TABLE gr_personopplysninger
    DROP COLUMN aktiv;
