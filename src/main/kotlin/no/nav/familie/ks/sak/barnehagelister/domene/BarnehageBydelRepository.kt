package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.jpa.repository.JpaRepository

interface BarnehageBydelRepository : JpaRepository<BarnehageBydel, String>
