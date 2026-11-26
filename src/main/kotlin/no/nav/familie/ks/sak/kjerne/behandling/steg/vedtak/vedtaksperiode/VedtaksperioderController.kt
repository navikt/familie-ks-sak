package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.brev.BrevKlient
import no.nav.familie.ks.sak.kjerne.brev.BrevPeriodeService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDtoMedData
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.FritekstBegrunnelseDto
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtaksperioder")
@Validated
class VedtaksperioderController(
    private val tilgangService: TilgangService,
    private val brevPeriodeService: BrevPeriodeService,
    private val brevKlient: BrevKlient,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
) {
    @GetMapping("/{vedtaksperiodeId}/brevbegrunnelser")
    fun genererBegrunnelserForPeriode(
        @PathVariable vedtaksperiodeId: Long,
    ): ResponseEntity<Ressurs<List<String>>> {
        val behandlingId =
            vedtaksperiodeHentOgPersisterService.finnBehandlingIdFor(vedtaksperiodeId)
                ?: throw FunksjonellFeil("Vedtaksperiodene har blitt oppdatert. Oppdater siden og forsøk på nytt.")
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente begrunnelsetekster",
        )

        val begrunnelser =
            brevPeriodeService.hentBegrunnelsesteksterForPeriode(vedtaksperiodeId).map {
                when (it) {
                    is FritekstBegrunnelseDto -> it.fritekst
                    is BegrunnelseDtoMedData -> brevKlient.hentBegrunnelsestekst(it)
                }
            }

        return ResponseEntity.ok(Ressurs.Companion.success(begrunnelser))
    }
}
