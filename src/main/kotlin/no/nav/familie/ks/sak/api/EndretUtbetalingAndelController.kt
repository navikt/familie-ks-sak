package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingBegrunnelseResponseDto
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
@RequestMapping("/api/endretutbetalingandel")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EndretUtbetalingAndelController(
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val sanityService: SanityService,
    ) {
    @PutMapping(path = ["/{behandlingId}/{endretUtbetalingAndelId}"])
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable endretUtbetalingAndelId: Long,
        @RequestBody endretUtbetalingAndelRequestDto: EndretUtbetalingAndelRequestDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdater endretutbetalingandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            endretUtbetalingAndelId,
            endretUtbetalingAndelRequestDto,
        )

        tilbakestillBehandlingService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @DeleteMapping(path = ["/{behandlingId}/{endretUtbetalingAndelId}"])
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable endretUtbetalingAndelId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.DELETE,
            handling = "Fjern endretutbetalingandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        endretUtbetalingAndelService.fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            endretUtbetalingAndelId,
        )

        tilbakestillBehandlingService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun lagreEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.CREATE,
            handling = "Lagre endretutbetalingandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        endretUtbetalingAndelService.opprettTomEndretUtbetalingAndel(behandling)

        tilbakestillBehandlingService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    fun hentSanityTekstIRestFormat (sanityBegrunnelser: List<SanityBegrunnelse>, begrunnelse:IBegrunnelse): List<EndretUtbetalingBegrunnelseResponseDto> {
        val sanityBegrunnelse = begrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)

        if (sanityBegrunnelse != null) {
            val visningsnavn = sanityBegrunnelse.navnISystem

            return listOf(EndretUtbetalingBegrunnelseResponseDto(
                    id = begrunnelse,
                    navn = visningsnavn,
                ))
        }

        return emptyList()
    }

    @GetMapping(
        path = ["/endret-utbetaling-begrunnelser"])
    fun hentBegrunnelser(): ResponseEntity<Ressurs<Map<BegrunnelseType, List<EndretUtbetalingBegrunnelseResponseDto>>>> {
        // Hent ut de råe sanityTekstene
        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        // Map sanityTekstene til allerede definerte begrunnelsesArrays og formater de riktig
        val mappingTilSanityTekster = (NasjonalEllerFellesBegrunnelse.entries + EØSBegrunnelse.entries)
            .groupBy { it.begrunnelseType }
            .mapValues { begrunnelseGruppe ->
                begrunnelseGruppe.value
                    .flatMap { endretUtbetalingBegrunnelse ->
                        hentSanityTekstIRestFormat(
                            sanityBegrunnelser,
                            endretUtbetalingBegrunnelse,
                        )
                    }
            }

        // Returnere en array med sanitytekster som det gjelder
        println(mappingTilSanityTekster)
        return ResponseEntity.ok(Ressurs.success(mappingTilSanityTekster));
    }
}
