package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import no.nav.familie.ks.sak.api.dto.OppdaterSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.OpprettSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.SammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.SlettSammensattKontrollsakDto
import no.nav.familie.prosessering.rest.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sammensatt-kontrollsak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SammensattKontrollsakController(
    private val sammensattKontrollsakValidator: SammensattKontrollsakValidator,
    private val sammensattKontrollsakService: SammensattKontrollsakService,
) {
    @GetMapping(
        path = ["/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentSammensattKontrollsak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<SammensattKontrollsakDto?>> {
        sammensattKontrollsakValidator.validerHentSammensattKontrollsakTilgang()

        val sammensattKontrollsak =
            sammensattKontrollsakService.finnSammensattKontrollsakForBehandling(
                behandlingId = behandlingId,
            )

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak?.tilSammensattKontrollDto()))
    }

    @PostMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun opprettSammensattKontrollsak(
        @RequestBody opprettSammensattKontrollsakDto: OpprettSammensattKontrollsakDto,
    ): ResponseEntity<Ressurs<SammensattKontrollsakDto>> {
        sammensattKontrollsakValidator.validerOpprettSammensattKontrollsakTilgang()

        sammensattKontrollsakValidator.validerRedigerbarBehandlingForBehandlingId(
            behandlingId = opprettSammensattKontrollsakDto.behandlingId,
        )

        val sammensattKontrollsak =
            sammensattKontrollsakService.opprettSammensattKontrollsak(
                opprettSammensattKontrollsakDto = opprettSammensattKontrollsakDto,
            )

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak.tilSammensattKontrollDto()))
    }

    @PutMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterSammensattKontrollsak(
        @RequestBody oppdaterSammensattKontrollsakDto: OppdaterSammensattKontrollsakDto,
    ): ResponseEntity<Ressurs<SammensattKontrollsakDto>> {
        sammensattKontrollsakValidator.validerOppdaterSammensattKontrollsakTilgang()

        sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
            sammensattKontrollsakId = oppdaterSammensattKontrollsakDto.id,
        )

        val sammensattKontrollsak =
            sammensattKontrollsakService.oppdaterSammensattKontrollsak(
                oppdaterSammensattKontrollsakDto = oppdaterSammensattKontrollsakDto,
            )

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak.tilSammensattKontrollDto()))
    }

    @DeleteMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun slettSammensattKontrollsak(
        @RequestBody slettSammensattKontrollsakDto: SlettSammensattKontrollsakDto,
    ): ResponseEntity<Ressurs<Long>> {
        sammensattKontrollsakValidator.validerSlettSammensattKontrollsakTilgang()

        sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
            sammensattKontrollsakId = slettSammensattKontrollsakDto.id,
        )

        sammensattKontrollsakService.slettSammensattKontrollsak(
            slettSammensattKontrollsakDto = slettSammensattKontrollsakDto,
        )

        return ResponseEntity.ok(Ressurs.success(slettSammensattKontrollsakDto.id))
    }
}
