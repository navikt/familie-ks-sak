package no.nav.familie.ks.sak.kjerne.korrigertetterbetaling

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
import no.nav.familie.ks.sak.api.dto.KorrigertEtterbetalingRequestDto
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling

@Entity(name = "KorrigertEtterbetaling")
@Table(name = "KORRIGERT_ETTERBETALING")
class KorrigertEtterbetaling(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "korrigert_etterbetaling_seq_generator")
    @SequenceGenerator(
        name = "korrigert_etterbetaling_seq_generator",
        sequenceName = "korrigert_etterbetaling_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    val årsak: KorrigertEtterbetalingÅrsak,
    @Column(name = "begrunnelse")
    val begrunnelse: String?,
    @Column(name = "belop")
    val beløp: Int,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id")
    val behandling: Behandling,
    @Column(name = "aktiv")
    var aktiv: Boolean,
) : BaseEntitet()

fun KorrigertEtterbetalingRequestDto.tilKorrigertEtterbetaling(behandling: Behandling) =
    KorrigertEtterbetaling(
        årsak = årsak,
        begrunnelse = begrunnelse,
        behandling = behandling,
        beløp = beløp,
        aktiv = true,
    )

enum class KorrigertEtterbetalingÅrsak(val visningsnavn: String) {
    FEIL_TIDLIGERE_UTBETALT_BELØP("Feil i tidligere utbetalt beløp"),
    REFUSJON_FRA_UDI("Refusjon fra UDI"),
    REFUSJON_FRA_ANDRE_MYNDIGHETER("Refusjon fra andre myndigheter"),
    MOTREGNING("Motregning"),
}
