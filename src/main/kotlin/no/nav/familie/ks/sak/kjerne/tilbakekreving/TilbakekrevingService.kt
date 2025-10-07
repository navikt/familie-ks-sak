package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.ManuellAdresseInfo
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.ks.sak.api.dto.ForhåndsvisTilbakekrevingVarselbrevDto
import no.nav.familie.ks.sak.api.dto.TilbakekrevingRequestDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentTilbakekrevingsperioderISimulering
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.opprettVarsel
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType.FULLMEKTIG
import no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType.VERGE
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingService(
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val vedtakRepository: VedtakRepository,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val simuleringService: SimuleringService,
    private val brevmottakerRepository: BrevmottakerRepository,
) {
    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean = tilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(fagsakId)

    fun finnTilbakekrevingsbehandling(behandlingId: Long): Tilbakekreving? = tilbakekrevingRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun lagreTilbakekreving(
        tilbakekrevingRequestDto: TilbakekrevingRequestDto,
        behandling: Behandling,
    ): Tilbakekreving? {
        val eksisterendeTilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)
        val tilbakekreving =
            Tilbakekreving(
                begrunnelse = tilbakekrevingRequestDto.begrunnelse,
                behandling = behandling,
                valg = tilbakekrevingRequestDto.valg,
                varsel = tilbakekrevingRequestDto.varsel,
                tilbakekrevingsbehandlingId = eksisterendeTilbakekreving?.tilbakekrevingsbehandlingId,
            )

        eksisterendeTilbakekreving?.let { tilbakekrevingRepository.deleteById(it.id) }
        return tilbakekrevingRepository.save(tilbakekreving)
    }

    @Transactional
    fun oppdaterTilbakekreving(
        tilbakekrevingsbehandlingId: String,
        behandlingId: Long,
    ) {
        val tilbakekreving =
            tilbakekrevingRepository.findByBehandlingId(behandlingId)
                ?: throw Feil("Fant ikke tilbakekreving for behandling $behandlingId")

        tilbakekreving.tilbakekrevingsbehandlingId = tilbakekrevingsbehandlingId
        tilbakekrevingRepository.save(tilbakekreving)
    }

    fun hentForhåndsvisningTilbakekrevingVarselBrev(
        behandlingId: Long,
        forhåndsvisTilbakekrevingVarselbrevDto: ForhåndsvisTilbakekrevingVarselbrevDto,
    ): ByteArray {
        val vedtak =
            vedtakRepository.findByBehandlingAndAktivOptional(behandlingId) ?: throw Feil(
                "Fant ikke vedtak for behandling $behandlingId ved forhåndsvisning av varselbrev for tilbakekreving.",
            )
        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val søker = personopplysningGrunnlag.søker
        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        return tilbakekrevingKlient.hentForhåndsvisningTilbakekrevingVarselbrev(
            forhåndsvisVarselbrevRequest =
                ForhåndsvisVarselbrevRequest(
                    varseltekst = forhåndsvisTilbakekrevingVarselbrevDto.fritekst,
                    ytelsestype = Ytelsestype.KONTANTSTØTTE,
                    behandlendeEnhetId = arbeidsfordeling.behandlendeEnhetId,
                    behandlendeEnhetsNavn = arbeidsfordeling.behandlendeEnhetNavn,
                    språkkode = søker.målform.tilSpråkkode(),
                    feilutbetaltePerioderDto =
                        FeilutbetaltePerioderDto(
                            sumFeilutbetaling = simuleringService.hentFeilutbetaling(behandlingId).longValueExact(),
                            perioder = hentTilbakekrevingsperioderISimulering(simuleringService.hentSimuleringPåBehandling(behandlingId)),
                        ),
                    // for ks vil fagsystem alltid være KONT,
                    fagsystem = Fagsystem.KONT,
                    eksternFagsakId =
                        vedtak.behandling.fagsak.id
                            .toString(),
                    ident = søker.aktør.aktivFødselsnummer(),
                    saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerNavn(),
                    // TODO kommer når verge er implementert
                    verge = null,
                    // Institusjon er alltid null for kontantstøtte
                    institusjon = null,
                ),
        )
    }

    fun sendOpprettTilbakekrevingRequest(behandling: Behandling) = tilbakekrevingKlient.opprettTilbakekrevingBehandling(lagOpprettTilbakekrevingRequest(behandling))

    fun opprettTilbakekrevingsbehandlingManuelt(fagsakId: Long) {
        val kanOpprettesRespons = tilbakekrevingKlient.kanTilbakekrevingsbehandlingOpprettesManuelt(fagsakId)
        if (!kanOpprettesRespons.kanBehandlingOpprettes) {
            throw FunksjonellFeil(
                frontendFeilmelding = kanOpprettesRespons.melding,
                melding = "Tilbakekrevingsbehandling manuelt kan ikke opprettes pga ${kanOpprettesRespons.melding}",
            )
        }
        val behandlingId =
            kanOpprettesRespons.kravgrunnlagsreferanse?.toLong()
                ?: throw Feil("Tilbakekrevingsbehandling kan opprettes, men har ikke kravgrunnlagsreferanse på respons-en")
        val behandling =
            vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)?.behandling
                ?: throw FunksjonellFeil(
                    frontendFeilmelding =
                        "Av tekniske årsaker så kan ikke tilbakekrevingsbehandling opprettes. " +
                            "Kontakt brukerstøtte for å rapportere feilen",
                    melding =
                        "Tilbakekrevingsbehandling kan ikke opprettes. " +
                            "Respons inneholder enten en referanse til en ukjent behandling eller behandling $behandlingId er ikke vedtatt",
                )

        tilbakekrevingKlient.opprettTilbakekrevingsbehandlingManuelt(
            OpprettManueltTilbakekrevingRequest(
                eksternFagsakId = fagsakId.toString(),
                eksternId = behandling.id.toString(),
                ytelsestype = Ytelsestype.KONTANTSTØTTE,
            ),
        )
    }

    private fun lagOpprettTilbakekrevingRequest(behandling: Behandling): OpprettTilbakekrevingRequest {
        val behandlingId = behandling.id
        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val søker = personopplysningGrunnlag.søker
        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        val aktivtVedtak =
            vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)
                ?: throw Feil("Fant ikke aktivt vedtak på behandling $behandlingId")
        val totrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)
        val revurderingVedtaksdato =
            aktivtVedtak.vedtaksdato?.toLocalDate()
                ?: throw Feil("Finner ikke revurderingsvedtaksdato på vedtak ${aktivtVedtak.id} ")
        val tilbakekreving =
            tilbakekrevingRepository.findByBehandlingId(behandling.id)
                ?: throw Feil("Fant ikke tilbakekreving på behandling ${behandling.id}")

        val manuelleBrevMottakere =
            brevmottakerRepository
                .finnBrevMottakereForBehandling(behandling.id)
                .map { mottaker ->
                    Brevmottaker(
                        type = MottakerType.valueOf(mottaker.type.name),
                        vergetype =
                            when (mottaker.type) {
                                FULLMEKTIG -> Vergetype.ANNEN_FULLMEKTIG
                                VERGE -> Vergetype.VERGE_FOR_VOKSEN
                                else -> null
                            },
                        navn = mottaker.navn,
                        manuellAdresseInfo =
                            ManuellAdresseInfo(
                                adresselinje1 = mottaker.adresselinje1,
                                adresselinje2 = mottaker.adresselinje2,
                                postnummer = mottaker.postnummer,
                                poststed = mottaker.poststed,
                                landkode = mottaker.landkode,
                            ),
                    )
                }.toSet()

        return OpprettTilbakekrevingRequest(
            fagsystem = Fagsystem.KONT,
            regelverk = behandling.kategori.tilRegelverk(),
            ytelsestype = Ytelsestype.KONTANTSTØTTE,
            eksternFagsakId = behandling.fagsak.id.toString(),
            personIdent = søker.aktør.aktivFødselsnummer(),
            eksternId = behandlingId.toString(),
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            // det er alltid false siden OpprettManueltTilbakekrevingRequest sendes for manuell opprettelse
            manueltOpprettet = false,
            språkkode = søker.målform.tilSpråkkode(),
            enhetId = arbeidsfordeling.behandlendeEnhetId,
            enhetsnavn = arbeidsfordeling.behandlendeEnhetNavn,
            saksbehandlerIdent = totrinnskontroll?.saksbehandlerId ?: SikkerhetContext.hentSaksbehandler(),
            varsel =
                if (tilbakekreving.valg == Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL) {
                    opprettVarsel(
                        varselTekst = checkNotNull(tilbakekreving.varsel),
                        simulering = simuleringService.hentSimuleringPåBehandling(behandlingId),
                    )
                } else {
                    null
                },
            revurderingsvedtaksdato = revurderingVedtaksdato,
            // TODO kommer når verge er implementert
            verge = null,
            faktainfo =
                Faktainfo(
                    revurderingsårsak = behandling.opprettetÅrsak.visningsnavn,
                    revurderingsresultat = behandling.resultat.displayName,
                    tilbakekrevingsvalg = tilbakekreving.valg,
                    konsekvensForYtelser = emptySet(),
                ),
            manuelleBrevmottakere = manuelleBrevMottakere,
            begrunnelseForTilbakekreving = tilbakekreving.begrunnelse,
        )
    }
}
