package no.nav.familie.ks.sak.kjerne.tilbakekreving.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker

@Entity(name = "Tilbakekreving")
@Table(name = "tilbakekreving")
data class Tilbakekreving(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilbakekreving_seq_generator")
    @SequenceGenerator(
        name = "tilbakekreving_seq_generator",
        sequenceName = "tilbakekreving_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @OneToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false, unique = true)
    val behandling: Behandling,
    @Enumerated(EnumType.STRING)
    @Column(name = "valg")
    val valg: Tilbakekrevingsvalg,
    @Column(name = "varsel")
    val varsel: String? = null,
    @Column(name = "begrunnelse")
    val begrunnelse: String,
    @Column(name = "tilbakekrevingsbehandling_id")
    var tilbakekrevingsbehandlingId: String?,
) : BaseEntitet() {
    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ØkonomiSimuleringMottaker) return false

        return (id == other.id)
    }

    override fun toString(): String =
        """Tilbakekreving(id=$id,
            "behandlingId=${behandling.id},
            "valg=$valg,
            "tilbakekrevingsbehandlingId=$tilbakekrevingsbehandlingId)"""
}
