package no.nav.familie.ks.sak.barnehagelister.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet

@Entity(name = "BarnehageBydel")
@Table(name = "BARNEHAGE_BYDEL")
data class BarnehageBydel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "barnehage_bydel_seq_generator")
    @SequenceGenerator(name = "barnehage_bydel_seq_generator", sequenceName = "barnehage_bydel_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "BARNEHAGE_ORG_NUMMER", nullable = false, updatable = false)
    val barnehageOrgNummer: String,
    @Column(name = "BYDEL_ORG_NUMMER", nullable = false, updatable = false)
    val bydelOrgNummer: String,
) : BaseEntitet()
