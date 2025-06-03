package no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.util.StringListConverter
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext

@Entity(name = "Totrinnskontroll")
@Table(name = "TOTRINNSKONTROLL")
data class Totrinnskontroll(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "totrinnskontroll_seq_generator")
    @SequenceGenerator(
        name = "totrinnskontroll_seq_generator",
        sequenceName = "totrinnskontroll_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,
    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,
    @Column(name = "saksbehandler", nullable = false)
    val saksbehandler: String,
    @Column(name = "saksbehandler_id", nullable = false)
    val saksbehandlerId: String,
    @Column(name = "beslutter")
    var beslutter: String? = null,
    @Column(name = "beslutter_id")
    var beslutterId: String? = null,
    @Column(name = "godkjent")
    var godkjent: Boolean = false,
    @Column(name = "kontrollerte_sider")
    @Convert(converter = StringListConverter::class)
    var kontrollerteSider: List<String> = emptyList(),
) : BaseEntitet() {
    fun erUgyldig(): Boolean {
        val sammePerson = saksbehandler == beslutter || saksbehandlerId == beslutterId
        val ikkeSystem = saksbehandler != SikkerhetContext.SYSTEM_NAVN

        return godkjent && sammePerson && ikkeSystem
    }
}
