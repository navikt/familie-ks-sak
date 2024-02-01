ALTER TABLE vilkar_resultat
    ADD COLUMN IF NOT EXISTS soker_har_meldt_fra_om_barnehageplass boolean default null;