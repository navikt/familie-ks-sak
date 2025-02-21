package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class EndringstidspunktService(
    private val kompetanseService: KompetanseService,
    private val behandlingRepository: BehandlingRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val vilkårsvurderingService: VilkårsvurderingService,
) {
    fun finnEndringstidspunktForBehandling(behandling: Behandling): LocalDate {
        val behandlingId = behandling.id

        // Hvis det ikke finnes en forrige behandling vil vi ha med alt (derfor setter vi endringstidspunkt til tidenes morgen)
        val forrigeBehandling = behandlingRepository.finnBehandlinger(behandling.fagsak.id).filter { it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }.maxByOrNull { it.aktivertTidspunkt } ?: return TIDENES_MORGEN

        val endringstidspunktUtbetalingsbeløp = finnEndringstidspunktForBeløp(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val endringstidspunktKompetanse = finnEndringstidspunktForKompetanse(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val endringstidspunktVilkårsvurdering = finnEndringstidspunktForVilkårsvurdering(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val endringstidspunktEndretUtbetalingAndeler = finnEndringstidspunktForEndretUtbetalingAndel(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val tidligsteEndringstidspunkt =
            utledEndringstidspunkt(
                endringstidspunktUtbetalingsbeløp = endringstidspunktUtbetalingsbeløp,
                endringstidspunktKompetanse = endringstidspunktKompetanse,
                endringstidspunktVilkårsvurdering = endringstidspunktVilkårsvurdering,
                endringstidspunktEndretUtbetalingAndeler = endringstidspunktEndretUtbetalingAndeler,
            )

        return tidligsteEndringstidspunkt
    }

    private fun finnEndringstidspunktForBeløp(
        inneværendeBehandlingId: Long,
        forrigeBehandlingId: Long,
    ): YearMonth? {
        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = inneværendeBehandlingId)
        val forrigeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandlingId)

        return EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
        )
    }

    private fun finnEndringstidspunktForKompetanse(
        inneværendeBehandlingId: Long,
        forrigeBehandlingId: Long,
    ): YearMonth? {
        val nåværendeKompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(inneværendeBehandlingId)).toList()
        val forrigeKompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(forrigeBehandlingId)).toList()

        return EndringIKompetanseUtil.utledEndringstidspunktForKompetanse(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser,
        )
    }

    private fun finnEndringstidspunktForVilkårsvurdering(
        inneværendeBehandlingId: Long,
        forrigeBehandlingId: Long,
    ): YearMonth? {
        val nåværendeVilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = inneværendeBehandlingId) ?: return null
        val forrigeVilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = forrigeBehandlingId) ?: return null

        return EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = nåværendeVilkårsvurdering.personResultater,
            forrigePersonResultat = forrigeVilkårsvurdering.personResultater,
        )
    }

    private fun finnEndringstidspunktForEndretUtbetalingAndel(
        inneværendeBehandlingId: Long,
        forrigeBehandlingId: Long,
    ): YearMonth? {
        val nåværendeEndretAndeler = endretUtbetalingAndelService.hentEndredeUtbetalingAndeler(behandlingId = inneværendeBehandlingId)
        val forrigeEndretAndeler = endretUtbetalingAndelService.hentEndredeUtbetalingAndeler(behandlingId = forrigeBehandlingId)

        return EndringIEndretUtbetalingAndelUtil.utledEndringstidspunktForEndretUtbetalingAndel(
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            forrigeEndretAndeler = forrigeEndretAndeler,
        )
    }

    /**
     * Utleder første endringstidspunkt fra fire mulige datoer basert på første endring av:
     * - utbetaling
     * - kompetanse
     * - vilkårsvurdering
     * - endret utbetaling andel
     * Hvis det ikke er endring på feks. utbetaling blir den datoen null.
     * Hvis det ikke finnes noen endring i det hele tatt (dvs. alle er null) setter vi endringstidspunkt til tidenes ende
     * Dette er for at vi dermed kun skal få med vedtaksperioder som kun strekker seg uendelig frem i tid (feks. opphørsperiode)
     * * */
    private fun utledEndringstidspunkt(
        endringstidspunktUtbetalingsbeløp: YearMonth?,
        endringstidspunktKompetanse: YearMonth?,
        endringstidspunktVilkårsvurdering: YearMonth?,
        endringstidspunktEndretUtbetalingAndeler: YearMonth?,
    ): LocalDate =
        listOfNotNull(
            endringstidspunktUtbetalingsbeløp,
            endringstidspunktKompetanse,
            endringstidspunktVilkårsvurdering,
            endringstidspunktEndretUtbetalingAndeler,
        ).minOfOrNull { it }?.førsteDagIInneværendeMåned() ?: TIDENES_ENDE
}
