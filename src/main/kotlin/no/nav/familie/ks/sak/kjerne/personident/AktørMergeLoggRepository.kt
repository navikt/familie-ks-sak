package no.nav.familie.ks.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository

interface AktørMergeLoggRepository : JpaRepository<AktørMergeLogg, Long>
