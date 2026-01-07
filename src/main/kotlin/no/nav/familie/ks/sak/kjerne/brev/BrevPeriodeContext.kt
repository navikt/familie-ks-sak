package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.erSenereEnnInneværendeMåned
import no.nav.familie.ks.sak.common.util.formaterBeløp
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.tilMånedÅr
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.integrasjon.sanity.domene.begrunnelseGjelderOpphørFraForrigeBehandling
import no.nav.familie.ks.sak.integrasjon.sanity.domene.inneholderGjelderFørstePeriodeTrigger
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.forskyvVilkårResultater
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelserForPeriodeContext
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelseMedKompetanseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelseUtenKompetanseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.FritekstBegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalOgFellesBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.erAvslagEllerEøsAvslag
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilBrevTekst
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeType
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.tilBarnasFødselsdatoer
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.hentVerdier
import no.nav.familie.tidslinje.utvidelser.klipp
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal
import java.time.LocalDate

class BrevPeriodeContext(
    private val utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val personopplysningGrunnlag: PersonopplysningGrunnlag,
    private val personResultater: List<PersonResultat>,
    private val andelTilkjentYtelserMedEndreteUtbetalinger: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    private val uregistrerteBarn: List<BarnMedOpplysningerDto>,
    private val kompetanser: List<UtfyltKompetanse>,
    private val landkoder: Map<String, String>,
    private val erFørsteVedtaksperiode: Boolean,
    private val overgangsordningAndeler: List<OvergangsordningAndel>,
    private val adopsjonerIBehandling: List<Adopsjon>,
) {
    private val personerMedUtbetaling =
        utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.map { it.person }

    fun genererBrevPeriodeDto(): BrevPeriodeDto? {
        val begrunnelserOgFritekster = hentNasjonalOgFellesBegrunnelseDtoer() + hentEøsBegrunnelseDataDtoer() + hentFritekstBegrunnelseDtoer()

        if (begrunnelserOgFritekster.isEmpty()) return null

        val tomDato =
            if (utvidetVedtaksperiodeMedBegrunnelser.tom?.erSenereEnnInneværendeMåned() == false ||
                utvidetVedtaksperiodeMedBegrunnelser.inneholderOvergangsordningBegrunnelser()
            ) {
                utvidetVedtaksperiodeMedBegrunnelser.tom?.tilDagMånedÅr()
            } else {
                null
            }

        val identerIBegrunnelene = emptyList<String>()

        return byggBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster.sorted(),
            identerIBegrunnelene = identerIBegrunnelene,
        )
    }

    private fun hentFritekstBegrunnelseDtoer() = utvidetVedtaksperiodeMedBegrunnelser.fritekster.map { FritekstBegrunnelseDto(it) }

    private fun byggBrevPeriode(
        tomDato: String?,
        begrunnelserOgFritekster: List<BegrunnelseDto>,
        identerIBegrunnelene: List<String>,
    ): BrevPeriodeDto {
        val (utbetalingerBarn, nullutbetalingerBarn) =
            utvidetVedtaksperiodeMedBegrunnelser
                .utbetalingsperiodeDetaljer
                .filter { it.person.type == PersonType.BARN }
                .partition { it.utbetaltPerMnd != 0 }

        val barnMedUtbetaling = utbetalingerBarn.map { it.person }
        val barnMedNullutbetaling = nullutbetalingerBarn.map { it.person }

        val barnIPeriode: List<Person> =
            when (utvidetVedtaksperiodeMedBegrunnelser.type) {
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
            tom =
                when {
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
            fodselsdagerBarnMedNullutbetaling = barnMedNullutbetaling.tilBarnasFødselsdatoer(),
        )
    }

    private fun hentFomTekst(): String =
        when (utvidetVedtaksperiodeMedBegrunnelser.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET -> "Du får:"
            Vedtaksperiodetype.UTBETALING -> utvidetVedtaksperiodeMedBegrunnelser.fom!!.tilDagMånedÅr()
            Vedtaksperiodetype.OPPHØR -> utvidetVedtaksperiodeMedBegrunnelser.fom!!.tilDagMånedÅr()
            Vedtaksperiodetype.AVSLAG -> if (utvidetVedtaksperiodeMedBegrunnelser.fom != null) utvidetVedtaksperiodeMedBegrunnelser.fom.tilDagMånedÅr() else ""
        }

    private fun hentPeriodetype(
        fom: LocalDate?,
        barnMedUtbetaling: List<Person>,
        utbetalingsbeløp: Int,
    ) = when (utvidetVedtaksperiodeMedBegrunnelser.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> {
            BrevPeriodeType.FORTSATT_INNVILGET
        }

        Vedtaksperiodetype.UTBETALING -> {
            when {
                utbetalingsbeløp == 0 -> BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING
                barnMedUtbetaling.isEmpty() -> BrevPeriodeType.INNVILGELSE_KUN_UTBETALING_PÅ_SØKER
                else -> BrevPeriodeType.INNVILGELSE
            }
        }

        Vedtaksperiodetype.AVSLAG -> {
            if (fom != null) BrevPeriodeType.AVSLAG else BrevPeriodeType.AVSLAG_UTEN_PERIODE
        }

        Vedtaksperiodetype.OPPHØR -> {
            BrevPeriodeType.OPPHOR
        }
    }

    fun finnBarnIUtbetalingPeriode(identerIBegrunnelene: List<String>): List<Person> {
        val identerMedUtbetaling =
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.map { it.person.aktør.aktivFødselsnummer() }

        val barnIPeriode =
            (identerIBegrunnelene + identerMedUtbetaling)
                .toSet()
                .mapNotNull { personIdent ->
                    personopplysningGrunnlag.personer.find { it.aktør.aktivFødselsnummer() == personIdent }
                }.filter { it.type == PersonType.BARN }

        return barnIPeriode
    }

    fun hentBarnasFødselsdagerForBegrunnelse(
        gjelderSøker: Boolean,
        personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse: Collection<Person>,
        begrunnelse: IBegrunnelse,
    ): List<LocalDate> =
        when {
            begrunnelse == NasjonalEllerFellesBegrunnelse.AVSLAG_UREGISTRERT_BARN -> {
                uregistrerteBarn.mapNotNull { it.fødselsdato }
            }

            else -> {
                hentBarnSomSkalIBegrunnelse(gjelderSøker, personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse, begrunnelse).map { it.fødselsdato }
            }
        }

    fun hentBarnSomSkalIBegrunnelse(
        gjelderSøker: Boolean,
        personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse: Collection<Person>,
        begrunnelse: IBegrunnelse,
    ): List<Person> =
        when {
            gjelderSøker &&
                begrunnelse.begrunnelseType != BegrunnelseType.ENDRET_UTBETALING &&
                begrunnelse.begrunnelseType != BegrunnelseType.ETTER_ENDRET_UTBETALING -> {
                if (begrunnelse.begrunnelseType.erAvslagEllerEøsAvslag()) {
                    personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse
                        .filter { it.type == PersonType.BARN }
                } else {
                    (personerMedUtbetaling + personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse)
                        .toSet()
                        .filter { it.type == PersonType.BARN }
                }
            }

            else -> {
                personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse
                    .filter { it.type == PersonType.BARN }
            }
        }

    fun hentAntallBarnForBegrunnelse(
        barnasFødselsdatoer: List<LocalDate>,
        begrunnelse: IBegrunnelse,
    ): Int {
        val erAvslagUregistrerteBarn =
            begrunnelse == NasjonalEllerFellesBegrunnelse.AVSLAG_UREGISTRERT_BARN

        return when {
            erAvslagUregistrerteBarn -> uregistrerteBarn.size
            else -> barnasFødselsdatoer.size
        }
    }

    private fun hentBeløp(nasjonalEllerFellesBegrunnelse: NasjonalEllerFellesBegrunnelse) =
        if (nasjonalEllerFellesBegrunnelse.begrunnelseType == BegrunnelseType.AVSLAG ||
            nasjonalEllerFellesBegrunnelse.begrunnelseType == BegrunnelseType.OPPHØR
        ) {
            0
        } else {
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.sumOf { it.utbetaltPerMnd }
        }

    private fun validerBrevbegrunnelse(
        gjelderSøker: Boolean,
        barnasFødselsdatoer: List<LocalDate>,
        sanityBegrunnelse: SanityBegrunnelse,
        nasjonalEllerFellesBegrunnelse: IBegrunnelse,
    ) {
        if (!gjelderSøker &&
            barnasFødselsdatoer.isEmpty() &&
            !sanityBegrunnelse.triggere.contains(Trigger.SATSENDRING) &&
            nasjonalEllerFellesBegrunnelse != NasjonalEllerFellesBegrunnelse.AVSLAG_UREGISTRERT_BARN &&
            nasjonalEllerFellesBegrunnelse != NasjonalEllerFellesBegrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE &&
            nasjonalEllerFellesBegrunnelse != NasjonalEllerFellesBegrunnelse.AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024
        ) {
            throw FunksjonellFeil("Begrunnelsen '${sanityBegrunnelse.apiNavn}' passer ikke vedtaksperioden. Hvis du mener dette er feil ta kontakt med team BAKS.")
        }
    }

    private val begrunnelserForPeriodeContext =
        BegrunnelserForPeriodeContext(
            utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
            sanityBegrunnelser = sanityBegrunnelser,
            kompetanser = kompetanser,
            personopplysningGrunnlag = personopplysningGrunnlag,
            adopsjonerIBehandling = adopsjonerIBehandling,
            overgangsordningAndeler = overgangsordningAndeler,
            personResultater = personResultater,
            endretUtbetalingsandeler = andelTilkjentYtelserMedEndreteUtbetalinger.flatMap { it.endreteUtbetalinger },
            erFørsteVedtaksperiode = erFørsteVedtaksperiode,
            andelerTilkjentYtelse = andelTilkjentYtelserMedEndreteUtbetalinger,
        )

    fun hentNasjonalOgFellesBegrunnelseDtoer(): List<NasjonalOgFellesBegrunnelseDataDto> =
        utvidetVedtaksperiodeMedBegrunnelser
            .begrunnelser
            .mapNotNull { vedtakBegrunnelse ->
                val nullableSanitybegrunnelse = vedtakBegrunnelse.nasjonalEllerFellesBegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)
                nullableSanitybegrunnelse?.let { Pair(vedtakBegrunnelse.nasjonalEllerFellesBegrunnelse, it) }
            }.map { (begrunnelse, sanityBegrunnelse) ->

                val relevantePersoner = hentRelevantePersonerForNasjonalOgFellesBegrunnelse(begrunnelse, sanityBegrunnelse)

                val vilkårResultaterForRelevantePersoner =
                    personResultater
                        .filter { relevantePersoner.map { person -> person.aktør }.contains(it.aktør) }
                        .flatMap { it.vilkårResultater }

                val antallTimerBarnehageplass =
                    hentAntallTimerBarnehageplassTekst(relevantePersoner)

                val relevanteEndringsperioderForBegrunnelse =
                    begrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
                        sanityBegrunnelse = sanityBegrunnelse,
                        endretUtbetalingAndeler = andelTilkjentYtelserMedEndreteUtbetalinger.flatMap { it.endreteUtbetalinger },
                        vedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser,
                    )

                val søknadstidspunkt =
                    relevanteEndringsperioderForBegrunnelse.minOfOrNull { it.søknadstidspunkt!! }

                val gjelderSøker = relevantePersoner.any { it.type == PersonType.SØKER }
                val gjelderAndreForelder =
                    relevantePersoner
                        .filter { it.type == PersonType.BARN }
                        .any {
                            if (begrunnelse.begrunnelseType == BegrunnelseType.AVSLAG) erMedlemskapVurdertPåAndreforelderSamtidigSomAvslag() else it.erMedlemskapVurdertPåAndreforelder()
                        }

                val barnasFødselsdatoer =
                    hentBarnasFødselsdagerForBegrunnelse(
                        gjelderSøker = gjelderSøker,
                        personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse = relevantePersoner,
                        begrunnelse = begrunnelse,
                    )

                val barnSomSkalIBegrunnelse = hentBarnSomSkalIBegrunnelse(gjelderSøker, relevantePersoner, begrunnelse)

                val alleLovverkForBarna =
                    barnSomSkalIBegrunnelse.map { barn ->
                        LovverkUtleder.utledLovverkForBarn(
                            fødselsdato = barn.fødselsdato,
                            adopsjonsdato = adopsjonerIBehandling.firstOrNull { it.aktør == barn.aktør }?.adopsjonsdato,
                        )
                    }

                val månedOgÅrBegrunnelsenGjelderFor =
                    this.utvidetVedtaksperiodeMedBegrunnelser.fom?.let { fom ->
                        hentMånedOgÅrForBegrunnelse(
                            vedtaksperiodeType = this.utvidetVedtaksperiodeMedBegrunnelser.type,
                            sanityBegrunnelse = sanityBegrunnelse,
                            vilkårResultaterForRelevantePersoner = vilkårResultaterForRelevantePersoner,
                            vedtaksperiodeFom = fom,
                            vedtaksperiodeTom = this.utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE,
                            endretUtbetalingAndeler = andelTilkjentYtelserMedEndreteUtbetalinger.flatMap { it.endreteUtbetalinger },
                            alleLovverkForBarna = alleLovverkForBarna,
                        )
                    }

                val månedOgÅrFørVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.fom?.minusMonths(1)?.tilMånedÅr()

                validerBrevbegrunnelse(
                    gjelderSøker = gjelderSøker,
                    barnasFødselsdatoer = barnasFødselsdatoer,
                    sanityBegrunnelse = sanityBegrunnelse,
                    nasjonalEllerFellesBegrunnelse = begrunnelse,
                )

                NasjonalOgFellesBegrunnelseDataDto(
                    gjelderSoker = gjelderSøker,
                    gjelderAndreForelder = gjelderAndreForelder,
                    barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
                    antallBarn =
                        hentAntallBarnForBegrunnelse(
                            barnasFødselsdatoer = barnasFødselsdatoer,
                            begrunnelse = begrunnelse,
                        ),
                    maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
                    maanedOgAarFoerVedtaksperiode = månedOgÅrFørVedtaksperiode,
                    maalform = personopplysningGrunnlag.søker.målform.tilSanityFormat(),
                    apiNavn = begrunnelse.sanityApiNavn,
                    belop = formaterBeløp(hentBeløp(begrunnelse)),
                    vedtakBegrunnelseType = begrunnelse.begrunnelseType,
                    antallTimerBarnehageplass = antallTimerBarnehageplass,
                    soknadstidspunkt = søknadstidspunkt?.tilKortString() ?: "",
                    sanityBegrunnelseType = sanityBegrunnelse.type,
                )
            }

    private fun hentRelevantePersonerForNasjonalOgFellesBegrunnelse(
        begrunnelse: IBegrunnelse,
        sanityBegrunnelse: SanityBegrunnelse,
    ) = when (begrunnelse.begrunnelseType) {
        BegrunnelseType.ETTER_ENDRET_UTBETALING -> {
            begrunnelserForPeriodeContext.hentPersonerMedEndretUtbetalingerSomPasserMedVedtaksperiode(sanityBegrunnelse)
        }

        BegrunnelseType.FORTSATT_INNVILGET -> {
            hentRelevantePersonerForFortsattInnvilget()
        }

        else -> {
            begrunnelserForPeriodeContext.hentPersonerSomPasserMedBegrunnelseOgPeriode(
                begrunnelse = begrunnelse,
                sanityBegrunnelse = sanityBegrunnelse,
            )
        }
    }

    private fun hentRelevantePersonerForEøsBegrunnelse(
        begrunnelse: IBegrunnelse,
        sanityBegrunnelse: SanityBegrunnelse,
    ) = when (begrunnelse.begrunnelseType) {
        BegrunnelseType.ETTER_ENDRET_UTBETALING -> {
            begrunnelserForPeriodeContext.hentPersonerMedEndretUtbetalingerSomPasserMedVedtaksperiode(sanityBegrunnelse)
        }

        BegrunnelseType.FORTSATT_INNVILGET -> {
            hentRelevantePersonerForFortsattInnvilget()
        }

        else -> {
            begrunnelserForPeriodeContext.hentPersonerSomPasserForKompetanseIPeriode(
                begrunnelse = begrunnelse,
                sanityBegrunnelse = sanityBegrunnelse,
            )
        }
    }

    private fun hentRelevantePersonerForFortsattInnvilget(): Set<Person> {
        val innvilgedeAndelerIPeriode =
            andelTilkjentYtelserMedEndreteUtbetalinger
                .filter { it.erInnvilget }
                .filter { utvidetVedtaksperiodeMedBegrunnelser.fom == null || it.stønadFom <= utvidetVedtaksperiodeMedBegrunnelser.fom.toYearMonth() }
                .filter { utvidetVedtaksperiodeMedBegrunnelser.tom == null || it.stønadTom >= utvidetVedtaksperiodeMedBegrunnelser.tom.toYearMonth() }

        val aktørerMedUtbetalingIVedtaksperiode = innvilgedeAndelerIPeriode.map { it.aktør }.toSet()
        return aktørerMedUtbetalingIVedtaksperiode.map { aktør -> personopplysningGrunnlag.personer.single { it.aktør == aktør } }.toSet()
    }

    fun hentEøsBegrunnelseDataDtoer(): List<EØSBegrunnelseDto> =
        this.utvidetVedtaksperiodeMedBegrunnelser
            .eøsBegrunnelser
            .mapNotNull { vedtakBegrunnelse ->
                val nullableSanitybegrunnelse = vedtakBegrunnelse.begrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)
                nullableSanitybegrunnelse?.let { Pair(vedtakBegrunnelse.begrunnelse, it) }
            }.flatMap { (begrunnelse, sanityBegrunnelse) ->
                val personerGjeldendeForBegrunnelse =
                    hentRelevantePersonerForNasjonalOgFellesBegrunnelse(begrunnelse, sanityBegrunnelse) +
                        hentRelevantePersonerForEøsBegrunnelse(begrunnelse, sanityBegrunnelse)

                val gjelderSøker = personerGjeldendeForBegrunnelse.any { it.type == PersonType.SØKER }

                val barnasFødselsdatoer =
                    hentBarnasFødselsdagerForBegrunnelse(
                        gjelderSøker = gjelderSøker,
                        personerMedVilkårEllerOvergangsordningSomPasserBegrunnelse = personerGjeldendeForBegrunnelse,
                        begrunnelse = begrunnelse,
                    )

                val kompetanserTilBarnIBegrunnelseTidslinjer =
                    personerGjeldendeForBegrunnelse
                        .filter { it.type == PersonType.BARN }
                        .associateWith { barn -> kompetanser.filter { barn.aktør in it.barnAktører }.tilTidslinje() }

                val begrunnelseGjelderSøkerOgOpphørFraForrigeBehandling = (sanityBegrunnelse.begrunnelseGjelderOpphørFraForrigeBehandling()) && gjelderSøker

                val relevanteKompetanser =
                    when (begrunnelse.begrunnelseType) {
                        BegrunnelseType.INNVILGET,
                        BegrunnelseType.EØS_INNVILGET,
                        BegrunnelseType.FORTSATT_INNVILGET,
                        BegrunnelseType.REDUKSJON,
                        BegrunnelseType.EØS_REDUKSJON,
                        BegrunnelseType.ENDRET_UTBETALING,
                        BegrunnelseType.ETTER_ENDRET_UTBETALING,
                        -> {
                            kompetanserTilBarnIBegrunnelseTidslinjer.values
                                .flatMap { it.klippEtterVedtaksperiode().hentVerdier() }
                                .filterNotNull()
                                .toSet()
                        }

                        BegrunnelseType.OPPHØR,
                        BegrunnelseType.EØS_OPPHØR,
                        BegrunnelseType.AVSLAG,
                        BegrunnelseType.EØS_AVSLAG,
                        -> {
                            if (begrunnelseGjelderSøkerOgOpphørFraForrigeBehandling) {
                                TODO("Må se på kompetansene i samme periode forrige behandling")
                            } else {
                                kompetanserTilBarnIBegrunnelseTidslinjer.values
                                    .mapNotNull { it.finnPeriodeSomAvsluttesRettFørVedtaksperioden()?.verdi }
                                    .toSet()
                            }
                        }
                    }

                validerBrevbegrunnelse(
                    gjelderSøker = gjelderSøker,
                    barnasFødselsdatoer = barnasFødselsdatoer,
                    sanityBegrunnelse = sanityBegrunnelse,
                    nasjonalEllerFellesBegrunnelse = begrunnelse,
                )

                val begrunnelseGjelderOpphørFraForrigeBehandling = sanityBegrunnelse.begrunnelseGjelderOpphørFraForrigeBehandling()

                if (relevanteKompetanser.isEmpty() && (begrunnelse.begrunnelseType.erAvslagEllerEøsAvslag() || begrunnelse.begrunnelseType == BegrunnelseType.EØS_OPPHØR)) {
                    val antallTimerBarnehageplass =
                        hentAntallTimerBarnehageplassTekst(personerGjeldendeForBegrunnelse)

                    val barnIBegrunnelse = personerGjeldendeForBegrunnelse.filter { it.type == PersonType.BARN }
                    val barnasFødselsdagerForAvslagOgOpphør =
                        hentBarnasFødselsdagerForAvslagOgOpphør(
                            barnIBegrunnelse = barnIBegrunnelse,
                            barnPåBehandling = personopplysningGrunnlag.barna,
                            uregistrerteBarn = uregistrerteBarn,
                            gjelderSøker = gjelderSøker,
                        )

                    listOf(
                        EØSBegrunnelseUtenKompetanseDto(
                            vedtakBegrunnelseType = begrunnelse.begrunnelseType,
                            apiNavn = begrunnelse.sanityApiNavn,
                            sanityBegrunnelseType = sanityBegrunnelse.type,
                            barnasFodselsdatoer = barnasFødselsdagerForAvslagOgOpphør.tilBrevTekst(),
                            antallBarn = barnIBegrunnelse.size,
                            maalform = personopplysningGrunnlag.søker.målform.tilSanityFormat(),
                            gjelderSoker = gjelderSøker,
                            antallTimerBarnehageplass = antallTimerBarnehageplass,
                        ),
                    )
                } else {
                    relevanteKompetanser.mapNotNull { kompetanse ->
                        val barnIBegrunnelseOgIKompetanse =
                            kompetanse.barnAktører
                                .mapNotNull { barnAktør ->
                                    if (gjelderSøker && begrunnelseGjelderOpphørFraForrigeBehandling) {
                                        personopplysningGrunnlag.barna
                                    } else {
                                        personerGjeldendeForBegrunnelse
                                    }.find { it.aktør == barnAktør }
                                }

                        val barnIBegrunnelseOgIKompetanseFødselsdato =
                            barnIBegrunnelseOgIKompetanse.map { it.fødselsdato }

                        if (barnIBegrunnelseOgIKompetanseFødselsdato.isNotEmpty()) {
                            val antallTimerBarnehageplass =
                                hentAntallTimerBarnehageplassTekst(barnIBegrunnelseOgIKompetanse.toSet())

                            EØSBegrunnelseMedKompetanseDto(
                                vedtakBegrunnelseType = begrunnelse.begrunnelseType,
                                apiNavn = begrunnelse.sanityApiNavn,
                                sanityBegrunnelseType = sanityBegrunnelse.type,
                                barnetsBostedsland = kompetanse.barnetsBostedsland.tilLandNavn(landkoder).navn,
                                annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
                                annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland?.tilLandNavn(landkoder)?.navn,
                                sokersAktivitet = kompetanse.søkersAktivitet,
                                sokersAktivitetsland = kompetanse.søkersAktivitetsland.tilLandNavn(landkoder).navn,
                                barnasFodselsdatoer = barnIBegrunnelseOgIKompetanseFødselsdato.tilBrevTekst(),
                                antallBarn = hentAntallBarnForBegrunnelse(barnasFødselsdatoer = barnIBegrunnelseOgIKompetanseFødselsdato, begrunnelse = begrunnelse),
                                maalform = personopplysningGrunnlag.søker.målform.tilSanityFormat(),
                                antallTimerBarnehageplass = antallTimerBarnehageplass,
                                erAnnenForelderOmfattetAvNorskLovgivning = kompetanse.erAnnenForelderOmfattetAvNorskLovgivning,
                            )
                        } else {
                            null
                        }
                    }
                }
            }

    private fun Tidslinje<UtfyltKompetanse>.finnPeriodeSomAvsluttesRettFørVedtaksperioden() =
        tilPerioderIkkeNull().singleOrNull {
            it.avsluttesMånedenFørVedtaksperioden()
        }

    private fun Periode<UtfyltKompetanse>.avsluttesMånedenFørVedtaksperioden() =
        this.tom != null &&
            this.tom!!.toYearMonth().plusMonths(1) == utvidetVedtaksperiodeMedBegrunnelser.fom?.toYearMonth()

    private fun <T> Tidslinje<T>.klippEtterVedtaksperiode() = klipp(utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN, utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE)

    fun hentBarnasFødselsdagerForAvslagOgOpphør(
        barnIBegrunnelse: List<Person>,
        barnPåBehandling: List<Person>,
        uregistrerteBarn: List<BarnMedOpplysningerDto>,
        gjelderSøker: Boolean,
    ): List<LocalDate> {
        val registrerteBarnFødselsdatoer =
            if (gjelderSøker) barnPåBehandling.map { it.fødselsdato } else barnIBegrunnelse.map { it.fødselsdato }
        val uregistrerteBarnFødselsdatoer =
            uregistrerteBarn.mapNotNull { it.fødselsdato }
        val alleBarnaFødselsdatoer = registrerteBarnFødselsdatoer + uregistrerteBarnFødselsdatoer
        return alleBarnaFødselsdatoer
    }

    private fun hentMånedOgÅrForBegrunnelse(
        vedtaksperiodeType: Vedtaksperiodetype,
        sanityBegrunnelse: SanityBegrunnelse,
        vilkårResultaterForRelevantePersoner: List<VilkårResultat>,
        vedtaksperiodeFom: LocalDate,
        vedtaksperiodeTom: LocalDate,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
        alleLovverkForBarna: List<Lovverk>,
    ): String {
        val fomErFørLovendring2024 = vedtaksperiodeFom.isBefore(DATO_LOVENDRING_2024)
        val månedenFørFom = vedtaksperiodeFom.minusMonths(1).tilMånedÅr()

        return when (vedtaksperiodeType) {
            Vedtaksperiodetype.AVSLAG -> {
                when {
                    vedtaksperiodeFom == TIDENES_MORGEN && vedtaksperiodeTom == TIDENES_ENDE -> {
                        ""
                    }

                    vedtaksperiodeTom == TIDENES_ENDE -> {
                        val vilkårMedEksplisitteAvslag = vilkårResultaterForRelevantePersoner.filter { it.erEksplisittAvslagPåSøknad == true }
                        val avslagsperiodeStarterMånedEtterVilkår =
                            vilkårMedEksplisitteAvslag.any {
                                it.periodeFom?.plusMonths(1)?.toYearMonth() == vedtaksperiodeFom.toYearMonth()
                            }

                        if (avslagsperiodeStarterMånedEtterVilkår) {
                            vedtaksperiodeFom.minusMonths(1).tilMånedÅr()
                        } else {
                            vedtaksperiodeFom.tilMånedÅr()
                        }
                    }

                    else -> {
                        "${vedtaksperiodeFom.tilMånedÅr()} til ${vedtaksperiodeTom.tilMånedÅr()}"
                    }
                }
            }

            Vedtaksperiodetype.OPPHØR -> {
                val opphørGrunnetFulltidsBarnehageplassAugust2024 =
                    endretUtbetalingAndeler.any { it.årsak == Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 } && vedtaksperiodeFom == DATO_LOVENDRING_2024

                kastFeilHvisFomErUgyldig(vedtaksperiodeFom)
                when {
                    sanityBegrunnelse.inneholderGjelderFørstePeriodeTrigger() -> hentSenesteFomSomIkkeErOppfyltOgOverstiger33Timer(vilkårResultaterForRelevantePersoner, vedtaksperiodeFom)
                    alleLovverkForBarna.all { it == Lovverk.LOVENDRING_FEBRUAR_2025 } -> vedtaksperiodeFom.tilMånedÅr()
                    opphørGrunnetFulltidsBarnehageplassAugust2024 -> vedtaksperiodeFom.tilMånedÅr()
                    fomErFørLovendring2024 -> vedtaksperiodeFom.tilMånedÅr()
                    else -> månedenFørFom
                }
            }

            Vedtaksperiodetype.UTBETALING,
            Vedtaksperiodetype.FORTSATT_INNVILGET,
            -> {
                kastFeilHvisFomErUgyldig(vedtaksperiodeFom)
                if (sanityBegrunnelse.resultat == SanityResultat.REDUKSJON && !fomErFørLovendring2024 && alleLovverkForBarna.none { it == Lovverk.LOVENDRING_FEBRUAR_2025 }) {
                    månedenFørFom
                } else {
                    vedtaksperiodeFom.tilMånedÅr()
                }
            }
        }
    }

    private fun hentSenesteFomSomIkkeErOppfyltOgOverstiger33Timer(
        vilkårResultaterForRelevantePersoner: List<VilkårResultat>,
        fom: LocalDate,
    ): String =
        vilkårResultaterForRelevantePersoner
            .filter {
                val vilkårResultatErIkkeOppfylt = it.resultat == Resultat.IKKE_OPPFYLT
                val vilkårResultatOverstiger33Timer = (it.antallTimer ?: BigDecimal(0)) >= BigDecimal(33)

                vilkårResultatErIkkeOppfylt && vilkårResultatOverstiger33Timer
            }.maxOfOrNull { it.periodeFom ?: fom }
            ?.tilMånedÅr() ?: fom.tilMånedÅr()

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
                .map { it.hentAntallTimerBarnehageplass().toString() },
        )

    private fun Person.hentAntallTimerBarnehageplass(): BigDecimal {
        val forskjøvetBarnehageplassPeriodeSomPasserVedtaksperiode =
            if (utvidetVedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.FORTSATT_INNVILGET) {
                hentForskjøvedeVilkårResultater()[this.aktør]
                    ?.get(Vilkår.BARNEHAGEPLASS)
                    ?.tilPerioderIkkeNull()
            } else {
                hentForskjøvedeVilkårResultaterSomErSamtidigSomVedtaksperiode()[this.aktør]
                    ?.get(Vilkår.BARNEHAGEPLASS)
                    ?.tilPerioderIkkeNull()
            }

        val antallTimerIBarnehageplassVilkår =
            forskjøvetBarnehageplassPeriodeSomPasserVedtaksperiode
                ?.maxByOrNull { it.fom ?: TIDENES_ENDE }
                ?.verdi
                ?.antallTimer

        val antallTimerIOvergangsordningsAndel =
            overgangsordningAndeler
                .find {
                    it.person == this &&
                        it.fom == utvidetVedtaksperiodeMedBegrunnelser.fom?.toYearMonth() &&
                        it.tom == utvidetVedtaksperiodeMedBegrunnelser.tom?.toYearMonth()
                }?.antallTimer

        return antallTimerIOvergangsordningsAndel
            ?: antallTimerIBarnehageplassVilkår
            ?: BigDecimal.ZERO
    }

    private fun Person.erMedlemskapVurdertPåAndreforelder(): Boolean {
        val forskjøvetMedlemskapPåAnnenForelderPeriodeSomErSamtidigSomVedtaksperiode =
            hentForskjøvedeVilkårResultaterSomErSamtidigSomVedtaksperiode()[this.aktør]
                ?.get(Vilkår.MEDLEMSKAP_ANNEN_FORELDER)
                ?.tilPerioderIkkeNull()

        return forskjøvetMedlemskapPåAnnenForelderPeriodeSomErSamtidigSomVedtaksperiode?.any { it.verdi.resultat == Resultat.OPPFYLT }
            ?: false
    }

    private fun erMedlemskapVurdertPåAndreforelderSamtidigSomAvslag(): Boolean {
        val alleMedlemskapAnnenForelderVilkår =
            personResultater
                .flatMap { it.vilkårResultater }
                .filter { it.vilkårType == Vilkår.MEDLEMSKAP_ANNEN_FORELDER }

        return alleMedlemskapAnnenForelderVilkår.any {
            it.periodeFom == utvidetVedtaksperiodeMedBegrunnelser.fom && it.resultat == Resultat.IKKE_OPPFYLT
        }
    }

    private fun hentForskjøvedeVilkårResultater(): Map<Aktør, Map<Vilkår, Tidslinje<VilkårResultat>>> =
        personResultater
            .forskyvVilkårResultater(
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = adopsjonerIBehandling,
            ).mapValues { entry -> entry.value.mapValues { it.value.tilTidslinje() } }

    private fun hentForskjøvedeVilkårResultaterSomErSamtidigSomVedtaksperiode(): Map<Aktør, Map<Vilkår, Tidslinje<VilkårResultat>>> {
        val vedtaksperiodeTidslinje =
            listOf(
                Periode(
                    verdi = utvidetVedtaksperiodeMedBegrunnelser,
                    fom = utvidetVedtaksperiodeMedBegrunnelser.fom,
                    tom = utvidetVedtaksperiodeMedBegrunnelser.tom,
                ),
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

private fun UtvidetVedtaksperiodeMedBegrunnelser.inneholderOvergangsordningBegrunnelser() =
    this.begrunnelser.any {
        it.nasjonalEllerFellesBegrunnelse == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING ||
            it.nasjonalEllerFellesBegrunnelse == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING
    }

private fun NasjonalEllerFellesBegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    vedtaksperiode: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelse: SanityBegrunnelse,
): List<EndretUtbetalingAndel> =
    when (this.begrunnelseType) {
        BegrunnelseType.ETTER_ENDRET_UTBETALING -> {
            endretUtbetalingAndeler.filter {
                it.periode.tom
                    .sisteDagIInneværendeMåned()
                    ?.erDagenFør(vedtaksperiode.fom?.førsteDagIInneværendeMåned()) == true &&
                    sanityBegrunnelse.endringsårsaker.contains(it.årsak)
            }
        }

        BegrunnelseType.AVSLAG -> {
            endretUtbetalingAndeler.filter {
                it.periode.fom == vedtaksperiode.fom?.toYearMonth()
            }
        }

        else -> {
            emptyList()
        }
    }
