package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import java.time.LocalDateTime

@Entity(name = "Vedtak")
@Table(name = "VEDTAK")
class Vedtak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_seq_generator")
    @SequenceGenerator(name = "vedtak_seq_generator", sequenceName = "vedtak_seq", allocationSize = 50)
    val id: Long = 0,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,
    @Column(name = "vedtaksdato", nullable = true)
    var vedtaksdato: LocalDateTime? = null,
    @Column(name = "stonad_brev_pdf", nullable = true)
    var stønadBrevPdf: ByteArray? = null,
    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,
) : BaseEntitet() {
    override fun toString(): String = "Vedtak(id=$id, behandling=$behandling, vedtaksdato=$vedtaksdato, aktiv=$aktiv)"
}
