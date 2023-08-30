package no.nav.familie.ks.sak.common.entitet

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDate

@Embeddable
data class DatoIntervallEntitet(
    @Column(name = "fom")
    val fom: LocalDate? = null,

    @Column(name = "tom")
    val tom: LocalDate? = null
)
