package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BeregningService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingRepository: BehandlingRepository,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val fagsakService: FagsakService,
    private val tilkjentYtelseEndretAbonnenter: List<TilkjentYtelseEndretAbonnent> = emptyList(),
    private val behandlingService: BehandlingService,
) {
    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtelse(behandlingId: Long): List<Aktør> {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId).barna.map { it.aktør }
            .filter { andelerTilkjentYtelse.any { aty -> aty.aktør == it } }
    }

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) = tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandlingId)

    fun hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(fagsakId: Long): List<TilkjentYtelse> {
        val iverksatteBehandlinger = behandlingRepository.finnByFagsakAndAvsluttet(fagsakId)
        return iverksatteBehandlinger.mapNotNull {
            tilkjentYtelseRepository.finnByBehandlingAndHasUtbetalingsoppdrag(
                it.id,
            )?.takeIf { tilkjentYtelse ->
                tilkjentYtelse.andelerTilkjentYtelse.any { aty -> aty.erAndelSomSkalSendesTilOppdrag() }
            }
        }
    }

    fun oppdaterTilkjentYtelsePåBehandling(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        endretUtbetalingAndel: EndretUtbetalingAndel? = null,
    ) {
        val endreteUtbetalingAndeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
                .filter {
                    when {
                        endretUtbetalingAndel != null -> it.id == endretUtbetalingAndel.id || it.andelerTilkjentYtelse.isNotEmpty()
                        else -> it.andelerTilkjentYtelse.isNotEmpty()
                    }
                }
        slettTilkjentYtelseForBehandling(behandling)

        val tilkjentYtelse =
            TilkjentYtelseUtils.beregnTilkjentYtelse(
                vilkårsvurdering,
                personopplysningGrunnlag,
                endreteUtbetalingAndeler,
            )

        val lagretTilkjentYtelse = tilkjentYtelseRepository.save(tilkjentYtelse)
        tilkjentYtelseEndretAbonnenter.forEach { it.endretTilkjentYtelse(lagretTilkjentYtelse) }
    }

    /**
     * Denne metoden henter alle relaterte behandlinger på en person.
     * Per fagsak henter man tilkjent ytelse fra:
     * 1. Behandling som er til godkjenning
     * 2. Siste behandling som er iverksatt
     * 3. Filtrer bort behandlinger der barnet ikke lenger finnes
     */
    fun hentRelevanteTilkjentYtelserForBarn(
        barnAktør: Aktør,
        fagsakId: Long,
    ): List<TilkjentYtelse> {
        val andreFagsaker =
            fagsakService.hentFagsakerPåPerson(barnAktør)
                .filter { it.id != fagsakId }

        return andreFagsaker.mapNotNull { fagsak ->

            behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsakId = fagsak.id).singleOrNull()
                ?: behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = fagsak.id).singleOrNull()
                ?: behandlingRepository.finnIverksatteBehandlinger(fagsakId = fagsak.id)
                    .filter { it.status == BehandlingStatus.AVSLUTTET }
                    .maxByOrNull { it.opprettetTidspunkt }
        }.map {
            hentTilkjentYtelseForBehandling(behandlingId = it.id)
        }.filter {
            personopplysningGrunnlagRepository
                .findByBehandlingAndAktiv(behandlingId = it.behandling.id)
                ?.barna?.map { barn -> barn.aktør }
                ?.contains(barnAktør)
                ?: false
        }
    }

    /*
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse som ikke er endret til null i utbetaling.
     */
    fun finnAlleBarnFraBehandlingMedPerioderSomSkalUtbetales(behandlingId: Long): List<Aktør> {
        val andelerMedEndringer =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
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
            behandlingRepository.finnIverksatteBehandlinger(behandling.fagsak.id)
                .filter { it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }.maxByOrNull { it.opprettetTidspunkt }?.let {
                    finnBarnFraBehandlingMedTilkjentYtelse(
                        it.id,
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

    fun hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: LocalDateTime,
    ): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnLøpendeAndelerTilkjentYtelseForBehandlinger(
            behandlingIder,
            avstemmingstidspunkt.toLocalDate().toYearMonth(),
        ).filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun slettTilkjentYtelseForBehandling(behandling: Behandling) = tilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling)

    fun erEndringIUtbetaling(behandling: Behandling): Boolean {
        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
        val andelerForrigeBehandling =
            forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }
                ?: emptyList()

        val andelerBehandling = behandling.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }

        return lagEndringIUtbetalingTidslinje(andelerBehandling, andelerForrigeBehandling)
            .tilPerioder().any { it.verdi == true }
    }
}

interface TilkjentYtelseEndretAbonnent {
    fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse)
}
