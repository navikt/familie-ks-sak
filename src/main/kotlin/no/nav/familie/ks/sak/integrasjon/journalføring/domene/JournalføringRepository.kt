package no.nav.familie.ks.sak.integrasjon.journalføring.domene

import org.springframework.data.jpa.repository.JpaRepository

interface JournalføringRepository : JpaRepository<DbJournalpost, Long>
