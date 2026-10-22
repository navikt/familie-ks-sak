CREATE TABLE barnehage_bydel
(
    barnehage_organisasjonsnummer VARCHAR      NOT NULL PRIMARY KEY,
    bydel_organisasjonsnummer     VARCHAR      NOT NULL,
    opprettet_tid                 TIMESTAMP(3) DEFAULT localtimestamp NOT NULL
);
