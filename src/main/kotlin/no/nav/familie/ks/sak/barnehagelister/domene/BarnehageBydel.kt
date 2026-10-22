package no.nav.familie.ks.sak.barnehagelister.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "BarnehageBydel")
@Table(name = "BARNEHAGE_BYDEL")
data class BarnehageBydel(
    @Id
    @Column(name = "BARNEHAGE_ORGANISASJONSNUMMER", nullable = false, updatable = false)
    val barnehageOrganisasjonsnummer: String,
    @Column(name = "BYDEL_ORGANISASJONSNUMMER", nullable = false, updatable = false)
    val bydelOrganisasjonsnummer: String,
)
