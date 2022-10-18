package no.nav.familie.ks.sak.kjerne.settpåvent.domene

import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "sett_paa_vent")
@Table(name = "sett_paa_vent")
data class SettPåVent(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sett_paa_vent_seq_generator")
    @SequenceGenerator(name = "sett_paa_vent_seq_generator", sequenceName = "sett_paa_vent_seq", allocationSize = 50)
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    @Column(name = "frist", nullable = false)
    var frist: LocalDate,

    @Column(name = "tid_tatt_av_vent", nullable = true)
    var tidTattAvVent: LocalDate? = null,

    @Column(name = "tid_satt_paa_vent", nullable = false)
    var tidSattPåVent: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak", nullable = false)
    var årsak: SettPåVentÅrsak,

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true
) : BaseEntitet()

enum class SettPåVentÅrsak(val visningsnavn: String) {
    AVVENTER_DOKUMENTASJON("Avventer dokumentasjon")
}
