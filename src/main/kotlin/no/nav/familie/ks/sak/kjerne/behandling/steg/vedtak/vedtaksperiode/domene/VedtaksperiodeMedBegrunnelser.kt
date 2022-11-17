package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.lagUtbetalingsperiodeDetaljer
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.tilKombinertTidslinjePerAktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "Vedtaksperiode")
@Table(name = "VEDTAKSPERIODE")
data class VedtaksperiodeMedBegrunnelser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksperiode_seq_generator")
    @SequenceGenerator(
        name = "vedtaksperiode_seq_generator",
        sequenceName = "vedtaksperiode_seq",
        allocationSize = 50
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
        orphanRemoval = true
    )
    val begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(),

    // Bruker list for å bevare rekkefølgen som settes frontend.
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf()

) : BaseEntitet() {

    fun settBegrunnelser(nyeBegrunnelser: List<Vedtaksbegrunnelse>) =
        begrunnelser.apply {
            clear()
            addAll(nyeBegrunnelser)
        }

    fun settFritekster(nyeFritekster: List<VedtaksbegrunnelseFritekst>) =
        fritekster.apply {
            clear()
            addAll(nyeFritekster)
        }

    fun harFriteksterUtenStandardbegrunnelser(): Boolean =
        (type == Vedtaksperiodetype.OPPHØR || type == Vedtaksperiodetype.AVSLAG) && fritekster.isNotEmpty() && begrunnelser.isEmpty()

    fun harFriteksterOgStandardbegrunnelser(): Boolean =
        fritekster.isNotEmpty() && begrunnelser.isNotEmpty()

    fun hentUtbetalingsperiodeDetaljer(
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): List<UtbetalingsperiodeDetalj> =
        if (this.type == Vedtaksperiodetype.UTBETALING ||
            this.type == Vedtaksperiodetype.FORTSATT_INNVILGET
        ) {
            val kombinertTidslinje = andelerTilkjentYtelse.tilKombinertTidslinjePerAktør()

            val vedtaksperiodeTidslinje = listOf(Periode(verdi = this, fom = this.fom, this.tom)).tilTidslinje()

            val tidslinjeMedAndelerIPeriode =
                kombinertTidslinje.kombinerMed(vedtaksperiodeTidslinje) { andelTilkjentYtelseIPeriode, vedtaksPeriode -> if (vedtaksPeriode != null) andelTilkjentYtelseIPeriode else null }

            val andelTilkjentYtelserIPeriode =
                tidslinjeMedAndelerIPeriode.tilPerioder().mapNotNull { it.verdi }.flatten()

            validerIkkeDelvisOverlappIAndelTilkjentYtelserOgVedtaksperiodeBegrunnelse(
                andelTilkjentYtelserIPeriode,
                personopplysningGrunnlag
            )

            andelTilkjentYtelserIPeriode.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
        } else {
            emptyList()
        }

    private fun validerIkkeDelvisOverlappIAndelTilkjentYtelserOgVedtaksperiodeBegrunnelse(
        andelTilkjentYtelserIPeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val delvisOverlapp = andelTilkjentYtelserIPeriode.any {
            (this.fom ?: TIDENES_MORGEN).isBefore(it.stønadFom.førsteDagIInneværendeMåned()) || (
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
