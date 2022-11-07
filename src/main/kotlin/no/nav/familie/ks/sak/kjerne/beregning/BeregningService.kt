package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service

@Service
class BeregningService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {

    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtelse(behandlingId: Long): List<Aktør> {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId).barna.map { it.aktør }
            .filter { andelerTilkjentYtelse.any { aty -> aty.aktør == it } }
    }

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandlingId)

    fun oppdaterTilkjentYtelsePåBehandling(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        endretUtbetalingAndel: EndretUtbetalingAndel? = null
    ) {
        val endreteUtbetalingAndeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
                .filter {
                    when {
                        endretUtbetalingAndel != null -> it.id == endretUtbetalingAndel.id
                        else -> it.andelerTilkjentYtelse.isNotEmpty()
                    }
                }
        tilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering,
            personopplysningGrunnlag,
            endreteUtbetalingAndeler
        )

        tilkjentYtelseRepository.save(tilkjentYtelse)
    }

    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse som ikke er endret til null i utbetaling.
     */
    fun finnAlleBarnFraBehandlingMedPerioderSomSkalUtbetales(behandlingId: Long): List<Aktør> {
        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        return personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId).barna.map { it.aktør }
            .filter { aktør ->
                andelerMedEndringer
                    .filter { it.aktør == aktør }.any { aty ->
                        aty.kalkulertUtbetalingsbeløp != 0 || aty.endreteUtbetalinger.isEmpty()
                    }
            }
    }

    fun innvilgetSøknadUtenUtbetalingsperioderGrunnetEndringsPerioder(behandling: Behandling): Boolean {
        val barnMedUtbetalingSomIkkeBlittEndretISisteBehandling =
            finnAlleBarnFraBehandlingMedPerioderSomSkalUtbetales(behandling.id)

        val alleBarnISisteBehanlding = finnBarnFraBehandlingMedTilkjentYtelse(behandling.id)

        val alleBarnISistIverksattBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)?.let {
                finnBarnFraBehandlingMedTilkjentYtelse(
                    it.id
                )
            }
                ?: emptyList()

        val nyeBarnISisteBehandling = alleBarnISisteBehanlding.minus(alleBarnISistIverksattBehandling.toSet())

        val nyeBarnMedUtebtalingSomIkkeErEndret =
            barnMedUtbetalingSomIkkeBlittEndretISisteBehandling.intersect(nyeBarnISisteBehandling)

        return behandling.resultat == Behandlingsresultat.INNVILGET_OG_OPPHØRT &&
            behandling.erSøknad() &&
            nyeBarnMedUtebtalingSomIkkeErEndret.isEmpty()
    }
}
