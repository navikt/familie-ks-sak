package no.nav.familie.ks.sak.barnehagelister.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import java.time.LocalDate
import java.util.UUID

@Entity(name = "Barnehagebarn")
@Table(name = "BARNEHAGEBARN")
data class Barnehagebarn(
    @Id
    @Column(name = "ID")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "IDENT", nullable = false, updatable = false)
    var ident: String,
    @Column(name = "FOM", nullable = false, updatable = false)
    var fom: LocalDate,
    @Column(name = "TOM", nullable = true, updatable = true)
    var tom: LocalDate? = null,
    @Column(name = "ANTALL_TIMER_I_BARNEHAGE", nullable = false, updatable = true)
    var antallTimerIBarnehage: Double,
    @Column(name = "ENDRINGSTYPE", nullable = true, updatable = false)
    var endringstype: String? = null,
    @Column(name = "KOMMUNE_NAVN", nullable = false, updatable = false)
    var kommuneNavn: String,
    @Column(name = "KOMMUNE_NR", nullable = false, updatable = false)
    var kommuneNr: String,
    // Id som er felles for alle barn som blir sendt inn på likt. UUID fra familie-ks-barnehagelister
    @Column(name = "ARKIV_REFERANSE", nullable = false, updatable = false)
    var arkivReferanse: String,
    @Column(name = "KILDE_TOPIC", nullable = true, updatable = false)
    var kildeTopic: String? = null,
) : BaseEntitet()
