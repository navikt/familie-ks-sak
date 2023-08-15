package no.nav.familie.ks.sak.statistikk.internstatistikk

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.common.util.RessursUtils
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ks.sak.kjerne.behandling.domene.SøknadsstatistikkForPeriode
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/internstatistikk")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InternStatistikkController(
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
) {

    @GetMapping(path = ["antallSoknader"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentSøknadsstatistikkForPeriode(
        @Schema(description = "Default er første dag i forrige tertial-periode regnet fra dagens dato")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        fom: LocalDate?,
        @Schema(description = "Default er siste dag i tertial-perioden regnet fra fom-dato")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        tom: LocalDate?,
    ): ResponseEntity<Ressurs<SøknadsstatistikkForPeriode>> {
        val fomDato = fom ?: LocalDate.now().minusMonths(4).withDayOfMonth(1)
        val tomDato = tom ?: fomDato.plusMonths(4).minusDays(1)

        return RessursUtils.ok(behandlingSøknadsinfoService.hentSøknadsstatistikk(fomDato, tomDato))
    }
}
