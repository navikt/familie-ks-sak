package no.nav.familie.ks.sak.api

import jakarta.transaction.Transactional
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.MinimalFagsakResponsDto
import no.nav.familie.ks.sak.api.dto.leggTilEnhet
import no.nav.familie.ks.sak.api.dto.utvidManueltBrevDtoMedEnhetOgMottaker
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.GenererBrevService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/brev")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevController(
    private val brevService: BrevService,
    private val genererBrevService: GenererBrevService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val fagsakService: FagsakService,
) {
    @PostMapping(path = ["/forhaandsvis-brev/{behandlingId}"])
    fun hentForhåndsvisning(
        @PathVariable behandlingId: Long,
        @RequestBody manueltBrevDto: ManueltBrevDto,
    ): Ressurs<ByteArray> {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} henter forhåndsvisning av brev " +
                "for mal: ${manueltBrevDto.brevmal}",
        )
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "hente forhåndsvisning brev",
        )

        return brevService
            .hentForhåndsvisningAvBrev(
                manueltBrevDto = manueltBrevDto.utvidManueltBrevDtoMedEnhetOgMottaker(behandlingId, personopplysningGrunnlagService, arbeidsfordelingService),
            ).let { Ressurs.success(it) }
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/forhaandsvis-brev"])
    fun hentForhåndsvisningPåFagsak(
        @PathVariable fagsakId: Long,
        @RequestBody manueltBrevDto: ManueltBrevDto,
    ): Ressurs<ByteArray> {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} henter forhåndsvisning av brev på fagsak $fagsakId " +
                "for mal: ${manueltBrevDto.brevmal}",
        )
        tilgangService.validerTilgangTilHandlingOgFagsakForPerson(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "hente forhåndsvisning brev",
            personIdent = manueltBrevDto.mottakerIdent,
            event = AuditLoggerEvent.ACCESS,
        )

        return brevService
            .hentForhåndsvisningAvBrev(
                manueltBrevDto = manueltBrevDto.leggTilEnhet(arbeidsfordelingService),
            ).let { Ressurs.success(it) }
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/send-brev"])
    fun sendBrevPåFagsak(
        @PathVariable fagsakId: Long,
        @RequestBody manueltBrevDto: ManueltBrevDto,
    ): ResponseEntity<Ressurs<MinimalFagsakResponsDto>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og sender brev på fagsak $fagsakId: ${manueltBrevDto.brevmal}")
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "sende brev",
        )

        val fagsak = fagsakService.hentFagsak(fagsakId)

        brevService.sendBrevNy(
            manueltBrevDto = manueltBrevDto.leggTilEnhet(arbeidsfordelingService),
            fagsak = fagsak,
        )

        return ResponseEntity.ok(Ressurs.success(fagsakService.hentMinimalFagsak(fagsakId = fagsakId)))
    }

    @PostMapping(path = ["/send-brev/{behandlingId}"])
    fun sendBrev(
        @PathVariable behandlingId: Long,
        @RequestBody manueltBrevDto: ManueltBrevDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og sender brev: ${manueltBrevDto.brevmal}")
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "sende brev",
        )

        brevService.genererOgSendBrev(behandlingId = behandlingId, manueltBrevDto = manueltBrevDto)

        return ResponseEntity.ok(
            Ressurs.success(
                behandlingService.lagBehandlingRespons(behandlingId = behandlingId),
            ),
        )
    }

    @GetMapping(path = ["/forhaandsvis-vedtaksbrev/{behandlingId}"])
    fun hentVedtaksbrev(
        @PathVariable behandlingId: Long,
    ): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent vedtaksbrev",
        )

        val vedtaksbrev =
            vedtakService.hentAktivVedtakForBehandling(behandlingId).stønadBrevPdf
                ?: throw Feil("Klarte ikke finne vedtaksbrev for behandling med id $behandlingId")

        return Ressurs.success(vedtaksbrev)
    }

    @Transactional
    @PostMapping(path = ["forhaandsvis-og-lagre-vedtaksbrev/{behandlingId}"])
    fun genererOgLagreVedtaksbrev(
        @PathVariable behandlingId: Long,
    ): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Vis og lagre vedtaksbrev",
        )

        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)
        val generertPdf = genererBrevService.genererBrevForBehandling(vedtak)

        vedtak.stønadBrevPdf = generertPdf

        return Ressurs.success(generertPdf)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BrevController::class.java)
    }
}
