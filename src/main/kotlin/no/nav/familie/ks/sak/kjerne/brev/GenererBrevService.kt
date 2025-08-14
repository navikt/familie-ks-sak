package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.tilBrev
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.formaterBeløp
import no.nav.familie.ks.sak.common.util.storForbokstavIAlleNavn
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.common.util.tilMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.opphørsperiode.Opphørsperiode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.opphørsperiode.OpphørsperiodeGenerator
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.totalKalkulertUtbetalingsbeløpForPeriode
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Dødsfall
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.DødsfallData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.EndringAvFramtidigOpphør
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.EndringAvFramtidigOpphørData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FeilutbetaltValuta
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.UtbetalingOvergangsordning
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.Avslag
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.FortsattInnvilget
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.Førstegangsvedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.OpphørMedEndring
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.Opphørt
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.VedtakEndring
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.VedtakOvergangsordning
import no.nav.familie.ks.sak.kjerne.brev.hjemler.HjemmeltekstUtleder
import no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak.SammensattKontrollsakBrevDtoUtleder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ks.sak.sikkerhet.SaksbehandlerContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class GenererBrevService(
    private val brevKlient: BrevKlient,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val brevPeriodeService: BrevPeriodeService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val feilutbetaltValutaService: FeilutbetaltValutaService,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val sammensattKontrollsakService: SammensattKontrollsakService,
    private val etterbetalingService: EtterbetalingService,
    private val simuleringService: SimuleringService,
    private val sammensattKontrollsakBrevDtoUtleder: SammensattKontrollsakBrevDtoUtleder,
    private val søkersMeldepliktService: SøkersMeldepliktService,
    private val opprettGrunnlagOgSignaturDataService: OpprettGrunnlagOgSignaturDataService,
    private val brevmalService: BrevmalService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val hjemmeltekstUtleder: HjemmeltekstUtleder,
    private val opphørsperiodeGenerator: OpphørsperiodeGenerator,
) {
    fun genererManueltBrev(
        manueltBrevRequest: ManueltBrevDto,
        erForhåndsvisning: Boolean = false,
    ): ByteArray {
        try {
            val brev = manueltBrevRequest.tilBrev(saksbehandlerContext.hentSaksbehandlerSignaturTilBrev())
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev,
            )
        } catch (it: Exception) {
            if (it is Feil || it is FunksjonellFeil) {
                throw it
            } else {
                throw Feil(
                    message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal}. ${it.message}",
                    frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    throwable = it,
                )
            }
        }
    }

    fun genererBrevForBehandling(vedtak: Vedtak): ByteArray {
        try {
            if (!vedtak.behandling.skalBehandlesAutomatisk() && vedtak.behandling.steg > BehandlingSteg.BESLUTTE_VEDTAK) {
                throw FunksjonellFeil(
                    melding = "Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter",
                )
            }

            val sammensattKontrollsak =
                sammensattKontrollsakService.finnSammensattKontrollsakForBehandling(
                    behandlingId = vedtak.behandling.id,
                )

            val målform =
                personopplysningGrunnlagService.hentSøkersMålform(
                    behandlingId = vedtak.behandling.id,
                )

            val vedtaksbrev =
                when {
                    sammensattKontrollsak != null -> sammensattKontrollsakBrevDtoUtleder.utled(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak)
                    vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL -> hentDødsfallbrevData(vedtak)
                    vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> TODO() // brevService.hentKorreksjonbrevData(vedtak)
                    vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.LOVENDRING_2024 -> hentEndringAvFramtidigOpphørData(vedtak)
                    else -> hentVedtaksbrevData(vedtak)
                }

            return brevKlient.genererBrev(
                målform = målform.tilSanityFormat(),
                brev = vedtaksbrev,
            )
        } catch (exception: Exception) {
            if (exception is FunksjonellFeil) {
                throw exception
            }
            throw Feil(
                message = "Klarte ikke generere vedtaksbrev på behandling ${vedtak.behandling}: ${exception.message}",
                frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = exception,
            )
        }
    }

    fun hentVedtaksbrevData(vedtak: Vedtak): VedtaksbrevDto {
        val behandling = vedtak.behandling
        val vedtaksbrevmal = brevmalService.hentVedtaksbrevmal(behandling)
        val fellesdataForVedtaksbrev = lagDataForVedtaksbrev(vedtak)
        val etterbetaling = etterbetalingService.hentEtterbetaling(vedtak)
        val søkerHarMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)
        val andeler by lazy { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id) }

        return when (vedtaksbrevmal) {
            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK -> {
                Førstegangsvedtak(
                    fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                    etterbetaling = etterbetaling,
                    refusjonEosAvklart = brevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = brevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    duMaaMeldeFraOmEndringerEosSelvstendigRett = søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(vedtak),
                    duMaaMeldeFraOmEndringer = søkerHarMeldtFraOmBarnehagePlass,
                    duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass = !søkerHarMeldtFraOmBarnehagePlass,
                )
            }

            Brevmal.VEDTAK_AVSLAG -> Avslag(fellesdataForVedtaksbrev = fellesdataForVedtaksbrev)

            Brevmal.VEDTAK_ENDRING ->
                VedtakEndring(
                    fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                    etterbetaling = etterbetaling,
                    erKlage = behandling.erKlage(),
                    erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                    feilutbetaltValuta =
                        feilutbetaltValutaService
                            .beskrivPerioderMedFeilutbetaltValuta(behandling.id)
                            ?.let {
                                FeilutbetaltValuta(perioderMedForMyeUtbetalt = it)
                            },
                    refusjonEosAvklart = brevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = brevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    duMaaMeldeFraOmEndringerEosSelvstendigRett = søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(vedtak),
                    duMaaMeldeFraOmEndringer = søkerHarMeldtFraOmBarnehagePlass,
                    duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass = !søkerHarMeldtFraOmBarnehagePlass,
                )

            Brevmal.VEDTAK_OVERGANGSORDNING -> {
                val sumAvOvergangsordningsAndeler = andeler.filter { it.type == YtelseType.OVERGANGSORDNING }.sumOf { it.totalKalkulertUtbetalingsbeløpForPeriode() }

                VedtakOvergangsordning(
                    mal = Brevmal.VEDTAK_OVERGANGSORDNING,
                    fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                    utbetalingOvergangsordning = UtbetalingOvergangsordning(utbetalingsbelop = formaterBeløp(sumAvOvergangsordningsAndeler)),
                )
            }

            Brevmal.VEDTAK_OPPHØRT ->
                Opphørt(
                    fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                    erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                )

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING ->
                OpphørMedEndring(
                    fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                    etterbetaling = etterbetaling,
                    erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    refusjonEosAvklart = brevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = brevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    erKlage = behandling.erKlage(),
                )

            Brevmal.VEDTAK_FORTSATT_INNVILGET ->
                FortsattInnvilget(
                    fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                    etterbetaling = etterbetaling,
                    informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                    refusjonEosAvklart = brevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = brevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    duMåMeldeFraOmEndringerEøsSelvstendigRett = søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(vedtak),
                )

            else -> throw Feil("Forsøker å hente vedtaksbrevdata for brevmal ${vedtaksbrevmal.visningsTekst}")
        }
    }

    fun lagDataForVedtaksbrev(vedtak: Vedtak): FellesdataForVedtaksbrev {
        val utvidetVedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(vedtak)

        val erBehandlingOpprettetForLovendring2024 = vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.LOVENDRING_2024
        if (erBehandlingOpprettetForLovendring2024) {
            begrunnPerioderLovendring2024(
                utvidetVedtaksperioderMedBegrunnelser = utvidetVedtaksperioderMedBegrunnelser,
            )
        }

        val oppdatertUtvidetVedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(vedtak).filter {
                !(it.begrunnelser.isEmpty() && it.fritekster.isEmpty() && it.eøsBegrunnelser.isEmpty())
            }

        if (oppdatertUtvidetVedtaksperioderMedBegrunnelser.isEmpty()) {
            throw FunksjonellFeil(
                "Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet.",
            )
        }

        val personopplysningsgrunnlagOgSignaturData = opprettGrunnlagOgSignaturDataService.opprett(vedtak)

        val brevPeriodeDtoer =
            brevPeriodeService
                .hentBrevPeriodeDtoer(oppdatertUtvidetVedtaksperioderMedBegrunnelser, vedtak.behandling.id)

        val korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(vedtak.behandling.id)

        val hjemmeltekst =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = vedtak.behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = korrigertVedtak != null,
                utvidetVedtaksperioderMedBegrunnelser = oppdatertUtvidetVedtaksperioderMedBegrunnelser,
            )

        return FellesdataForVedtaksbrev(
            enhet = personopplysningsgrunnlagOgSignaturData.enhet,
            saksbehandler = personopplysningsgrunnlagOgSignaturData.saksbehandler,
            beslutter = personopplysningsgrunnlagOgSignaturData.beslutter,
            hjemmeltekst = Hjemmeltekst(hjemmeltekst),
            søkerNavn = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer =
                personopplysningsgrunnlagOgSignaturData.grunnlag.søker.aktør
                    .aktivFødselsnummer(),
            perioder = brevPeriodeDtoer,
            gjelder = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.navn,
            korrigertVedtakData = korrigertVedtak?.let { KorrigertVedtakData(datoKorrigertVedtak = it.vedtaksdato.tilDagMånedÅr()) },
        )
    }

    private fun begrunnPerioderLovendring2024(
        utvidetVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
    ) {
        val utvidetVedtaksperiodeMedBegrunnelserAvTypeUtbetaling =
            utvidetVedtaksperioderMedBegrunnelser.singleOrNull {
                it.type == Vedtaksperiodetype.UTBETALING
            } ?: throw Feil(
                "Forventet én vedtaksperiode med begrunnelse av type ${Vedtaksperiodetype.UTBETALING}",
            )
        val utvidetVedtaksperiodeMedBegrunnelserAvTypeOpphør =
            utvidetVedtaksperioderMedBegrunnelser.singleOrNull {
                it.type == Vedtaksperiodetype.OPPHØR
            } ?: throw Feil(
                "Forventet én vedtaksperiode med begrunnelse av type ${Vedtaksperiodetype.OPPHØR}",
            )
        vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
            vedtaksperiodeId = utvidetVedtaksperiodeMedBegrunnelserAvTypeUtbetaling.id,
            begrunnelserFraFrontend =
                listOf(
                    NasjonalEllerFellesBegrunnelse.INNVILGET_PÅ_GRUNN_AV_LOVENDRING_2024,
                ),
            eøsBegrunnelserFraFrontend = listOf(),
        )
        vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
            vedtaksperiodeId = utvidetVedtaksperiodeMedBegrunnelserAvTypeOpphør.id,
            begrunnelserFraFrontend =
                listOf(
                    NasjonalEllerFellesBegrunnelse.OPPHØR_NYTT_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS,
                ),
            eøsBegrunnelserFraFrontend = listOf(),
        )
    }

    fun hentEndringAvFramtidigOpphørData(vedtak: Vedtak): BrevDto {
        val fellesdataForVedtaksbrev = lagDataForVedtaksbrev(vedtak)
        opprettGrunnlagOgSignaturDataService.opprett(vedtak).let { data ->
            return EndringAvFramtidigOpphør(
                data =
                    EndringAvFramtidigOpphørData(
                        delmalData =
                            EndringAvFramtidigOpphørData.DelmalData(
                                signaturVedtak =
                                    SignaturVedtak(
                                        enhet = data.enhet,
                                        saksbehandler = data.saksbehandler,
                                        beslutter = data.beslutter,
                                    ),
                            ),
                        flettefelter =
                            EndringAvFramtidigOpphørData.Flettefelter(
                                navn = data.grunnlag.søker.navn,
                                fodselsnummer =
                                    data.grunnlag.søker.aktør
                                        .aktivFødselsnummer(),
                            ),
                        perioder = fellesdataForVedtaksbrev.perioder,
                    ),
            )
        }
    }

    fun hentDødsfallbrevData(
        vedtak: Vedtak,
    ) = opprettGrunnlagOgSignaturDataService.opprett(vedtak).let { data ->
        Dødsfall(
            data =
                DødsfallData(
                    delmalData =
                        DødsfallData.DelmalData(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = data.enhet,
                                    saksbehandler = data.saksbehandler,
                                    beslutter = data.beslutter,
                                ),
                        ),
                    flettefelter =
                        DødsfallData.Flettefelter(
                            navn = data.grunnlag.søker.navn,
                            fodselsnummer =
                                data.grunnlag.søker.aktør
                                    .aktivFødselsnummer(),
                            // Selv om det er feil å anta at alle navn er på dette formatet er det ønskelig å skrive
                            // det slik, da uppercase kan oppleves som skrikende i et brev som skal være skånsomt
                            navnAvdode =
                                data.grunnlag.søker.navn
                                    .storForbokstavIAlleNavn(),
                            virkningstidspunkt =
                                hentVirkningstidspunkt(
                                    opphørsperioder = opphørsperiodeGenerator.genererOpphørsperioder(vedtak.behandling),
                                    behandlingId = vedtak.behandling.id,
                                ),
                        ),
                ),
        )
    }

    private fun hentVirkningstidspunkt(
        opphørsperioder: List<Opphørsperiode>,
        behandlingId: Long,
    ) = (
        opphørsperioder
            .maxOfOrNull { it.periodeFom }
            ?.tilMånedÅr()
            ?: throw Feil("Fant ikke opphørdato ved generering av dødsfallbrev på behandling $behandlingId")
    )
}
