package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.lagUtbetalingsperiodeDetaljer
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.tilKombinertTidslinjePerAktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.LocalDate

@Entity(name = "Vedtaksperiode")
@Table(name = "VEDTAKSPERIODE")
data class VedtaksperiodeMedBegrunnelser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksperiode_seq_generator")
    @SequenceGenerator(
        name = "vedtaksperiode_seq_generator",
        sequenceName = "vedtaksperiode_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vedtak_id")
    val vedtak: Vedtak,
    @Column(name = "fom", updatable = false)
    val fom: LocalDate? = null,
    @Column(name = "tom", updatable = false)
    val tom: LocalDate? = null,
    @Column(name = "type", updatable = false)
    @Enumerated(EnumType.STRING)
    val type: Vedtaksperiodetype,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val begrunnelser: MutableSet<NasjonalEllerFellesBegrunnelseDB> = mutableSetOf(),
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val eøsBegrunnelser: MutableSet<EØSBegrunnelseDB> = mutableSetOf(),
    // Bruker list for å bevare rekkefølgen som settes frontend.
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
) : BaseEntitet() {
    fun settBegrunnelser(
        nyeBegrunnelser: List<NasjonalEllerFellesBegrunnelseDB>,
    ) {
        begrunnelser.apply {
            clear()
            addAll(nyeBegrunnelser)
        }
    }

    fun settEøsBegrunnelser(nyeEøsBegrunnelser: List<EØSBegrunnelseDB>) {
        eøsBegrunnelser.apply {
            clear()
            addAll(nyeEøsBegrunnelser)
        }
    }

    fun settFritekster(nyeFritekster: List<VedtaksbegrunnelseFritekst>) =
        fritekster.apply {
            clear()
            addAll(nyeFritekster)
        }

    fun harFriteksterUtenStandardbegrunnelser(): Boolean = fritekster.isNotEmpty() && begrunnelser.isEmpty() && eøsBegrunnelser.isEmpty()

    fun hentUtbetalingsperiodeDetaljer(
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        dagensDato: LocalDate = LocalDate.now(),
    ): List<UtbetalingsperiodeDetalj> =
        when (this.type) {
            Vedtaksperiodetype.UTBETALING -> {
                val kombinertTidslinje = andelerTilkjentYtelse.tilKombinertTidslinjePerAktør()
                val vedtaksperiodeTidslinje = listOf(Periode(verdi = this, fom = fom, tom)).tilTidslinje()

                val andelTilkjentYtelserIPeriode =
                    kombinertTidslinje
                        .kombinerMed(
                            vedtaksperiodeTidslinje,
                        ) { andelTilkjentYtelseIPeriode, vedtaksPeriode -> if (vedtaksPeriode != null) andelTilkjentYtelseIPeriode else null }
                        .tilPerioder()
                        .mapNotNull { it.verdi }
                        .flatten()

                validerIkkeDelvisOverlappIAndelTilkjentYtelserOgVedtaksperiodeBegrunnelse(
                    andelTilkjentYtelserIPeriode,
                    personopplysningGrunnlag,
                )

                andelTilkjentYtelserIPeriode.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
            }

            Vedtaksperiodetype.FORTSATT_INNVILGET -> {
                val kombinertTidslinje = andelerTilkjentYtelse.tilKombinertTidslinjePerAktør()

                val andelTilkjentYtelserIPeriode =
                    kombinertTidslinje.tilPerioder().lastOrNull { (it.fom ?: TIDENES_MORGEN) <= dagensDato }
                        ?: kombinertTidslinje.tilPerioder().firstOrNull()

                andelTilkjentYtelserIPeriode?.verdi?.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
                    ?: throw Feil("Finner ikke gjeldende segment ved fortsatt innvilget")
            }

            Vedtaksperiodetype.OPPHØR,
            Vedtaksperiodetype.AVSLAG,
            -> emptyList()
        }

    fun støtterFritekst(
        sanityBegrunnelser: List<SanityBegrunnelse>,
        alleBegrunnelserStøtterFritekst: Boolean,
    ) =
        alleBegrunnelserStøtterFritekst ||
            (type !== Vedtaksperiodetype.UTBETALING) ||
            begrunnelser.any { it.nasjonalEllerFellesBegrunnelse.støtterFritekst(sanityBegrunnelser) } ||
            eøsBegrunnelser.any { it.begrunnelse.støtterFritekst(sanityBegrunnelser) }

    private fun validerIkkeDelvisOverlappIAndelTilkjentYtelserOgVedtaksperiodeBegrunnelse(
        andelTilkjentYtelserIPeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ) {
        val delvisOverlapp =
            andelTilkjentYtelserIPeriode.any {
                (this.fom ?: TIDENES_MORGEN).isBefore(it.stønadFom.førsteDagIInneværendeMåned()) ||
                    (
                        (
                            this.tom
                                ?: TIDENES_ENDE
                        ).isAfter(it.stønadTom.sisteDagIInneværendeMåned())
                    )
            }

        if (delvisOverlapp) {
            throw Feil("Andel overlapper vedtaksperiode kun delvis for behandling ${personopplysningGrunnlag.behandlingId}")
        }
    }
}
