package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/brev")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevController(private val brevService: BrevService, private val tilgangService: TilgangService) {

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

    companion object {

        private val logger = LoggerFactory.getLogger(BrevController::class.java)
    }
}
