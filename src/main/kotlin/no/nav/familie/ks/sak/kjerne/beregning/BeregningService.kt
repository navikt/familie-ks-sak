package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.AndelTilkjentYtelseForUtbetalingsoppdragFactory
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.pakkInnForUtbetaling
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
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
import java.time.LocalDate

@Service
class BeregningService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingRepository: BehandlingRepository,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val fagsakService: FagsakService
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

    fun hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(fagsakId: Long): List<TilkjentYtelse> {
        val iverksatteBehandlinger = behandlingRepository.finnByFagsakAndAvsluttet(fagsakId)
        return iverksatteBehandlinger.mapNotNull {
            tilkjentYtelseRepository.finnByBehandlingAndHasUtbetalingsoppdrag(
                it.id
            )?.takeIf { tilkjentYtelse ->
                tilkjentYtelse.andelerTilkjentYtelse.any { aty -> aty.erAndelSomSkalSendesTilOppdrag() }
            }
        }
    }

    fun lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelseRepository.save(tilkjentYtelse)

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
     * Denne metoden henter alle relaterte behandlinger på en person.
     * Per fagsak henter man tilkjent ytelse fra:
     * 1. Behandling som er til godkjenning
     * 2. Siste behandling som er iverksatt
     * 3. Filtrer bort behandlinger der barnet ikke lenger finnes
     */
    fun hentRelevanteTilkjentYtelserForBarn(
        barnAktør: Aktør,
        fagsakId: Long
    ): List<TilkjentYtelse> {
        val andreFagsaker = fagsakService.hentFagsakerPåPerson(barnAktør)
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

    // Har ikke klart å identifisere at opphørFom på TilkjentYtelse faktisk brukes til noe, så mulig mye av logikken her kan fjernes, men har latt det være slik det er i BA inntil videre.
    fun populerTilkjentYtelse(
        behandling: Behandling,
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): TilkjentYtelse {
        val erRentOpphør =
            utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
        var opphørsdato: LocalDate? = null
        if (erRentOpphør) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode.minOf { it.opphør!!.opphørDatoFom }
        }

        if (behandling.type == BehandlingType.REVURDERING) {
            val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            if (opphørPåRevurdering.isNotEmpty()) {
                opphørsdato = opphørPåRevurdering.maxByOrNull { it.opphør!!.opphørDatoFom }!!.opphør!!.opphørDatoFom
            }
        }

        val tilkjentYtelse =
            tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandling.id)

        return tilkjentYtelse.apply {
            this.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
            this.stønadTom = tilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
            this.stønadFom =
                if (erRentOpphør) null else tilkjentYtelse.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
            this.endretDato = LocalDate.now()
            this.opphørFom = opphørsdato?.toYearMonth()
        }
    }

    fun hentSisteOffsetPerIdent(
        fagsakId: Long,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory
    ): Map<String, Int> {
        val alleAndelerTilkjentYtelserIverksattMotØkonomi =
            hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(fagsakId)
                .flatMap { it.andelerTilkjentYtelse }
                .filter { it.erAndelSomSkalSendesTilOppdrag() }
                .pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)

        val alleTidligereKjederIverksattMotØkonomi =
            ØkonomiUtils.kjedeinndelteAndeler(alleAndelerTilkjentYtelserIverksattMotØkonomi)

        return ØkonomiUtils.gjeldendeForrigeOffsetForKjede(alleTidligereKjederIverksattMotØkonomi)
    }

    fun hentSisteOffsetPåFagsak(behandling: Behandling): Int? =
        behandlingRepository.finnIverksatteBehandlinger(behandling.fagsak.id)
            .filter { it.status == BehandlingStatus.AVSLUTTET }
            .mapNotNull { iverksattBehandling ->
                hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(iverksattBehandling.id)
                    .takeIf { it.isNotEmpty() }
                    ?.let { andelerTilkjentYtelse ->
                        andelerTilkjentYtelse.maxByOrNull { it.periodeOffset!! }?.periodeOffset?.toInt()
                    }
            }.maxByOrNull { it }

    /*
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

        val alleBarnISistIverksattBehandling = behandlingRepository.finnIverksatteBehandlinger(behandling.fagsak.id)
            .filter { it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }.maxByOrNull { it.opprettetTidspunkt }?.let {
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
