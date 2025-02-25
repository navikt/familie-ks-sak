package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ValutakursService(
    valutakursRepository: EøsSkjemaRepository<Valutakurs>,
    endringsabonnenter: List<EøsSkjemaEndringAbonnent<Valutakurs>>,
) {
    val skjemaService =
        EøsSkjemaService(
            valutakursRepository,
            endringsabonnenter,
        )

    fun hentValutakurs(valutakursId: Long): Valutakurs = skjemaService.hentMedId(valutakursId)

    fun hentValutakurser(behandlingId: BehandlingId) = skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterValutakurs(
        behandlingId: BehandlingId,
        valutakurs: Valutakurs,
    ) = skjemaService.endreSkjemaer(behandlingId, valutakurs)

    fun slettValutakurs(
        valutakursId: Long,
    ) = skjemaService.slettSkjema(valutakursId)

    @Transactional
    fun kopierOgErstattValutakurser(
        fraBehandlingId: BehandlingId,
        tilBehandlingId: BehandlingId,
    ) = skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
