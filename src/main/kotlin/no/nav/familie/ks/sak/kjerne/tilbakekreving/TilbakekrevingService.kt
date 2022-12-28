package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.ks.sak.api.dto.ForhåndsvisTilbakekrevingVarselbrevDto
import no.nav.familie.ks.sak.api.dto.TilbakekrevingRequestDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentTilbakekrevingsperioderISimulering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingService(
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val vedtakRepository: VedtakRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val simuleringService: SimuleringService
) {

    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean =
        tilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(fagsakId)

    @Transactional
    fun lagreTilbakekreving(tilbakekrevingRequestDto: TilbakekrevingRequestDto, behandling: Behandling): Tilbakekreving? {
        val eksisterendeTilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)
        val tilbakekreving = Tilbakekreving(
            begrunnelse = tilbakekrevingRequestDto.begrunnelse,
            behandling = behandling,
            valg = tilbakekrevingRequestDto.valg,
            varsel = tilbakekrevingRequestDto.varsel,
            tilbakekrevingsbehandlingId = eksisterendeTilbakekreving?.tilbakekrevingsbehandlingId
        )

        eksisterendeTilbakekreving?.let { tilbakekrevingRepository.deleteById(it.id) }
        return tilbakekrevingRepository.save(tilbakekreving)
    }

    fun hentForhåndsvisningTilbakekrevingVarselBrev(
        behandlingId: Long,
        forhåndsvisTilbakekrevingVarselbrevDto: ForhåndsvisTilbakekrevingVarselbrevDto
    ): ByteArray {
        val vedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandlingId) ?: throw Feil(
            "Fant ikke vedtak for behandling $behandlingId ved forhåndsvisning av varselbrev for tilbakekreving."
        )
        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val søker = personopplysningGrunnlag.søker
        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        return tilbakekrevingKlient.hentForhåndsvisningTilbakekrevingVarselbrev(
            forhåndsvisVarselbrevRequest = ForhåndsvisVarselbrevRequest(
                varseltekst = forhåndsvisTilbakekrevingVarselbrevDto.fritekst,
                ytelsestype = Ytelsestype.KONTANTSTØTTE,
                behandlendeEnhetId = arbeidsfordeling.behandlendeEnhetId,
                behandlendeEnhetsNavn = arbeidsfordeling.behandlendeEnhetNavn,
                språkkode = søker.målform.tilSpråkkode(),
                feilutbetaltePerioderDto = FeilutbetaltePerioderDto(
                    sumFeilutbetaling = simuleringService.hentFeilutbetaling(behandlingId).longValueExact(),
                    perioder = hentTilbakekrevingsperioderISimulering(simuleringService.hentSimuleringPåBehandling(behandlingId))
                ),
                fagsystem = Fagsystem.KONT, // for ks vil fagsystem alltid være KONT,
                eksternFagsakId = vedtak.behandling.fagsak.id.toString(),
                ident = søker.aktør.aktivFødselsnummer(),
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerNavn(),
                verge = null, // TODO kommer når verge er implementert
                institusjon = null // Institusjon er alltid null for kontantstøtte
            )
        )
    }
}
