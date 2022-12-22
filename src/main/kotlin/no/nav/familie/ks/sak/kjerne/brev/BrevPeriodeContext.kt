package no.nav.familie.ks.sak.kjerne.brev

import forskyvVilkårResultater
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.erSenereEnnInneværendeMåned
import no.nav.familie.ks.sak.common.util.formaterBeløp
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelserForPeriodeContext
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.FritekstBegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilBrevTekst
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeType
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.tilBarnasFødselsdatoer
import java.math.BigDecimal
import java.time.LocalDate

class BrevPeriodeContext(
    private val utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val persongrunnlag: PersonopplysningGrunnlag,
    private val personResultater: List<PersonResultat>,
    private val andelTilkjentYtelserMedEndreteUtbetalinger: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,

    private val uregistrerteBarn: List<BarnMedOpplysningerDto>,
    private val barnSomDødeIForrigePeriode: List<Person>
) {

    private val personerMedUtbetaling =
        utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.map { it.person }

    fun genererBrevPeriodeDto(): BrevPeriodeDto? {
        val begrunnelserOgFritekster = hentBegrunnelseDtoer() + hentFritekstBegrunnelseDtoer()

        if (begrunnelserOgFritekster.isEmpty()) return null

        val tomDato =
            if (utvidetVedtaksperiodeMedBegrunnelser.tom?.erSenereEnnInneværendeMåned() == false) {
                utvidetVedtaksperiodeMedBegrunnelser.tom.tilDagMånedÅr()
            } else {
                null
            }

        val identerIBegrunnelene = emptyList<String>()

        return byggBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster.sorted(),
            identerIBegrunnelene = identerIBegrunnelene
        )
    }

    private fun hentFritekstBegrunnelseDtoer() =
        utvidetVedtaksperiodeMedBegrunnelser.fritekster.map { FritekstBegrunnelseDto(it) }

    private fun byggBrevPeriode(
        tomDato: String?,
        begrunnelserOgFritekster: List<BegrunnelseDto>,
        identerIBegrunnelene: List<String>
    ): BrevPeriodeDto {
        val (utbetalingerBarn, nullutbetalingerBarn) = utvidetVedtaksperiodeMedBegrunnelser
            .utbetalingsperiodeDetaljer
            .filter { it.person.type == PersonType.BARN }
            .partition { it.utbetaltPerMnd != 0 }

        val barnMedUtbetaling = utbetalingerBarn.map { it.person }
        val barnMedNullutbetaling = nullutbetalingerBarn.map { it.person }

        val barnIPeriode: List<Person> = when (utvidetVedtaksperiodeMedBegrunnelser.type) {
            Vedtaksperiodetype.UTBETALING -> finnBarnIUtbetalingPeriode(identerIBegrunnelene)

            Vedtaksperiodetype.OPPHØR -> emptyList()
            Vedtaksperiodetype.AVSLAG -> emptyList()
            Vedtaksperiodetype.FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
        }

        val utbetalingsbeløp =
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.sumOf { it.utbetaltPerMnd }
        val brevPeriodeType =
            hentPeriodetype(utvidetVedtaksperiodeMedBegrunnelser.fom, barnMedUtbetaling, utbetalingsbeløp)
        return BrevPeriodeDto(

            fom = this.hentFomTekst(),
            tom = when {
                utvidetVedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.FORTSATT_INNVILGET -> ""
                tomDato.isNullOrBlank() -> ""
                brevPeriodeType == BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING -> " til $tomDato"
                else -> "til $tomDato "
            },
            belop = formaterBeløp(utbetalingsbeløp),
            begrunnelser = begrunnelserOgFritekster,
            brevPeriodeType = brevPeriodeType,
            antallBarn = barnIPeriode.size.toString(),
            barnasFodselsdager = barnIPeriode.tilBarnasFødselsdatoer(),
            antallBarnMedUtbetaling = barnMedUtbetaling.size.toString(),
            antallBarnMedNullutbetaling = barnMedNullutbetaling.size.toString(),
            fodselsdagerBarnMedUtbetaling = barnMedUtbetaling.tilBarnasFødselsdatoer(),
            fodselsdagerBarnMedNullutbetaling = barnMedNullutbetaling.tilBarnasFødselsdatoer()
        )
    }

    private fun hentFomTekst(): String = when (utvidetVedtaksperiodeMedBegrunnelser.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> "Du får:"
        Vedtaksperiodetype.UTBETALING -> utvidetVedtaksperiodeMedBegrunnelser.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.OPPHØR -> utvidetVedtaksperiodeMedBegrunnelser.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.AVSLAG -> if (utvidetVedtaksperiodeMedBegrunnelser.fom != null) utvidetVedtaksperiodeMedBegrunnelser.fom.tilDagMånedÅr() else ""
    }

    private fun hentPeriodetype(
        fom: LocalDate?,
        barnMedUtbetaling: List<Person>,
        utbetalingsbeløp: Int
    ) =
        when (utvidetVedtaksperiodeMedBegrunnelser.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET -> BrevPeriodeType.FORTSATT_INNVILGET
            Vedtaksperiodetype.UTBETALING -> when {
                utbetalingsbeløp == 0 -> BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING
                barnMedUtbetaling.isEmpty() -> BrevPeriodeType.INNVILGELSE_KUN_UTBETALING_PÅ_SØKER
                else -> BrevPeriodeType.INNVILGELSE
            }

            Vedtaksperiodetype.AVSLAG -> if (fom != null) BrevPeriodeType.AVSLAG else BrevPeriodeType.AVSLAG_UTEN_PERIODE
            Vedtaksperiodetype.OPPHØR -> BrevPeriodeType.OPPHOR
        }

    fun finnBarnIUtbetalingPeriode(identerIBegrunnelene: List<String>): List<Person> {
        val identerMedUtbetaling =
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.map { it.person.aktør.aktivFødselsnummer() }

        val barnIPeriode = (identerIBegrunnelene + identerMedUtbetaling)
            .toSet()
            .mapNotNull { personIdent ->
                persongrunnlag.personer.find { it.aktør.aktivFødselsnummer() == personIdent }
            }
            .filter { it.type == PersonType.BARN }

        return barnIPeriode
    }

    fun hentBarnasFødselsdagerForBegrunnelse(
        gjelderSøker: Boolean,
        personerMedVilkårSomPasserBegrunnelse: Collection<Person>,
        begrunnelse: Begrunnelse
    ): List<LocalDate> = when {
        begrunnelse == Begrunnelse.AVSLAG_UREGISTRERT_BARN ->
            uregistrerteBarn.mapNotNull { it.fødselsdato }

        gjelderSøker &&
            begrunnelse.begrunnelseType != BegrunnelseType.ENDRET_UTBETALING &&
            begrunnelse.begrunnelseType != BegrunnelseType.ETTER_ENDRET_UTBETALING -> {
            if (begrunnelse.begrunnelseType == BegrunnelseType.AVSLAG) {
                persongrunnlag.personer
                    .filter { it.type == PersonType.BARN }
                    .map { it.fødselsdato } +
                    uregistrerteBarn.mapNotNull { it.fødselsdato }
            } else {
                (personerMedUtbetaling + personerMedVilkårSomPasserBegrunnelse).toSet()
                    .filter { it.type == PersonType.BARN }
                    .map { it.fødselsdato }
            }
        }

        else ->
            personerMedVilkårSomPasserBegrunnelse
                .filter { it.type == PersonType.BARN }
                .map { it.fødselsdato }
    }

    fun hentAntallBarnForBegrunnelse(
        gjelderSøker: Boolean,
        barnasFødselsdatoer: List<LocalDate>,
        begrunnelse: Begrunnelse
    ): Int {
        val erAvslagUregistrerteBarn =
            begrunnelse == Begrunnelse.AVSLAG_UREGISTRERT_BARN

        return when {
            erAvslagUregistrerteBarn -> uregistrerteBarn.size
            gjelderSøker && begrunnelse.begrunnelseType == BegrunnelseType.AVSLAG -> 0
            else -> barnasFødselsdatoer.size
        }
    }

    private fun hentBeløp(
        begrunnelse: Begrunnelse
    ) = if (begrunnelse.begrunnelseType == BegrunnelseType.AVSLAG ||
        begrunnelse.begrunnelseType == BegrunnelseType.OPPHØR
    ) {
        0
    } else {
        utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.sumOf { it.utbetaltPerMnd }
    }

    private fun validerBrevbegrunnelse(
        gjelderSøker: Boolean,
        barnasFødselsdatoer: List<LocalDate>,
        sanityBegrunnelse: SanityBegrunnelse,
        begrunnelse: Begrunnelse
    ) {
        if (!gjelderSøker && barnasFødselsdatoer.isEmpty() &&
            !sanityBegrunnelse.triggere.contains(Trigger.SATSENDRING) &&
            begrunnelse != Begrunnelse.AVSLAG_UREGISTRERT_BARN
        ) {
            throw IllegalStateException("Ingen personer på brevbegrunnelse $begrunnelse")
        }
    }

    private fun endringsperioderSomPasserMedPeriodeDatoOgBegrunnelse(begrunnelse: Begrunnelse): List<EndretUtbetalingAndel> {
        val endredeUtbetalinger = andelTilkjentYtelserMedEndreteUtbetalinger.flatMap { it.endreteUtbetalinger }

        return when (begrunnelse.begrunnelseType) {
            BegrunnelseType.ETTER_ENDRET_UTBETALING -> {
                endredeUtbetalinger.filter {
                    it.tom?.sisteDagIInneværendeMåned()
                        ?.erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom?.førsteDagIInneværendeMåned()) == true
                }
            }

            BegrunnelseType.ENDRET_UTBETALING -> {
                endredeUtbetalinger.filter {
                    it.periode.overlapperHeltEllerDelvisMed(
                        MånedPeriode(
                            (utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN).tilYearMonth(),
                            (utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE).tilYearMonth()
                        )
                    )
                }
            }

            else -> emptyList()
        }
    }

    private val begrunnelserForPeriodeContext = BegrunnelserForPeriodeContext(
        utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
        sanityBegrunnelser = sanityBegrunnelser,
        personopplysningGrunnlag = persongrunnlag,
        personResultater = personResultater
    )

    fun hentBegrunnelseDtoer(): List<BegrunnelseDataDto> {
        return utvidetVedtaksperiodeMedBegrunnelser
            .begrunnelser
            .mapNotNull { vedtakBegrunnelse ->
                val nullableSanitybegrunnelse = vedtakBegrunnelse.begrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)
                nullableSanitybegrunnelse?.let { Pair(vedtakBegrunnelse.begrunnelse, it) }
            }.map { (begrunnelse, sanityBegrunnelse) ->
                val personerMedVilkårSomPasserBegrunnelse = begrunnelserForPeriodeContext
                    .hentPersonerMedVilkårResultaterSomPasserMedBegrunnelseOgPeriode(
                        begrunnelse = begrunnelse,
                        sanityBegrunnelse = sanityBegrunnelse
                    )

                val antallTimerBarnehageplass =
                    hentAntallTimerBarnehageplassTekst(personerMedVilkårSomPasserBegrunnelse)

                val gjelderSøker = personerMedVilkårSomPasserBegrunnelse.any { it.type == PersonType.SØKER }

                val barnasFødselsdatoer = hentBarnasFødselsdagerForBegrunnelse(
                    gjelderSøker = gjelderSøker,
                    personerMedVilkårSomPasserBegrunnelse = personerMedVilkårSomPasserBegrunnelse,
                    begrunnelse = begrunnelse
                )

                validerBrevbegrunnelse(
                    gjelderSøker = gjelderSøker,
                    barnasFødselsdatoer = barnasFødselsdatoer,
                    sanityBegrunnelse = sanityBegrunnelse,
                    begrunnelse = begrunnelse
                )

                BegrunnelseDataDto(
                    gjelderSoker = gjelderSøker,
                    barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
                    antallBarn = hentAntallBarnForBegrunnelse(
                        gjelderSøker = gjelderSøker,
                        barnasFødselsdatoer = barnasFødselsdatoer,
                        begrunnelse = begrunnelse
                    ),
                    maalform = persongrunnlag.søker.målform.tilSanityFormat(),
                    apiNavn = begrunnelse.sanityApiNavn,
                    belop = formaterBeløp(hentBeløp(begrunnelse)),
                    vedtakBegrunnelseType = begrunnelse.begrunnelseType,
                    antallTimerBarnehageplass = antallTimerBarnehageplass,
                    sanityBegrunnelseType = sanityBegrunnelse.type
                )
            }
    }

    private fun hentAntallTimerBarnehageplassTekst(personerMedVilkårSomPasserBegrunnelse: Set<Person>) =
        slåSammen(
            personerMedVilkårSomPasserBegrunnelse
                .filter { it.type == PersonType.BARN }
                .sortedBy { it.fødselsdato }
                .map { it.hentAntallTimerBarnehageplass().toString() }
        )

    private fun Person.hentAntallTimerBarnehageplass(): BigDecimal {
        val forskjøvetBarnehageplassPeriodeSomErSamtidigSomVedtaksperiode =
            hentForskjøvedeVilkårResultaterSomErSamtidigSomVedtaksperiode()[this.aktør]
                ?.get(Vilkår.BARNEHAGEPLASS)
                ?.tilPerioderIkkeNull()

        return forskjøvetBarnehageplassPeriodeSomErSamtidigSomVedtaksperiode
            ?.singleOrNull() // Skal være maks ett barnehageresultat i periode, ellers burde vi ha splittet opp vedtaksperioden.
            ?.verdi?.antallTimer ?: BigDecimal.ZERO
    }

    private fun hentForskjøvedeVilkårResultater(): Map<Aktør, Map<Vilkår, Tidslinje<VilkårResultat>>> {
        return personResultater.associate { personResultat ->
            val vilkårTilVilkårResultaterMap = personResultat.vilkårResultater.groupBy { it.vilkårType }

            personResultat.aktør to vilkårTilVilkårResultaterMap.mapValues { (vilkår, vilkårResultater) ->
                forskyvVilkårResultater(vilkår, vilkårResultater).tilTidslinje()
            }
        }
    }

    private fun hentForskjøvedeVilkårResultaterSomErSamtidigSomVedtaksperiode(): Map<Aktør, Map<Vilkår, Tidslinje<VilkårResultat>>> {
        val vedtaksperiodeTidslinje = listOf(
            Periode(
                verdi = utvidetVedtaksperiodeMedBegrunnelser,
                fom = utvidetVedtaksperiodeMedBegrunnelser.fom,
                tom = utvidetVedtaksperiodeMedBegrunnelser.tom
            )
        ).tilTidslinje()

        val personTilVilkårResultaterMap = hentForskjøvedeVilkårResultater()

        return personTilVilkårResultaterMap.mapValues { (_, vilkårResultaterMap) ->
            vilkårResultaterMap.mapValues { (_, vilkårResultatTidslinje) ->
                vilkårResultatTidslinje.kombinerMed(vedtaksperiodeTidslinje) { vilkårResultat, vedtaksperiode ->
                    vedtaksperiode?.let { vilkårResultat }
                }
            }
        }
    }
}
