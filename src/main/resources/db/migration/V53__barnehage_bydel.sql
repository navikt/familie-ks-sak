CREATE TABLE barnehage_bydel
(
    barnehage_org_nummer VARCHAR      NOT NULL PRIMARY KEY,
    bydel_org_nummer     VARCHAR      NOT NULL,
    opprettet_tid        TIMESTAMP(3) DEFAULT localtimestamp NOT NULL
);
