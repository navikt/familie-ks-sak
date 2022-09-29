package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service

@Service
class BeregningService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtelse(behandlingId: Long): List<Aktør> {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId).barna.map { it.aktør }
            .filter { andelerTilkjentYtelse.any { aty -> aty.aktør == it } }
    }
}
