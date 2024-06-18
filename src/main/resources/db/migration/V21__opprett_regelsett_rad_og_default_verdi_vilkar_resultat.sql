ALTER TABLE vilkar_resultat
    ADD COLUMN regelsett TEXT DEFAULT null;

UPDATE vilkar_resultat
SET regelsett = 'LOV_AUGUST_2021';