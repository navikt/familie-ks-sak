package no.nav.familie.ks.sak.barnehagelister.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "BarnehageBydel")
@Table(name = "BARNEHAGE_BYDEL")
data class BarnehageBydel(
    @Id
    @Column(name = "BARNEHAGE_ORG_NUMMER", nullable = false, updatable = false)
    val barnehageOrgNummer: String,
    @Column(name = "BYDEL_ORG_NUMMER", nullable = false, updatable = false)
    val bydelOrgNummer: String,
)
