package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.brev.BrevKlient
import no.nav.familie.ks.sak.kjerne.brev.BrevPeriodeService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.FritekstBegrunnelseDto
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtaksperioder")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksperioderController(
    private val tilgangService: TilgangService,
    private val brevPeriodeService: BrevPeriodeService,
    private val brevKlient: BrevKlient,
) {

    @GetMapping("/{vedtaksperiodeId}/brevbegrunnelser")
    fun genererBegrunnelserForPeriode(@PathVariable vedtaksperiodeId: Long): ResponseEntity<Ressurs<List<String>>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Henter begrunnelsetekster",
        )

        val begrunnelser = brevPeriodeService.hentBegrunnelsesteksterForPeriode(vedtaksperiodeId).map {
            when (it) {
                is FritekstBegrunnelseDto -> it.fritekst
                is BegrunnelseDataDto -> brevKlient.hentBegrunnelsestekst(it)
                is EØSBegrunnelseDataDto -> brevKlient.hentBegrunnelsestekst(it)
            }
        }

        return ResponseEntity.ok(Ressurs.Companion.success(begrunnelser))
    }
}
