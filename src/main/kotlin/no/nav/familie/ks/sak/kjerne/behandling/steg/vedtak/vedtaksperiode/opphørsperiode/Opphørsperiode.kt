package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.opphørsperiode

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSammeEllerFør
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiode.Utbetalingsperiode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiode.mapTilUtbetalingsperioder
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2025
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2021.barnehageplass.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.barnehageplass.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilTidslinjePerioderMedDato
import java.time.LocalDate
import java.time.YearMonth

data class Opphørsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate?,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
    val begrunnelser: List<NasjonalEllerFellesBegrunnelse> = emptyList(),
) : Vedtaksperiode

fun mapTilOpphørsperioder(
    forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = emptyList(),
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vilkårsvurdering: Vilkårsvurdering,
    adopsjonerIBehandling: List<Adopsjon>,
    endringstidspunktForBehandling: LocalDate,
): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder =
        if (forrigePersonopplysningGrunnlag != null) {
            mapTilUtbetalingsperioder(
                personopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                andelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
            )
        } else {
            emptyList()
        }
    val utbetalingsperioder =
        mapTilUtbetalingsperioder(personopplysningGrunnlag, andelerTilkjentYtelse)

    val alleOpphørsperioder =
        if (forrigeUtbetalingsperioder.isNotEmpty() && utbetalingsperioder.isEmpty()) {
            listOf(
                Opphørsperiode(
                    periodeFom = forrigeUtbetalingsperioder.minOf { it.periodeFom },
                    periodeTom = null,
                ),
            )
        } else {
            if (utbetalingsperioder.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(forrigeUtbetalingsperioder, utbetalingsperioder),
                    finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder),
                    finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder, vilkårsvurdering, personopplysningGrunnlag, adopsjonerIBehandling),
                ).flatten()
            }.sortedBy { it.periodeFom }
        }

    val (perioderFørEndringstidspunkt, fraEndringstidspunktOgUtover) =
        alleOpphørsperioder.partition { it.periodeFom.isBefore(endringstidspunktForBehandling) }

    return perioderFørEndringstidspunkt + slåSammenOpphørsperioder(fraEndringstidspunktOgUtover)
}

fun slåSammenOpphørsperioder(alleOpphørsperioder: List<Opphørsperiode>): List<Opphørsperiode> {
    if (alleOpphørsperioder.isEmpty()) return emptyList()

    val sortertOpphørsperioder = alleOpphørsperioder.sortedBy { it.periodeFom }

    return sortertOpphørsperioder.fold(
        mutableListOf(
            sortertOpphørsperioder.first(),
        ),
    ) { acc: MutableList<Opphørsperiode>, nesteOpphørsperiode: Opphørsperiode ->
        val forrigeOpphørsperiode = acc.last()
        when {
            nesteOpphørsperiode.periodeFom.erSammeEllerFør(forrigeOpphørsperiode.periodeTom ?: TIDENES_ENDE) -> {
                acc[acc.lastIndex] =
                    forrigeOpphørsperiode.copy(
                        periodeTom =
                            maxOfOpphørsperiodeTom(
                                forrigeOpphørsperiode.periodeTom,
                                nesteOpphørsperiode.periodeTom,
                            ),
                    )
            }

            else -> {
                acc.add(nesteOpphørsperiode)
            }
        }

        acc
    }
}

private fun finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(
    forrigeUtbetalingsperioder: List<Utbetalingsperiode>,
    utbetalingsperioder: List<Utbetalingsperiode>,
): List<Opphørsperiode> {
    val opphørteUtbetalingTidslinje =
        forrigeUtbetalingsperioder
            .tilTidslinje()
            .kombinerMed(utbetalingsperioder.tilTidslinje()) { forrigeUtbetaling, utbetaling ->
                forrigeUtbetaling != null && utbetaling == null
            }

    return opphørteUtbetalingTidslinje
        .tilPerioder()
        .mapNotNull { opphørtUtbetalingPeriode ->
            val fomDatoPåPeriode = opphørtUtbetalingPeriode.fom

            if (opphørtUtbetalingPeriode.verdi == true && fomDatoPåPeriode != null) {
                Opphørsperiode(
                    periodeFom = fomDatoPåPeriode,
                    periodeTom = opphørtUtbetalingPeriode.tom,
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                )
            } else {
                null
            }
        }
}

private fun List<Utbetalingsperiode>.tilTidslinje() =
    this
        .map {
            Periode(
                fom = it.periodeFom,
                tom = it.periodeTom,
                verdi = it,
            )
        }.tilTidslinje()

private fun maxOfOpphørsperiodeTom(
    a: LocalDate?,
    b: LocalDate?,
): LocalDate? = if (a != null && b != null) maxOf(a, b) else null

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(
    utbetalingsperioder: List<Utbetalingsperiode>,
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom =
        utbetalingsperioder
            .filter { it.utbetaltPerMnd != 0 }
            .ifEmpty { return emptyList() }
            .maxOf { it.periodeTom }
            .toYearMonth()

    val cutOffDato = inneværendeMåned().plusMonths(2)

    val erFramtidigOpphørPgaBarnehageplass =
        vilkårsvurdering.personResultater.any { personResultat ->

            personResultat.vilkårResultater
                .filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BARNEHAGEPLASS }
                .filter { it.periodeTom != null }
                .any { barnehageplassVilkårResultat ->

                    val barnet =
                        personopplysningGrunnlag.barna.find { it.aktør == personResultat.aktør }
                            ?: throw Feil("Barn i personopplysningsgrunnlaget samsvarer ikke med barnet i vilkårsresultat")

                    val adopsjonForBarnet = adopsjonerIBehandling.firstOrNull { it.aktør == personResultat.aktør }

                    val forskjøvetTomForSisteUtbetalingsperiodePgaFremtidigOpphør =
                        forskyvTomBasertPåLovverkForSisteUtbetalingsperiodePgaFremtidigOpphør(barnet = barnet, adopsjon = adopsjonForBarnet, periodeTom = barnehageplassVilkårResultat.periodeTom!!)

                    barnehageplassVilkårResultat.søkerHarMeldtFraOmBarnehageplass == true &&
                        forskjøvetTomForSisteUtbetalingsperiodePgaFremtidigOpphør == sisteUtbetalingsperiodeTom
                }
        }

    return if (sisteUtbetalingsperiodeTom.isBefore(cutOffDato) || erFramtidigOpphørPgaBarnehageplass) {
        listOf(
            Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.nesteMåned().førsteDagIInneværendeMåned(),
                periodeTom = null,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                begrunnelser = if (erFramtidigOpphørPgaBarnehageplass) mutableListOf(NasjonalEllerFellesBegrunnelse.OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS) else mutableListOf(),
            ),
        )
    } else {
        emptyList()
    }
}

private fun forskyvTomBasertPåLovverkForSisteUtbetalingsperiodePgaFremtidigOpphør(
    barnet: Person,
    adopsjon: Adopsjon?,
    periodeTom: LocalDate,
): YearMonth? {
    val lovverk = LovverkUtleder.utledLovverkForBarn(fødselsdato = barnet.fødselsdato, adopsjonsdato = adopsjon?.adopsjonsdato)

    val periodeTomForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør =
        when (lovverk) {
            Lovverk.FØR_LOVENDRING_2025 -> {
                if (periodeTom.isBefore(DATO_LOVENDRING_2024)) {
                    periodeTom.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør()
                } else {
                    periodeTom.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024()
                }
            }

            Lovverk.LOVENDRING_FEBRUAR_2025 -> {
                periodeTom.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2025()
            }
        }
    return periodeTomForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør
}

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val utbetalingsperioderTidslinje =
        utbetalingsperioder.map { Periode(it, it.periodeFom, it.periodeTom) }.tilTidslinje()

    return utbetalingsperioderTidslinje
        .tilTidslinjePerioderMedDato()
        .filter { it.periodeVerdi is Udefinert }
        .map {
            Opphørsperiode(
                periodeFom = it.fom.tilLocalDateEllerNull() ?: TIDENES_MORGEN,
                periodeTom = it.tom.tilLocalDateEllerNull(),
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
            )
        }
}

fun Opphørsperiode.tilVedtaksperiodeMedBegrunnelse(
    vedtak: Vedtak,
): VedtaksperiodeMedBegrunnelser {
    val behandlingsÅrsak = vedtak.behandling.opprettetÅrsak

    return VedtaksperiodeMedBegrunnelser(
        fom = this.periodeFom,
        tom = this.periodeTom,
        vedtak = vedtak,
        type = this.vedtaksperiodetype,
    ).also { vedtaksperiodeMedBegrunnelser ->
        if (behandlingsÅrsak == BehandlingÅrsak.OVERGANGSORDNING_2024) {
            settOvergangsordningOpphørBegrunnelser(vedtaksperiodeMedBegrunnelser)
        } else {
            settBegrunnelserPåPeriode(vedtaksperiodeMedBegrunnelser)
        }
    }
}

private fun Opphørsperiode.settOvergangsordningOpphørBegrunnelser(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
) {
    val behandlingKategori = vedtaksperiodeMedBegrunnelser.vedtak.behandling.kategori

    when (behandlingKategori) {
        BehandlingKategori.NASJONAL ->
            vedtaksperiodeMedBegrunnelser.settBegrunnelser(
                listOf(
                    NasjonalEllerFellesBegrunnelse.OPPHØR_OVERGANGSORDNING_OPPHØR.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser),
                ),
            )

        BehandlingKategori.EØS ->
            vedtaksperiodeMedBegrunnelser.settEøsBegrunnelser(
                listOf(
                    EØSBegrunnelse.OPPHØR_OVERGANGSORDNING_OPPHØR_EØS.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser),
                ),
            )
    }
}

private fun Opphørsperiode.settBegrunnelserPåPeriode(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
) {
    vedtaksperiodeMedBegrunnelser.settBegrunnelser(
        begrunnelser.map {
            it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
        },
    )
}
