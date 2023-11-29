package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import org.springframework.stereotype.Service

@Service
class UtenlandskPeriodebeløpService(
    utenlandskPeriodebeløpRepository: EøsSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: List<EøsSkjemaEndringAbonnent<UtenlandskPeriodebeløp>>,
) {
    val skjemaService =
        EøsSkjemaService(
            utenlandskPeriodebeløpRepository,
            endringsabonnenter,
        )

    fun hentUtenlandskePeriodebeløp(behandlingId: BehandlingId) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        utenlandskPeriodebeløp: UtenlandskPeriodebeløp,
    ) =
        skjemaService.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)

    fun slettUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        utenlandskPeriodebeløpId: Long,
    ) =
        skjemaService.slettSkjema(utenlandskPeriodebeløpId)

//    @Transactional
//    fun kopierOgErstattUtenlandskPeriodebeløp(
//        fraBehandlingId: Long,
//        tilBehandlingId: Long,
//    ) =
//        skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
