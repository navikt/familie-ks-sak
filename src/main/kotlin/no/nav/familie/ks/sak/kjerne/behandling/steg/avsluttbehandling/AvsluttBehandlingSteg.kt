package no.nav.familie.ks.sak.kjerne.behandling.steg.avsluttbehandling

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AvsluttBehandlingSteg(
    private val behandlingService: BehandlingService,
    private val loggService: LoggService,
    private val beregningService: BeregningService,
    private val fagsakService: FagsakService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.AVSLUTT_BEHANDLING

    @Transactional
    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
            throw Feil("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        // opprett historikk innslag
        loggService.opprettFerdigstillBehandling(behandling)

        // oppdater fagsak status
        if (behandling.resultat != Behandlingsresultat.AVSLÅTT) {
            oppdaterFagsakStatus(behandling)
        }

        // oppdater behandling status til Avsluttet
        behandling.status = BehandlingStatus.AVSLUTTET

        // trenger ikke å kalle eksplisitt save fordi behandling objekt er mutert og ha @Transactional
    }

    private fun oppdaterFagsakStatus(behandling: Behandling) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val erLøpende = tilkjentYtelse.andelerTilkjentYtelse.any { it.stønadTom >= inneværendeMåned() }
        if (erLøpende) {
            fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.LØPENDE)
        } else {
            fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AvsluttBehandlingSteg::class.java)
    }
}
