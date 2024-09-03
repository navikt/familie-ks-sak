package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FeilutbetaltValuta
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VedtakEndringSammensattKontrollsakDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpprettVedtakEndringSammensattKontrollsakDtoService(
    private val søkersMeldepliktService: SøkersMeldepliktService,
    private val opprettVedtakFellesfelterSammensattKontrollsakDtoService: OpprettVedtakFellesfelterSammensattKontrollsakDtoService,
    private val etterbetalingService: EtterbetalingService,
    private val simuleringService: SimuleringService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val feilutbetaltValutaService: FeilutbetaltValutaService,
    private val brevPeriodeService: BrevPeriodeService,
) {
    private val logger = LoggerFactory.getLogger(OpprettVedtakEndringSammensattKontrollsakDtoService::class.java)

    fun opprett(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): VedtakEndringSammensattKontrollsakDto {
        logger.debug("Oppretter ${VedtakEndringSammensattKontrollsakDto::class.simpleName} for vedtak ${vedtak.id}")

        val søkerHarMeldtFraOmBarnehagePlass =
            søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                vedtak = vedtak,
            )

        val feilutbetaltValuta =
            feilutbetaltValutaService
                .beskrivPerioderMedFeilutbetaltValuta(
                    behandlingId = vedtak.behandling.id,
                )?.let {
                    FeilutbetaltValuta(perioderMedForMyeUtbetalt = it)
                }

        return VedtakEndringSammensattKontrollsakDto(
            vedtakFellesfelter = opprettVedtakFellesfelterSammensattKontrollsakDtoService.opprett(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak),
            etterbetaling = etterbetalingService.hentEtterbetaling(vedtak = vedtak),
            erKlage = vedtak.behandling.erKlage(),
            erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = vedtak.behandling.id),
            informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak = vedtak),
            feilutbetaltValuta = feilutbetaltValuta,
            refusjonEosAvklart = brevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(vedtak = vedtak),
            refusjonEosUavklart = brevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(vedtak = vedtak),
            duMåMeldeFraOmEndringer = søkerHarMeldtFraOmBarnehagePlass,
            duMåMeldeFraOmEndringerEøsSelvstendigRett = søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(vedtak = vedtak),
            duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass = !søkerHarMeldtFraOmBarnehagePlass,
        )
    }
}
