package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.eksterne.kontrakter.Kompetanse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
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
    private val minimerteKompetanserForPeriode: List<Kompetanse>,
    private val minimerteKompetanserSomStopperRettFørPeriode: List<Kompetanse>,
    private val dødeBarnForrigePeriode: List<String>
) {

    fun genererBrevPeriode(): BrevPeriode? {
        // kan droppe hentBegrunnelsegrunnlagMedPersoner
        val begrunnelseGrunnlagMedPersoner = hentBegrunnelsegrunnlagMedPersoner()
        val eøsBegrunnelserMedKompetanser = hentEøsBegrunnelserMedKompetanser()

        val begrunnelserOgFritekster =
            byggBegrunnelserOgFritekster(
                begrunnelserGrunnlagMedPersoner = begrunnelseGrunnlagMedPersoner,
                eøsBegrunnelserMedKompetanser = eøsBegrunnelserMedKompetanser
            )

        if (begrunnelserOgFritekster.isEmpty()) return null

        val tomDato =
            if (brevVedtaksPeriode.tom?.erSenereEnnInneværendeMåned() == false) {
                brevVedtaksPeriode.tom.tilDagMånedÅr()
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

    fun hentEØSBegrunnelseData(eøsBegrunnelserMedKompetanser: List<EØSBegrunnelseMedKompetanser>): List<EØSBegrunnelseData> =
        eøsBegrunnelserMedKompetanser.flatMap { begrunnelseMedData ->
            val begrunnelse = begrunnelseMedData.begrunnelse

            begrunnelseMedData.kompetanser.map { kompetanse ->
                EØSBegrunnelseData(
                    vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
                    apiNavn = begrunnelse.sanityApiNavn,
                    annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
                    annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetslandNavn?.navn,
                    barnetsBostedsland = kompetanse.barnetsBostedslandNavn.navn,
                    barnasFodselsdatoer = Utils.slåSammen(kompetanse.personer.map { it.fødselsdato.tilKortString() }),
                    antallBarn = kompetanse.personer.size,
                    maalform = brevMålform.tilSanityFormat(),
                    sokersAktivitet = kompetanse.søkersAktivitet,
                    sokersAktivitetsland = kompetanse.søkersAktivitetsland?.navn
                )
            }
        }

    fun hentEøsBegrunnelserMedKompetanser(): List<EØSBegrunnelseMedKompetanser> =
        brevVedtaksPeriode.eøsBegrunnelser.map { eøsBegrunnelseMedTriggere ->
            val kompetanser = when (eøsBegrunnelseMedTriggere.eøsBegrunnelse.vedtakBegrunnelseType) {
                VedtakBegrunnelseType.EØS_INNVILGET -> hentKompetanserForEØSBegrunnelse(
                    eøsBegrunnelseMedTriggere,
                    minimerteKompetanserForPeriode
                )

                VedtakBegrunnelseType.EØS_OPPHØR -> hentKompetanserForEØSBegrunnelse(
                    eøsBegrunnelseMedTriggere,
                    minimerteKompetanserSomStopperRettFørPeriode
                )

                else -> emptyList()
            }
            EØSBegrunnelseMedKompetanser(
                begrunnelse = eøsBegrunnelseMedTriggere.eøsBegrunnelse,
                kompetanser = kompetanser
            )
        }

    fun hentBegrunnelsegrunnlagMedPersoner() = brevVedtaksPeriode.begrunnelser.flatMap {
        it.tilBrevBegrunnelseGrunnlagMedPersoner(
            periode = NullablePeriode(
                fom = brevVedtaksPeriode.fom,
                tom = brevVedtaksPeriode.tom
            ),
            vedtaksperiodetype = brevVedtaksPeriode.type,
            brevBehandlingsGrunnlag = brevBehandlingsGrunnlag,
            identerMedUtbetalingPåPeriode = brevVedtaksPeriode.minimerteUtbetalingsperiodeDetaljer
                .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
            minimerteUtbetalingsperiodeDetaljer = brevVedtaksPeriode.minimerteUtbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            erUregistrerteBarnPåbehandling = uregistrerteBarn.isNotEmpty(),
            barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
            dødeBarnForrigePeriode = dødeBarnForrigePeriode
        )
    }

    fun byggBegrunnelserOgFritekster(
        begrunnelserGrunnlagMedPersoner: List<BrevBegrunnelseGrunnlagMedPersoner>,
        eøsBegrunnelserMedKompetanser: List<EØSBegrunnelseMedKompetanser>
    ): List<Begrunnelse> {
        val brevBegrunnelser = begrunnelserGrunnlagMedPersoner
            .map {
                it.tilBrevBegrunnelse(
                    vedtaksperiode = NullablePeriode(brevVedtaksPeriode.fom, brevVedtaksPeriode.tom),
                    personerIPersongrunnlag = brevBehandlingsGrunnlag.personerPåBehandling,
                    brevMålform = brevMålform,
                    uregistrerteBarn = uregistrerteBarn,
                    minimerteUtbetalingsperiodeDetaljer = brevVedtaksPeriode.minimerteUtbetalingsperiodeDetaljer,
                    minimerteRestEndredeAndeler = brevBehandlingsGrunnlag.minimerteEndredeUtbetalingAndeler
                )
            }

        val eøsBegrunnelser = hentEØSBegrunnelseData(eøsBegrunnelserMedKompetanser)

        val fritekster = brevVedtaksPeriode.fritekster.map { FritekstBegrunnelse(it) }

        return (brevBegrunnelser + eøsBegrunnelser + fritekster).sorted()
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
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            Vedtaksperiodetype.UTBETALING -> finnBarnIUtbetalingPeriode(identerIBegrunnelene)

            Vedtaksperiodetype.OPPHØR -> emptyList()
            Vedtaksperiodetype.AVSLAG -> emptyList()
            Vedtaksperiodetype.FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
            Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
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
        Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
        Vedtaksperiodetype.OPPHØR -> brevVedtaksPeriode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.AVSLAG -> if (brevVedtaksPeriode.fom != null) brevVedtaksPeriode.fom.tilDagMånedÅr() else ""
        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> brevVedtaksPeriode.fom!!.tilDagMånedÅr()
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
}
