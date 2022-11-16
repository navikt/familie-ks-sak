package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.util.erSenereEnnInneværendeMåned
import no.nav.familie.ks.sak.common.util.formaterBeløp
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.BegrunnelseDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevBehandlingsGrunnlag
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevtUregistrertBarn
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeType
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBarnasFødselsdatoer
import no.nav.familie.ks.sak.kjerne.brev.domene.totaltUtbetalt
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
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

    constructor(grunnlagForBrevperiode: GrunnlagForBrevperiode) : this(
        brevBehandlingsGrunnlag = grunnlagForBrevperiode.brevBehandlingsGrunnlag,
        erFørsteVedtaksperiodePåFagsak = grunnlagForBrevperiode.erFørsteVedtaksperiodePåFagsak,
        uregistrerteBarn = grunnlagForBrevperiode.uregistrerteBarn,
        brevMålform = grunnlagForBrevperiode.brevMålform,
        brevVedtaksPeriode = grunnlagForBrevperiode.brevVedtaksPeriode,
        barnMedReduksjonFraForrigeBehandlingIdent = grunnlagForBrevperiode.barnMedReduksjonFraForrigeBehandlingIdent,
        dødeBarnForrigePeriode = grunnlagForBrevperiode.dødeBarnForrigePeriode
    )

    fun genererBrevPeriode(): BrevPeriodeDto? {
        // TODO: Hente relevante begrunnelser med riktige personer
        // oppgave her: https://favro.com/organization/98c34fb974ce445eac854de0/077068028bffba85055cca2d?card=Tea-10394
        val begrunnelserOgFritekster = emptyList<BegrunnelseDto>()

        if (begrunnelserOgFritekster.isEmpty()) return null

        val tomDato =
            if (brevVedtaksPeriode.tom?.erSenereEnnInneværendeMåned() == false) {
                brevVedtaksPeriode.tom.tilDagMånedÅr()
            } else {
                null
            }

        val identerIBegrunnelene = emptyList<String>()

        return byggBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster,
            identerIBegrunnelene = identerIBegrunnelene
        )
    }

    private fun byggBrevPeriode(
        tomDato: String?,
        begrunnelserOgFritekster: List<BegrunnelseDto>,
        identerIBegrunnelene: List<String>
    ): BrevPeriodeDto {
        val (utbetalingerBarn, nullutbetalingerBarn) = brevVedtaksPeriode
            .brevUtbetalingsperiodeDetaljer
            .filter { it.person.type == PersonType.BARN }
            .partition { it.utbetaltPerMnd != 0 }

        val barnMedUtbetaling = utbetalingerBarn.map { it.person }
        val barnMedNullutbetaling = nullutbetalingerBarn.map { it.person }

        val barnIPeriode: List<BrevPerson> = when (brevVedtaksPeriode.type) {
            Vedtaksperiodetype.UTBETALING -> finnBarnIUtbetalingPeriode(identerIBegrunnelene)

            Vedtaksperiodetype.OPPHØR -> emptyList()
            Vedtaksperiodetype.AVSLAG -> emptyList()
            Vedtaksperiodetype.FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
        }

        val utbetalingsbeløp = brevVedtaksPeriode.brevUtbetalingsperiodeDetaljer.totaltUtbetalt()
        val brevPeriodeType = hentPeriodetype(brevVedtaksPeriode.fom, barnMedUtbetaling, utbetalingsbeløp)
        return BrevPeriodeDto(

            fom = this.hentFomTekst(),
            tom = when {
                brevVedtaksPeriode.type == Vedtaksperiodetype.FORTSATT_INNVILGET -> ""
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

    private fun hentFomTekst(): String = when (brevVedtaksPeriode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> "Du får:"
        Vedtaksperiodetype.UTBETALING -> brevVedtaksPeriode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.OPPHØR -> brevVedtaksPeriode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.AVSLAG -> if (brevVedtaksPeriode.fom != null) brevVedtaksPeriode.fom.tilDagMånedÅr() else ""
    }

    private fun hentPeriodetype(
        fom: LocalDate?,
        barnMedUtbetaling: List<BrevPerson>,
        utbetalingsbeløp: Int
    ) =
        when (brevVedtaksPeriode.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET -> BrevPeriodeType.FORTSATT_INNVILGET
            Vedtaksperiodetype.UTBETALING -> when {
                utbetalingsbeløp == 0 -> BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING
                barnMedUtbetaling.isEmpty() -> BrevPeriodeType.INNVILGELSE_KUN_UTBETALING_PÅ_SØKER
                else -> BrevPeriodeType.INNVILGELSE
            }

            Vedtaksperiodetype.AVSLAG -> if (fom != null) BrevPeriodeType.AVSLAG else BrevPeriodeType.AVSLAG_UTEN_PERIODE
            Vedtaksperiodetype.OPPHØR -> BrevPeriodeType.OPPHOR
        }

    fun finnBarnIUtbetalingPeriode(identerIBegrunnelene: List<String>): List<BrevPerson> {
        val identerMedUtbetaling =
            brevVedtaksPeriode.brevUtbetalingsperiodeDetaljer.map { it.person.aktivPersonIdent }

        val barnIPeriode = (identerIBegrunnelene + identerMedUtbetaling)
            .toSet()
            .mapNotNull { personIdent ->
                brevBehandlingsGrunnlag.personerPåBehandling.find { it.aktivPersonIdent == personIdent }
            }
            .filter { it.type == PersonType.BARN }

        return barnIPeriode
    }
}
