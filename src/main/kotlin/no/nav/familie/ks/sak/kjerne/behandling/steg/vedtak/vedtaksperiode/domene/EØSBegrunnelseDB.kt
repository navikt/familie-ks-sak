package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse

@Entity(name = "eøsBegrunnelse")
@Table(name = "eos_begrunnelse")
class EØSBegrunnelseDB(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eos_begrunnelse_seq_generator")
    @SequenceGenerator(
        name = "eos_begrunnelse_seq_generator",
        sequenceName = "eos_begrunnelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vedtaksperiode_id", nullable = false, updatable = false)
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    @Enumerated(EnumType.STRING)
    @Column(name = "begrunnelse", updatable = false)
    val begrunnelse: EØSBegrunnelse,
) {
    fun kopier(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): EØSBegrunnelseDB =
        EØSBegrunnelseDB(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            begrunnelse = this.begrunnelse,
        )
}
