-- indekser er lagt til for konsistensavstemming
CREATE INDEX fagsak_status_idx
    ON fagsak (status);

CREATE INDEX behandling_opprettet_tid_idx
    ON behandling (opprettet_tid);