package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.brev.BrevService
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
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/brev")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevController(
    private val brevService: BrevService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService
) {

    @PostMapping(path = ["/forhåndsvis-brev/{behandlingId}"])
    fun hentForhåndsvisning(
        @PathVariable behandlingId: Long,
        @RequestBody manueltBrevDto: ManueltBrevDto
    ): Ressurs<ByteArray> {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} henter forhåndsvisning av brev " +
                "for mal: ${manueltBrevDto.brevmal}"
        )
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "hente forhåndsvisning brev"
        )

        return brevService.hentForhåndsvisningAvBrev(
            behandlingId = behandlingId,
            manueltBrevDto = manueltBrevDto
        ).let { Ressurs.success(it) }
    }

    @PostMapping(path = ["/send-brev/{behandlingId}"])
    fun sendBrev(
        @PathVariable behandlingId: Long,
        @RequestBody manueltBrevDto: ManueltBrevDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og sender brev: ${manueltBrevDto.brevmal}")
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "sende brev"
        )

        brevService.genererOgSendBrev(behandlingId = behandlingId, manueltBrevDto = manueltBrevDto)

        return ResponseEntity.ok(
            Ressurs.success(
                behandlingService.lagBehandlingRespons(behandlingId = behandlingId)
            )
        )
    }

    @GetMapping(path = ["/forhåndsvis-vedtaksbrev/{behandlingId}"])
    fun genererVedtaksbrev(@PathVariable behandlingId: Long): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Vis vedtaksbrev"
        )

        return Ressurs.success(brevService.genererBrevForBehandling(behandlingId))
    }

    @Transactional
    @PostMapping(path = ["forhåndsvis-og-lagre-vedtaksbrev/{behandlingId}"])
    fun genererOgLagreVedtaksbrev(@PathVariable behandlingId: Long): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Vis vedtaksbrev"
        )

        val generertPdf = brevService.genererBrevForBehandling(behandlingId)

        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)
        vedtak.stønadBrevPdf = generertPdf

        return Ressurs.success(generertPdf)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(BrevController::class.java)
    }
}
