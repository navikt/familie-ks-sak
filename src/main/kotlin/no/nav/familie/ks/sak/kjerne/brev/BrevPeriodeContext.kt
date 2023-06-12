package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.common.exception.Feil
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
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.tilMånedÅr
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.integrasjon.sanity.domene.inneholderGjelderFørstePeriodeTrigger
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvVilkårResultater
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
    private val barnSomDødeIForrigePeriode: List<Person>,
    private val erFørsteVedtaksperiode: Boolean
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
                personerMedVilkårSomPasserBegrunnelse
                    .filter { it.type == PersonType.BARN }
                    .map { it.fødselsdato }
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
        barnasFødselsdatoer: List<LocalDate>,
        begrunnelse: Begrunnelse
    ): Int {
        val erAvslagUregistrerteBarn =
            begrunnelse == Begrunnelse.AVSLAG_UREGISTRERT_BARN

        return when {
            erAvslagUregistrerteBarn -> uregistrerteBarn.size
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
            begrunnelse != Begrunnelse.AVSLAG_UREGISTRERT_BARN &&
            begrunnelse != Begrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE
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
        personResultater = personResultater,
        endretUtbetalingsandeler = andelTilkjentYtelserMedEndreteUtbetalinger.flatMap { it.endreteUtbetalinger },
        erFørsteVedtaksperiode = erFørsteVedtaksperiode
    )

    fun hentBegrunnelseDtoer(): List<BegrunnelseDataDto> {
        return utvidetVedtaksperiodeMedBegrunnelser
            .begrunnelser
            .mapNotNull { vedtakBegrunnelse ->
                val nullableSanitybegrunnelse = vedtakBegrunnelse.begrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)
                nullableSanitybegrunnelse?.let { Pair(vedtakBegrunnelse.begrunnelse, it) }
            }.map { (begrunnelse, sanityBegrunnelse) ->

                val relevantePersoner =
                    when (begrunnelse.begrunnelseType) {
                        BegrunnelseType.ETTER_ENDRET_UTBETALING -> begrunnelserForPeriodeContext.hentPersonerMedEndretUtbetalingerSomPasserMedVedtaksperiode(
                            sanityBegrunnelse
                        )

                        else -> begrunnelserForPeriodeContext.hentPersonerMedVilkårResultaterSomPasserMedBegrunnelseOgPeriode(
                            begrunnelse = begrunnelse,
                            sanityBegrunnelse = sanityBegrunnelse
                        )
                    }

                val vilkårResultaterForRelevantePersoner = personResultater
                    .filter { relevantePersoner.map { person -> person.aktør }.contains(it.aktør) }
                    .flatMap { it.vilkårResultater }

                val antallTimerBarnehageplass =
                    hentAntallTimerBarnehageplassTekst(relevantePersoner)

                val relevanteEndringsperioderForBegrunnelse = begrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
                    sanityBegrunnelse = sanityBegrunnelse,
                    endretUtbetalingAndeler = andelTilkjentYtelserMedEndreteUtbetalinger.flatMap { it.endreteUtbetalinger },
                    vedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser
                )

                val søknadstidspunkt =
                    relevanteEndringsperioderForBegrunnelse.minOfOrNull { it.søknadstidspunkt!! }

                val gjelderSøker = relevantePersoner.any { it.type == PersonType.SØKER }
                val gjelderAndreForelder = relevantePersoner.filter { it.type == PersonType.BARN }
                    .any {
                        if (begrunnelse.begrunnelseType == BegrunnelseType.AVSLAG) it.erMedlemskapVurdertPåAndreforelderSamtidigSomAvslag() else it.erMedlemskapVurdertPåAndreforelder()
                    }

                val barnasFødselsdatoer = hentBarnasFødselsdagerForBegrunnelse(
                    gjelderSøker = gjelderSøker,
                    personerMedVilkårSomPasserBegrunnelse = relevantePersoner,
                    begrunnelse = begrunnelse
                )

                val maanedOgAarBegrunnelsenGjelderFor = this.utvidetVedtaksperiodeMedBegrunnelser.fom?.let { fom ->
                    hentMånedOgÅrForBegrunnelse(
                        vedtaksperiodeType = this.utvidetVedtaksperiodeMedBegrunnelser.type,
                        sanityBegrunnelse = sanityBegrunnelse,
                        vilkårResultaterForRelevantePersoner = vilkårResultaterForRelevantePersoner,
                        tom = this.utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE,
                        fom = fom
                    )
                }

                validerBrevbegrunnelse(
                    gjelderSøker = gjelderSøker,
                    barnasFødselsdatoer = barnasFødselsdatoer,
                    sanityBegrunnelse = sanityBegrunnelse,
                    begrunnelse = begrunnelse
                )

                BegrunnelseDataDto(
                    gjelderSoker = gjelderSøker,
                    gjelderAndreForelder = gjelderAndreForelder,
                    barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
                    antallBarn = hentAntallBarnForBegrunnelse(
                        barnasFødselsdatoer = barnasFødselsdatoer,
                        begrunnelse = begrunnelse
                    ),
                    maanedOgAarBegrunnelsenGjelderFor = maanedOgAarBegrunnelsenGjelderFor,
                    maalform = persongrunnlag.søker.målform.tilSanityFormat(),
                    apiNavn = begrunnelse.sanityApiNavn,
                    belop = formaterBeløp(hentBeløp(begrunnelse)),
                    vedtakBegrunnelseType = begrunnelse.begrunnelseType,
                    antallTimerBarnehageplass = antallTimerBarnehageplass,
                    soknadstidspunkt = søknadstidspunkt?.tilKortString() ?: "",
                    sanityBegrunnelseType = sanityBegrunnelse.type
                )
            }
    }

    private fun hentMånedOgÅrForBegrunnelse(
        vedtaksperiodeType: Vedtaksperiodetype,
        sanityBegrunnelse: SanityBegrunnelse,
        vilkårResultaterForRelevantePersoner: List<VilkårResultat>,
        fom: LocalDate,
        tom: LocalDate
    ): String =
        when (vedtaksperiodeType) {
            Vedtaksperiodetype.AVSLAG ->
                if (fom == TIDENES_MORGEN && tom == TIDENES_ENDE) {
                    ""
                } else if (tom == TIDENES_ENDE) {
                    fom.tilMånedÅr()
                } else {
                    "${fom.tilMånedÅr()} til ${tom.tilMånedÅr()}"
                }

            Vedtaksperiodetype.OPPHØR -> {
                kastFeilHvisFomErUgyldig(fom)
                if (sanityBegrunnelse.inneholderGjelderFørstePeriodeTrigger()) {
                    hentTidligesteFomSomIkkeErOppfyltOgOverstiger33Timer(vilkårResultaterForRelevantePersoner, fom)
                } else {
                    fom.tilMånedÅr()
                }
            }

            Vedtaksperiodetype.UTBETALING,
            Vedtaksperiodetype.FORTSATT_INNVILGET -> {
                kastFeilHvisFomErUgyldig(fom)
                fom.tilMånedÅr()
            }
        }

    private fun hentTidligesteFomSomIkkeErOppfyltOgOverstiger33Timer(
        vilkårResultaterForRelevantePersoner: List<VilkårResultat>,
        fom: LocalDate
    ): String = vilkårResultaterForRelevantePersoner
        .filter {
            val vilkårResultatErIkkeOppfylt = it.resultat == Resultat.IKKE_OPPFYLT
            val vilkårResultatOverstiger33Timer = (it.antallTimer ?: BigDecimal(0)) >= BigDecimal(33)

            vilkårResultatErIkkeOppfylt && vilkårResultatOverstiger33Timer
        }
        .minOf { it.periodeFom ?: fom }
        .tilMånedÅr()

    private fun kastFeilHvisFomErUgyldig(fom: LocalDate) {
        if (fom == TIDENES_MORGEN) {
            throw Feil("Prøver å finne fom-dato for begrunnelse, men fikk \"TIDENES_MORGEN\".")
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

    private fun Person.erMedlemskapVurdertPåAndreforelder(): Boolean {
        val forskjøvetMedlemskapPåAnnenForelderPeriodeSomErSamtidigSomVedtaksperiode =
            hentForskjøvedeVilkårResultaterSomErSamtidigSomVedtaksperiode()[this.aktør]
                ?.get(Vilkår.MEDLEMSKAP_ANNEN_FORELDER)
                ?.tilPerioderIkkeNull()

        return forskjøvetMedlemskapPåAnnenForelderPeriodeSomErSamtidigSomVedtaksperiode?.any { it.verdi.resultat == Resultat.OPPFYLT }
            ?: false
    }

    private fun Person.erMedlemskapVurdertPåAndreforelderSamtidigSomAvslag(): Boolean {
        val alleMedlemskapAnnenForelderVilkår = personResultater.flatMap { it.vilkårResultater }
            .filter { it.vilkårType == Vilkår.MEDLEMSKAP_ANNEN_FORELDER }

        return alleMedlemskapAnnenForelderVilkår.any {
            it.periodeFom == utvidetVedtaksperiodeMedBegrunnelser.fom && it.resultat == Resultat.IKKE_OPPFYLT
        }
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

private fun Begrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    vedtaksperiode: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelse: SanityBegrunnelse
): List<EndretUtbetalingAndel> = when (this.begrunnelseType) {
    BegrunnelseType.ETTER_ENDRET_UTBETALING -> {
        endretUtbetalingAndeler.filter {
            it.periode.tom.sisteDagIInneværendeMåned()
                ?.erDagenFør(vedtaksperiode.fom?.førsteDagIInneværendeMåned()) == true &&
                sanityBegrunnelse.endringsårsaker.contains(it.årsak)
        }
    }

    BegrunnelseType.AVSLAG -> {
        endretUtbetalingAndeler.filter {
            it.periode.fom == vedtaksperiode.fom?.toYearMonth()
        }
    }

    else -> emptyList()
}
