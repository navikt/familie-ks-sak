package no.nav.familie.ks.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseEndretAbonnent
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.oppdaterTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// interface BarnasDifferanseberegningEndretAbonnent {
//    fun barnasDifferanseberegningEndret(tilkjentYtelse: TilkjentYtelse)
// }

@Service
class TilpassDifferanseberegningEtterTilkjentYtelseService(
    private val valutakursRepository: EøsSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpRepository: EøsSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
//    private val barnasDifferanseberegningEndretAbonnenter: List<BarnasDifferanseberegningEndretAbonnent>,
) : TilkjentYtelseEndretAbonnent {
    @Transactional
    override fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) {
        val behandlingId = BehandlingId(tilkjentYtelse.behandling.id)
        val valutakurser = valutakursRepository.findByBehandlingId(behandlingId.id)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.findByBehandlingId(behandlingId.id)

        val oppdaterteAndeler =
            beregnDifferanse(
                tilkjentYtelse.andelerTilkjentYtelse.toList(),
                utenlandskePeriodebeløp,
                valutakurser,
            )

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
//        barnasDifferanseberegningEndretAbonnenter.forEach { it.barnasDifferanseberegningEndret(oppdatertTilkjentYtelse) }
    }
}

@Service
class TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(
    private val valutakursRepository: EøsSkjemaRepository<Valutakurs>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
//    private val barnasDifferanseberegningEndretAbonnenter: List<BarnasDifferanseberegningEndretAbonnent>,
) : EøsSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.hentOptionalTilkjentYtelseForBehandling(behandlingId.id) ?: return
        val valutakurser = valutakursRepository.findByBehandlingId(behandlingId.id)

        val oppdaterteAndeler =
            beregnDifferanse(
                tilkjentYtelse.andelerTilkjentYtelse.toList(),
                utenlandskePeriodebeløp,
                valutakurser,
            )

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
//        barnasDifferanseberegningEndretAbonnenter.forEach { it.barnasDifferanseberegningEndret(oppdatertTilkjentYtelse) }
    }
}

@Service
class TilpassDifferanseberegningEtterValutakursService(
    private val utenlandskPeriodebeløpRepository: EøsSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
//    private val barnasDifferanseberegningEndretAbonnenter: List<BarnasDifferanseberegningEndretAbonnent>,
) : EøsSkjemaEndringAbonnent<Valutakurs> {
    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        valutakurser: List<Valutakurs>,
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.hentOptionalTilkjentYtelseForBehandling(behandlingId.id) ?: return
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.findByBehandlingId(behandlingId.id)

        val oppdaterteAndeler =
            beregnDifferanse(
                tilkjentYtelse.andelerTilkjentYtelse.toList(),
                utenlandskePeriodebeløp,
                valutakurser,
            )

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
//        barnasDifferanseberegningEndretAbonnenter.forEach { it.barnasDifferanseberegningEndret(oppdatertTilkjentYtelse) }
    }
}

// @Service
// class TilpassDifferanseberegningSøkersYtelserService(
//     private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
//     private val kompetanseRepository: KompetanseRepository,
//     private val tilkjentYtelseRepository: TilkjentYtelseRepository,
// ) : BarnasDifferanseberegningEndretAbonnent {
//     override fun barnasDifferanseberegningEndret(tilkjentYtelse: TilkjentYtelse) {
//         val oppdaterteAndeler =
//             tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(
//                 barna = personopplysningGrunnlagService.hentBarna(tilkjentYtelse.behandling.id),
//                 kompetanser = kompetanseRepository.findByBehandlingId(tilkjentYtelse.behandling.id),
//             )
//         tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
//     }
// }
