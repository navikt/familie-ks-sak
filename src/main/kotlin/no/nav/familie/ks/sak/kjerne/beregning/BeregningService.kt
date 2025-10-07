package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.util.toYearMonth
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
import no.nav.familie.ks.sak.kjerne.beregning.domene.filtrerAndelerSomSkalSendesTilOppdrag
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
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtelse(behandlingId: Long): List<Aktør> {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return personopplysningGrunnlagRepository
            .hentByBehandlingAndAktiv(behandlingId)
            .barna
            .map { it.aktør }
            .filter { andelerTilkjentYtelse.any { aty -> aty.aktør == it } }
    }

    fun finnTilkjentYtelseForBehandling(behandlingId: Long) = tilkjentYtelseRepository.hentOptionalTilkjentYtelseForBehandling(behandlingId)

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) = tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandlingId)

    fun hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filtrerAndelerSomSkalSendesTilOppdrag()

    fun hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(fagsakId: Long): List<TilkjentYtelse> {
        val iverksatteBehandlinger = behandlingRepository.finnByFagsakAndAvsluttet(fagsakId)
        return iverksatteBehandlinger.mapNotNull {
            tilkjentYtelseRepository
                .finnByBehandlingAndHasUtbetalingsoppdrag(
                    it.id,
                )?.takeIf { tilkjentYtelse ->
                    tilkjentYtelse.andelerTilkjentYtelse.filtrerAndelerSomSkalSendesTilOppdrag().isNotEmpty()
                }
        }
    }

    /**
     For at endret utbetaling andeler skal fungere så må man generere andeler før man kobler endringene på andelene.
     Dette er fordi en endring regnes som gyldig når den overlapper med en andel og har gyldig årsak.
     Hvis man ikke genererer andeler før man kobler på endringene så vil ingen av endringene ses på som gyldige, altså ikke oppdatere noen andeler.
     Dette gjøres spesifikt fra vilkårsvurdering steget da på dette tidspunktet så har man ikke generert andeler.
     */
    fun oppdaterTilkjentYtelsePåBehandlingFraVilkårsvurdering(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
                endretUtbetalingAndeler = emptyList(),
            )

        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)

        this.oppdaterTilkjentYtelsePåBehandling(
            behandling,
            personopplysningGrunnlag,
            vilkårsvurdering,
        )
    }

    fun oppdaterTilkjentYtelsePåBehandling(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        endretUtbetalingAndel: EndretUtbetalingAndel? = null,
    ) {
        val endreteUtbetalingAndeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
                .filter {
                    when {
                        behandling.skalBehandlesAutomatisk() -> true
                        endretUtbetalingAndel != null -> it.id == endretUtbetalingAndel.id || it.andelerTilkjentYtelse.isNotEmpty()
                        else -> it.andelerTilkjentYtelse.isNotEmpty()
                    }
                }
        slettTilkjentYtelseForBehandling(behandling)

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
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
            fagsakService
                .hentFagsakerPåPerson(barnAktør)
                .filter { it.id != fagsakId }

        return andreFagsaker
            .mapNotNull { fagsak ->

                behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsakId = fagsak.id).singleOrNull()
                    ?: behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = fagsak.id).singleOrNull()
                    ?: behandlingRepository
                        .finnIverksatteBehandlinger(fagsakId = fagsak.id)
                        .filter { it.status == BehandlingStatus.AVSLUTTET }
                        .maxByOrNull { it.aktivertTidspunkt }
            }.map {
                hentTilkjentYtelseForBehandling(behandlingId = it.id)
            }.filter {
                personopplysningGrunnlagRepository
                    .findByBehandlingAndAktiv(behandlingId = it.behandling.id)
                    ?.barna
                    ?.map { barn -> barn.aktør }
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

        return personopplysningGrunnlagRepository
            .hentByBehandlingAndAktiv(behandlingId)
            .barna
            .map { it.aktør }
            .filter { aktør ->
                andelerMedEndringer
                    .filter { it.aktør == aktør }
                    .any { aty ->
                        aty.kalkulertUtbetalingsbeløp != 0 || aty.endreteUtbetalinger.isEmpty()
                    }
            }
    }

    fun innvilgetSøknadUtenUtbetalingsperioderGrunnetEndringsPerioder(behandling: Behandling): Boolean {
        val barnMedUtbetalingSomIkkeBlittEndretISisteBehandling =
            finnAlleBarnFraBehandlingMedPerioderSomSkalUtbetales(behandling.id)

        val alleBarnISisteBehanlding = finnBarnFraBehandlingMedTilkjentYtelse(behandling.id)

        val alleBarnISistIverksattBehandling =
            behandlingRepository
                .finnIverksatteBehandlinger(behandling.fagsak.id)
                .filter { it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }
                .maxByOrNull { it.aktivertTidspunkt }
                ?.let {
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
        andelTilkjentYtelseRepository
            .finnLøpendeAndelerTilkjentYtelseForBehandlinger(
                behandlingIder,
                avstemmingstidspunkt.toLocalDate().toYearMonth(),
            ).filtrerAndelerSomSkalSendesTilOppdrag()

    fun slettTilkjentYtelseForBehandling(behandling: Behandling) = tilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling)
}

interface TilkjentYtelseEndretAbonnent {
    fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse)
}
