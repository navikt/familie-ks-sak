package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.VersjonsinfoDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("!prod")
@RestController
@RequestMapping("/api/preprod")
class PreprodController(
    @Value("\${APP_BRANCH:ukjent}") private val branch: String,
    @Value("\${APP_VERSION:ukjent}") private val versjon: String,
) {
    @GetMapping("/versjonsinfo")
    fun hentVersjonsinfo(): ResponseEntity<Ressurs<VersjonsinfoDto>> = ResponseEntity.ok(Ressurs.success(VersjonsinfoDto(branch = branch, versjon = versjon)))
}
