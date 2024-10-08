package no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet

@Entity(name = "ArbeidsfordelingPåBehandling")
@Table(name = "ARBEIDSFORDELING_PA_BEHANDLING")
data class ArbeidsfordelingPåBehandling(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arbeidsfordeling_pa_behandling_seq_generator")
    @SequenceGenerator(
        name = "arbeidsfordeling_pa_behandling_seq_generator",
        sequenceName = "arbeidsfordeling_pa_behandling_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", nullable = false, updatable = false, unique = true)
    val behandlingId: Long,
    @Column(name = "behandlende_enhet_id", nullable = false)
    var behandlendeEnhetId: String,
    @Column(name = "behandlende_enhet_navn", nullable = false)
    var behandlendeEnhetNavn: String,
    @Column(name = "manuelt_overstyrt", nullable = false)
    var manueltOverstyrt: Boolean = false,
) {
    override fun toString(): String = "ArbeidsfordelingPåBehandling(id=$id, manueltOverstyrt=$manueltOverstyrt)"

    fun toSecureString(): String = "ArbeidsfordelingPåBehandling(id=$id, behandlendeEnhetId=$behandlendeEnhetId, behandlendeEnhetNavn=$behandlendeEnhetNavn, manueltOverstyrt=$manueltOverstyrt)"
}

fun ArbeidsfordelingPåBehandling.tilArbeidsfordelingsenhet(): Arbeidsfordelingsenhet =
    Arbeidsfordelingsenhet(
        enhetId = this.behandlendeEnhetId,
        enhetNavn = this.behandlendeEnhetNavn,
    )
