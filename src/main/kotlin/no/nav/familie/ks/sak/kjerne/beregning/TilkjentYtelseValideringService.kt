package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.finnAktørIderMedUgyldigEtterbetalingsperiode
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.springframework.stereotype.Service

@Service
class TilkjentYtelseValideringService(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val beregningService: BeregningService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val personidentService: PersonidentService,
    private val behandlingService: BehandlingService,
) {
    fun validerAtIngenUtbetalingerOverstiger100Prosent(behandling: Behandling) {
        if (behandling.erTekniskEndring()) return
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)

        if (totrinnskontroll.godkjent) {
            val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)

            val personopplysningGrunnlag =
                personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)

            val barnMedAndreRelevanteTilkjentYtelser =
                personopplysningGrunnlag.barna.map {
                    Pair(
                        it,
                        beregningService.hentRelevanteTilkjentYtelserForBarn(it.aktør, behandling.fagsak.id),
                    )
                }

            validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                tilkjentYtelseForBehandling = tilkjentYtelse,
                barnMedAndreRelevanteTilkjentYtelser = barnMedAndreRelevanteTilkjentYtelser,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        }
    }

    fun validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId: Long) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)

        if (tilkjentYtelse.harAndelerTilkjentYtelseMedSammeOffset()) {
            secureLogger.info("Behandling har flere andeler med likt offset: ${tilkjentYtelse.andelerTilkjentYtelse}")
            throw Feil("Behandling $behandlingId har andel tilkjent ytelse med offset lik en annen andel i behandlingen.")
        }
    }

    private fun TilkjentYtelse.harAndelerTilkjentYtelseMedSammeOffset(): Boolean {
        val periodeOffsetForAndeler = this.andelerTilkjentYtelse.mapNotNull { it.periodeOffset }

        return periodeOffsetForAndeler.size != periodeOffsetForAndeler.distinct().size
    }

    fun kontantstøtteLøperForAnnenForelder(
        behandling: Behandling,
        barna: List<Person>,
    ): Boolean =
        barna.any {
            beregningService
                .hentRelevanteTilkjentYtelserForBarn(barnAktør = it.aktør, fagsakId = behandling.fagsak.id)
                .isNotEmpty()
        }

    fun finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId: Long): List<Aktør> {
        val tilkjentYtelse = beregningService.finnTilkjentYtelseForBehandling(behandlingId = behandlingId) ?: return emptyList()

        val forrigeBehandling =
            behandlingService.hentSisteBehandlingSomErVedtatt(
                fagsakId = behandlingService.hentBehandling(behandlingId).fagsak.id,
            )
        val forrigeAndelerTilkjentYtelse =
            forrigeBehandling
                ?.let {
                    beregningService.hentTilkjentYtelseForBehandling(behandlingId = it.id)
                }?.andelerTilkjentYtelse
                ?.toList()

        val aktørIderMedUgyldigEtterbetaling =
            finnAktørIderMedUgyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                kravDato = tilkjentYtelse.behandling.opprettetTidspunkt,
            )

        return aktørIderMedUgyldigEtterbetaling.map { aktørId -> personidentService.hentAktør(identEllerAktørId = aktørId) }
    }
}
