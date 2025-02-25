package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSkjemaer
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medBehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.tidslinje.utvidelser.outerJoin
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassValutakurserTilUtenlandskePeriodebeløpService(
    valutakursRepository: EøsSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpRepository: EøsSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: List<EøsSkjemaEndringAbonnent<Valutakurs>>,
) : EøsSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    val skjemaService =
        EøsSkjemaService(
            valutakursRepository,
            endringsabonnenter,
        )

    @Transactional
    fun tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId: BehandlingId) {
        val gjeldendeUtenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.findByBehandlingId(behandlingId.id)

        tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId, gjeldendeUtenlandskePeriodebeløp)
    }

    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: List<UtenlandskPeriodebeløp>,
    ) {
        tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId, endretTil)
    }

    private fun tilpassValutakursTilUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        gjeldendeUtenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
    ) {
        val forrigeValutakurser = skjemaService.hentMedBehandlingId(behandlingId)

        val oppdaterteValutakurser =
            tilpassValutakurserTilUtenlandskePeriodebeløp(
                forrigeValutakurser,
                gjeldendeUtenlandskePeriodebeløp,
            ).medBehandlingId(behandlingId).toList()

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, forrigeValutakurser, oppdaterteValutakurser)
    }
}

internal fun tilpassValutakurserTilUtenlandskePeriodebeløp(
    forrigeValutakurser: List<Valutakurs>,
    gjeldendeUtenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
): List<Valutakurs> {
    val barnasUtenlandskePeriodebeløpTidslinjer =
        gjeldendeUtenlandskePeriodebeløp
            .tilSeparateTidslinjerForBarna()

    return forrigeValutakurser
        .tilSeparateTidslinjerForBarna()
        .outerJoin(barnasUtenlandskePeriodebeløpTidslinjer) { valutakurs, utenlandskPeriodebeløp ->
            when {
                utenlandskPeriodebeløp == null -> null
                valutakurs == null || valutakurs.valutakode != utenlandskPeriodebeløp.valutakode ->
                    Valutakurs.NULL.copy(valutakode = utenlandskPeriodebeløp.valutakode)
                else -> valutakurs
            }
        }.tilSkjemaer()
}
