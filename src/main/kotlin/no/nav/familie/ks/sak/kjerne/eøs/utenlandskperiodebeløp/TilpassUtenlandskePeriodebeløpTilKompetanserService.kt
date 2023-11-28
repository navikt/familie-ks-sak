package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.outerJoin
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.filtrer
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSkjemaer
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassUtenlandskePeriodebeløpTilKompetanserService(
    utenlandskPeriodebeløpRepository: EøsSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: List<EøsSkjemaEndringAbonnent<UtenlandskPeriodebeløp>>,
    private val kompetanseRepository: EøsSkjemaRepository<Kompetanse>,
) : EøsSkjemaEndringAbonnent<Kompetanse> {
    val skjemaService =
        EøsSkjemaService(
            utenlandskPeriodebeløpRepository,
            endringsabonnenter,
        )

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: Long) {
        val gjeldendeKompetanser = kompetanseRepository.findByBehandlingId(behandlingId)

        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, gjeldendeKompetanser)
    }

    @Transactional
    override fun skjemaerEndret(
        behandlingId: Long,
        endretTil: List<Kompetanse>,
    ) {
        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, endretTil)
    }

    private fun tilpassUtenlandskPeriodebeløpTilKompetanser(
        behandlingId: Long,
        gjeldendeKompetanser: List<Kompetanse>,
    ) {
        val forrigeUtenlandskePeriodebeløp = skjemaService.hentMedBehandlingId(behandlingId)

        val oppdaterteUtenlandskPeriodebeløp =
            tilpassUtenlandskePeriodebeløpTilKompetanser(
                forrigeUtenlandskePeriodebeløp,
                gjeldendeKompetanser,
            ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(
            behandlingId,
            forrigeUtenlandskePeriodebeløp,
            oppdaterteUtenlandskPeriodebeløp.toList(),
        )
    }
}

internal fun tilpassUtenlandskePeriodebeløpTilKompetanser(
    forrigeUtenlandskePeriodebeløp: Iterable<UtenlandskPeriodebeløp>,
    gjeldendeKompetanser: Iterable<Kompetanse>,
): Collection<UtenlandskPeriodebeløp> {
    val barnasKompetanseTidslinjer =
        gjeldendeKompetanser.tilSeparateTidslinjerForBarna()
            .filtrerSekundærland()

    return forrigeUtenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
        .outerJoin(barnasKompetanseTidslinjer) { upb, kompetanse ->
            when {
                kompetanse == null -> null
                upb == null || upb.utbetalingsland != kompetanse.annenForeldersAktivitetsland ->
                    UtenlandskPeriodebeløp.NULL.copy(utbetalingsland = kompetanse.annenForeldersAktivitetsland)
                else -> upb
            }
        }
        .tilSkjemaer()
}

fun Map<Aktør, Tidslinje<Kompetanse>>.filtrerSekundærland() =
    this.mapValues { (_, tidslinje) -> tidslinje.filtrer { it?.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND } }

fun <T : EøsSkjemaEntitet<T>> Collection<T>.medBehandlingId(behandlingId: Long): Collection<T> {
    this.forEach { it.behandlingId = behandlingId }
    return this
}
