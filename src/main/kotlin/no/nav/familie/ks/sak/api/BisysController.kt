package no.nav.familie.ks.sak.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.familie.ks.sak.api.dto.BisysDto
import no.nav.familie.ks.sak.api.dto.BisysResponsDto
import no.nav.familie.ks.sak.bisys.BisysService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bisys")
@ProtectedWithClaims(issuer = "azuread")
class BisysController(private val bisysService: BisysService, private val tilgangService: TilgangService) {
    @Operation(description = "Tjeneste for BISYS for å hente utbetalingsinfo for barna.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = """Liste over kontantstøtte utbetalingsperioder med beløp for hver barn.Returnerer både fra Infotrygd og KS-SAK.
                    fomMåned: Første måned i perioden
                    tomMåned: Den siste måneden i perioden. Hvis null, så er stønaden løpende
                    beløp: utbetalingsbeløp
                    """,
                content = [
                    Content(
                        mediaType = "application/json",
                        array = (ArraySchema(schema = Schema(implementation = BisysResponsDto::class))),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig input. barnIdenter må være 11 siffer",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil", content = [Content()]),
        ],
    )
    @PostMapping(
        path = ["/hent-utbetalingsinfo"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentUtbetalingsinfo(
        @RequestBody bisysRequestDto: BisysDto,
    ): ResponseEntity<BisysResponsDto> {
        return ResponseEntity.ok(bisysService.hentUtbetalingsinfo(bisysRequestDto.fom, bisysRequestDto.identer))
    }
}
