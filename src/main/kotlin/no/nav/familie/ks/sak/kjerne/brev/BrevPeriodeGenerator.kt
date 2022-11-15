package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.erSenereEnnInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.BegrunnelseDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevBehandlingsGrunnlag
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevtUregistrertBarn
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import java.time.LocalDate

class BrevPeriodeGenerator(
    private val brevBehandlingsGrunnlag: BrevBehandlingsGrunnlag,
    private val erFørsteVedtaksperiodePåFagsak: Boolean,
    private val uregistrerteBarn: List<BrevtUregistrertBarn>,
    private val brevMålform: Målform,
    private val brevVedtaksPeriode: BrevVedtaksPeriode,
    private val barnMedReduksjonFraForrigeBehandlingIdent: List<String>,
    private val dødeBarnForrigePeriode: List<String>
) {

    fun genererBrevPeriode(): BrevPeriode? {
        // kan droppe hentBegrunnelsegrunnlagMedPersoner
        val begrunnelseGrunnlagMedPersoner = hentBegrunnelsegrunnlagMedPersoner()

        val begrunnelserOgFritekster =
            byggBegrunnelserOgFritekster(
                begrunnelserGrunnlagMedPersoner = begrunnelseGrunnlagMedPersoner
            )

        if (begrunnelserOgFritekster.isEmpty()) return null

        val tomDato =
            if (brevVedtaksPeriode.tom?.erSenereEnnInneværendeMåned() == false) {
                brevVedtaksPeriode.tom?.tilDagMånedÅr()
            } else {
                null
            }

        val identerIBegrunnelene = begrunnelseGrunnlagMedPersoner
            .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET }
            .flatMap { it.personIdenter }

        return byggBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster,
            identerIBegrunnelene = identerIBegrunnelene
        )
    }

    fun hentBegrunnelsegrunnlagMedPersoner() = brevVedtaksPeriode.begrunnelser.flatMap {
        it.tilBrevBegrunnelseGrunnlagMedPersoner(
            periode = NullablePeriode(
                fom = brevVedtaksPeriode.fom,
                tom = brevVedtaksPeriode.tom
            ),
            vedtaksperiodetype = brevVedtaksPeriode.type,
            restBehandlingsgrunnlagForBrev = brevBehandlingsGrunnlag,
            identerMedUtbetalingPåPeriode = brevVedtaksPeriode.brevUtbetalingsperiodeDetaljer
                .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.aktivPersonIdent },
            minimerteUtbetalingsperiodeDetaljer = brevVedtaksPeriode.brevUtbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            erUregistrerteBarnPåbehandling = uregistrerteBarn.isNotEmpty(),
            barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
            dødeBarnForrigePeriode = dødeBarnForrigePeriode
        )
    }

    fun byggBegrunnelserOgFritekster(
        begrunnelserGrunnlagMedPersoner: List<BrevBegrunnelseGrunnlagMedPersoner>
    ): List<BegrunnelseDto> {
        val brevBegrunnelser = begrunnelserGrunnlagMedPersoner
            .map {
                it.tilBrevBegrunnelse(
                    vedtaksperiode = NullablePeriode(brevVedtaksPeriode.fom, brevVedtaksPeriode.tom),
                    personerIPersongrunnlag = brevBehandlingsGrunnlag.personerPåBehandling,
                    brevMålform = brevMålform,
                    uregistrerteBarn = uregistrerteBarn,
                    minimerteUtbetalingsperiodeDetaljer = brevVedtaksPeriode.brevUtbetalingsperiodeDetaljer,
                    minimerteRestEndredeAndeler = brevBehandlingsGrunnlag.endretUtbetalingAndeler
                )
            }

        // val eøsBegrunnelser = hentEØSBegrunnelseData(eøsBegrunnelserMedKompetanser)

        // val fritekster = brevVedtaksPeriode.fritekster.map { FritekstBegrunnelse(it) }

        // return (brevBegrunnelser + eøsBegrunnelser + fritekster).sorted()
        return (brevBegrunnelser)
    }

    private fun byggBrevPeriode(
        tomDato: String?,
        begrunnelserOgFritekster: List<Begrunnelse>,
        identerIBegrunnelene: List<String>
    ): BrevPeriode {
        val (utbetalingerBarn, nullutbetalingerBarn) = brevVedtaksPeriode.minimerteUtbetalingsperiodeDetaljer
            .filter { it.person.type == PersonType.BARN }
            .partition { it.utbetaltPerMnd != 0 }

        val barnMedUtbetaling = utbetalingerBarn.map { it.person }
        val barnMedNullutbetaling = nullutbetalingerBarn.map { it.person }

        val barnIPeriode: List<MinimertRestPerson> = when (brevVedtaksPeriode.type) {
            Vedtaksperiodetype.UTBETALING -> finnBarnIUtbetalingPeriode(identerIBegrunnelene)

            Vedtaksperiodetype.OPPHØR -> emptyList()
            Vedtaksperiodetype.AVSLAG -> emptyList()
            Vedtaksperiodetype.FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
        }

        val utbetalingsbeløp = brevVedtaksPeriode.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()
        val brevPeriodeType = hentPeriodetype(brevVedtaksPeriode.fom, barnMedUtbetaling, utbetalingsbeløp)
        return BrevPeriode(

            fom = this.hentFomTekst(),
            tom = when {
                brevVedtaksPeriode.type == Vedtaksperiodetype.FORTSATT_INNVILGET -> ""
                tomDato.isNullOrBlank() -> ""
                brevPeriodeType == BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING -> " til $tomDato"
                else -> "til $tomDato "
            },
            belop = Utils.formaterBeløp(utbetalingsbeløp),
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

    private fun hentFomTekst(): String = when (brevVedtaksPeriode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> hentFomtekstFortsattInnvilget(
            brevMålform,
            brevVedtaksPeriode.fom,
            brevVedtaksPeriode.begrunnelser.map { it.standardbegrunnelse }
        ) ?: "Du får:"

        Vedtaksperiodetype.UTBETALING -> brevVedtaksPeriode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.OPPHØR -> brevVedtaksPeriode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.AVSLAG -> if (brevVedtaksPeriode.fom != null) brevVedtaksPeriode.fom.tilDagMånedÅr() else ""
    }

    private fun hentPeriodetype(
        fom: LocalDate?,
        barnMedUtbetaling: List<MinimertRestPerson>,
        utbetalingsbeløp: Int
    ) = if (brevBehandlingsGrunnlag.erInstitusjon) {
        when (brevVedtaksPeriode.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET -> BrevPeriodeType.FORTSATT_INNVILGET_INSTITUSJON
            Vedtaksperiodetype.UTBETALING -> when (utbetalingsbeløp) {
                0 -> BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING
                else -> BrevPeriodeType.INNVILGELSE_INSTITUSJON
            }

            Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
            Vedtaksperiodetype.AVSLAG -> if (fom != null) BrevPeriodeType.AVSLAG_INSTITUSJON else BrevPeriodeType.AVSLAG_UTEN_PERIODE_INSTITUSJON
            Vedtaksperiodetype.OPPHØR -> BrevPeriodeType.OPPHOR_INSTITUSJON
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> BrevPeriodeType.INNVILGELSE_INSTITUSJON
        }
    } else {
        when (brevVedtaksPeriode.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET -> BrevPeriodeType.FORTSATT_INNVILGET
            Vedtaksperiodetype.UTBETALING -> when {
                utbetalingsbeløp == 0 -> BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING
                barnMedUtbetaling.isEmpty() -> BrevPeriodeType.INNVILGELSE_KUN_UTBETALING_PÅ_SØKER
                else -> BrevPeriodeType.INNVILGELSE
            }

            Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
            Vedtaksperiodetype.AVSLAG -> if (fom != null) BrevPeriodeType.AVSLAG else BrevPeriodeType.AVSLAG_UTEN_PERIODE
            Vedtaksperiodetype.OPPHØR -> BrevPeriodeType.OPPHOR
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> BrevPeriodeType.INNVILGELSE
        }
    }

    fun finnBarnIUtbetalingPeriode(identerIBegrunnelene: List<String>): List<MinimertRestPerson> {
        val identerMedUtbetaling =
            brevVedtaksPeriode.minimerteUtbetalingsperiodeDetaljer.map { it.person.personIdent }

        val barnIPeriode = (identerIBegrunnelene + identerMedUtbetaling)
            .toSet()
            .mapNotNull { personIdent ->
                brevBehandlingsGrunnlag.personerPåBehandling.find { it.personIdent == personIdent }
            }
            .filter { it.type == PersonType.BARN }

        return barnIPeriode
    }

    private fun hentFomtekstFortsattInnvilget(
        målform: Målform,
        fom: LocalDate?,
        begrunnelser: List<Standardbegrunnelse>
    ): String? {
        val erAutobrev = begrunnelser.any {
            it == Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK ||
                it == Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK
        }
        return if (erAutobrev && fom != null) {
            val fra = if (målform == Målform.NB) "Fra" else "Frå"
            "$fra ${fom.tilDagMånedÅr()} får du:"
        } else {
            null
        }
    }

    fun BrevBegrunnelseGrunnlagMedPersoner.tilBrevBegrunnelse(
        vedtaksperiode: NullablePeriode,
        personerIPersongrunnlag: List<MinimertRestPerson>,
        brevMålform: Målform,
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
        minimerteRestEndredeAndeler: List<MinimertRestEndretAndel>
    ): Begrunnelse {
        val personerPåBegrunnelse =
            personerIPersongrunnlag.filter { person -> this.personIdenter.contains(person.personIdent) }

        val barnSomOppfyllerTriggereOgHarUtbetaling = personerPåBegrunnelse.filter { person ->
            person.type == PersonType.BARN && minimerteUtbetalingsperiodeDetaljer.any { it.utbetaltPerMnd > 0 && it.person.personIdent == person.personIdent }
        }
        val barnSomOppfyllerTriggereOgHarNullutbetaling = personerPåBegrunnelse.filter { person ->
            person.type == PersonType.BARN && minimerteUtbetalingsperiodeDetaljer.any { it.utbetaltPerMnd == 0 && it.person.personIdent == person.personIdent }
        }

        val gjelderSøker = personerPåBegrunnelse.any { it.type == PersonType.SØKER }

        val barnasFødselsdatoer = this.hentBarnasFødselsdagerForBegrunnelse(
            uregistrerteBarn = uregistrerteBarn,
            personerIBehandling = personerIPersongrunnlag,
            personerPåBegrunnelse = personerPåBegrunnelse,
            personerMedUtbetaling = minimerteUtbetalingsperiodeDetaljer.map { it.person },
            gjelderSøker = gjelderSøker
        )

        val antallBarn = this.hentAntallBarnForBegrunnelse(
            uregistrerteBarn = uregistrerteBarn,
            gjelderSøker = gjelderSøker,
            barnasFødselsdatoer = barnasFødselsdatoer
        )

        val månedOgÅrBegrunnelsenGjelderFor =
            if (vedtaksperiode.fom == null) {
                null
            } else {
                this.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                    periode = Periode(
                        fom = vedtaksperiode.fom,
                        tom = vedtaksperiode.tom ?: TIDENES_ENDE
                    )
                )
            }

        val beløp = this.hentBeløp(gjelderSøker, minimerteUtbetalingsperiodeDetaljer)

        val endringsperioder = this.standardbegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
            minimerteRestEndredeAndeler = minimerteRestEndredeAndeler,
            vedtaksperiode = vedtaksperiode
        )

        val søknadstidspunkt = endringsperioder.sortedBy { it.søknadstidspunkt }
            .firstOrNull { this.triggesAv.endringsaarsaker.contains(it.årsak) }?.søknadstidspunkt

        val søkersRettTilUtvidet =
            finnUtOmSøkerFårUtbetaltEllerHarRettPåUtvidet(minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer)

        this.validerBrevbegrunnelse(
            gjelderSøker = gjelderSøker,
            barnasFødselsdatoer = barnasFødselsdatoer
        )

        return BegrunnelseData(
            gjelderSoker = gjelderSøker,
            barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
            fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling = barnSomOppfyllerTriggereOgHarUtbetaling.map { it.fødselsdato }
                .tilBrevTekst(),
            fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling = barnSomOppfyllerTriggereOgHarNullutbetaling.map { it.fødselsdato }
                .tilBrevTekst(),
            antallBarn = antallBarn,
            antallBarnOppfyllerTriggereOgHarUtbetaling = barnSomOppfyllerTriggereOgHarUtbetaling.size,
            antallBarnOppfyllerTriggereOgHarNullutbetaling = barnSomOppfyllerTriggereOgHarNullutbetaling.size,
            maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
            maalform = brevMålform.tilSanityFormat(),
            apiNavn = this.standardbegrunnelse.sanityApiNavn,
            belop = Utils.formaterBeløp(beløp),
            soknadstidspunkt = søknadstidspunkt?.tilKortString() ?: "",
            avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted?.tilKortString() ?: "",
            sokersRettTilUtvidet = søkersRettTilUtvidet.tilSanityFormat(),
            vedtakBegrunnelseType = this.vedtakBegrunnelseType
        )
    }
}
