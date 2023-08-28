package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.GenererFortsattInnvilgetVedtaksperioderDto
import no.nav.familie.ks.sak.api.dto.GenererVedtaksperioderForOverstyrtEndringstidspunktDto
import no.nav.familie.ks.sak.api.dto.VedtaksperiodeMedBegrunnelserDto
import no.nav.familie.ks.sak.api.dto.VedtaksperiodeMedFriteksterDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtaksperioder")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksperiodeMedBegrunnelserController(
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val tilgangService: TilgangService,
) {

    @PutMapping("/begrunnelser/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeMedBegrunnelser(
        @PathVariable vedtaksperiodeId: Long,
        @RequestBody vedtaksperiodeMedBegrunnelserDto: VedtaksperiodeMedBegrunnelserDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "oppdatere vedtaksperiode med begrunnelser",
        )

        val begrunnelser = vedtaksperiodeMedBegrunnelserDto.begrunnelser.mapNotNull {
            konverterTilEnumverdi<Begrunnelse>(it)
        }

        val eøsBegrunnelser = vedtaksperiodeMedBegrunnelserDto.begrunnelser.mapNotNull {
            konverterTilEnumverdi<EØSBegrunnelse>(it)
        }

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
            vedtaksperiodeId = vedtaksperiodeId,
            begrunnelserFraFrontend = begrunnelser,
            eøsBegrunnelserFraFrontend = eøsBegrunnelser,
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = vedtak.behandling.id)))
    }

    @PutMapping("/fritekster/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeMedFritekster(
        @PathVariable vedtaksperiodeId: Long,
        @RequestBody vedtaksperiodeMedFriteksterDto: VedtaksperiodeMedFriteksterDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "oppdatere vedtaksperiode med fritekster",
        )

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(
            vedtaksperiodeId,
            vedtaksperiodeMedFriteksterDto.fritekster,
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = vedtak.behandling.id)))
    }

    @PutMapping("/endringstidspunkt")
    fun genererVedtaksperioderTilOgMedFørsteEndringstidspunkt(
        @RequestBody genererVedtaksperioderForOverstyrtEndringstidspunktDto: GenererVedtaksperioderForOverstyrtEndringstidspunktDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Generer vedtaksperioder",
        )

        vedtaksperiodeService.genererVedtaksperiodeForOverstyrtEndringstidspunkt(
            genererVedtaksperioderForOverstyrtEndringstidspunktDto.behandlingId,
            genererVedtaksperioderForOverstyrtEndringstidspunktDto.overstyrtEndringstidspunkt,
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = genererVedtaksperioderForOverstyrtEndringstidspunktDto.behandlingId)))
    }

    /*
     * Dette endepnktet brukes for å overstyre hva slags vedtaksperioder man ønsker når resultatet er fortsatt innvilget.
     * Muligheter:
     * - skalGenererePerioderForFortsattInnvilget = false -> det blir kun generert 1 periode, uten dato (default valg for fortsatt innvilget)
     * - skalGenererePerioderForFortsattInnvilget = true -> det blir generert 'vanlige' perioder (overstyrer default for fortsatt innvilget)
     */
    @PutMapping("/overstyr-fortsatt-innvilget-vedtaksperioder")
    fun genererFortsattInnvilgetVedtaksperioder(
        @RequestBody genererFortsattInnvilgetVedtaksperioderDto: GenererFortsattInnvilgetVedtaksperioderDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "oppdater vedtaksperioder fortsatt innvilget",
        )

        val vedtak =
            vedtakService.hentAktivVedtakForBehandling(behandlingId = genererFortsattInnvilgetVedtaksperioderDto.behandlingId)

        if (vedtak.behandling.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
            throw FunksjonellFeil(
                melding = "Kan ikke overstyre vedtaksperioder når resultatet ikke er fortsatt innvilget.",
            )
        }

        vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(
            vedtak = vedtak,
            skalOverstyreFortsattInnvilget = genererFortsattInnvilgetVedtaksperioderDto.skalGenererePerioderForFortsattInnvilget,
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = vedtak.behandling.id)))
    }

    private inline fun <reified T> konverterTilEnumverdi(it: String): T? where T : Enum<T> =
        enumValues<T>().find { enum -> enum.name == it }
}
