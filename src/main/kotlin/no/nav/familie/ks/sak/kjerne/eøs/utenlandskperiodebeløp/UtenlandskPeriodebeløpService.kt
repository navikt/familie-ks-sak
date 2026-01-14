package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.isSameOrAfter
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

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

    fun hentUtenlandskePeriodebeløp(behandlingId: BehandlingId) = skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        utenlandskPeriodebeløp: UtenlandskPeriodebeløp,
    ) {
        validerUtenlandskPeriodeBeløp(utenlandskPeriodebeløp)

        skjemaService.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)
    }

    fun slettUtenlandskPeriodebeløp(
        utenlandskPeriodebeløpId: Long,
    ) = skjemaService.slettSkjema(utenlandskPeriodebeløpId)

    @Transactional
    fun kopierOgErstattUtenlandskPeriodebeløp(
        fraBehandlingId: BehandlingId,
        tilBehandlingId: BehandlingId,
    ) = skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)

    private fun validerUtenlandskPeriodeBeløp(utenlandskPeriodebeløp: UtenlandskPeriodebeløp) {
        val fom = utenlandskPeriodebeløp.fom ?: throw FunksjonellFeil("Fra og med dato på utenlandskperiode beløp må være satt")
        val tom = utenlandskPeriodebeløp.tom ?: TIDENES_ENDE.toYearMonth()
        val januar2026 = YearMonth.of(2026, 1)

        if (utenlandskPeriodebeløp.valutakode == BULGARSK_LEV &&
            ((fom.isSameOrAfter(januar2026)) || (tom.isSameOrAfter(januar2026)))
        ) {
            throw FunksjonellFeil(
                "Bulgarske lev er ikke lenger gyldig valuta fra 01.01.26",
            )
        }
    }

    companion object {
        const val BULGARSK_LEV = "BGN"
    }
}
