package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val vedtaksperiodeService: VedtaksperiodeService
) {

    fun hentVedtak(vedtakId: Long): Vedtak = vedtakRepository.hentVedtak(vedtakId)

    fun hentAktivVedtakForBehandling(behandlingId: Long): Vedtak =
        vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)
            ?: throw Feil("Fant ikke aktiv vedtak for behandling $behandlingId")

    fun oppdaterVedtak(vedtak: Vedtak) =
        vedtakRepository.saveAndFlush(vedtak)

    fun opprettOgInitierNyttVedtakForBehandling(behandling: Behandling, kopierVedtakBegrunnelser: Boolean = false) {
        behandling.steg.takeUnless { it !== BehandlingSteg.BESLUTTE_VEDTAK && it !== BehandlingSteg.REGISTRERE_PERSONGRUNNLAG }
            ?: throw Feil("Forsøker å initiere vedtak på steg ${behandling.steg}")

        val deaktivertVedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandlingId = behandling.id)
            ?.let { vedtakRepository.saveAndFlush(it.also { it.aktiv = false }) }

        val nyttVedtak = Vedtak(behandling = behandling)

        if (kopierVedtakBegrunnelser && deaktivertVedtak != null) {
            vedtaksperiodeService.kopierOverVedtaksperioder(
                deaktivertVedtak = deaktivertVedtak,
                aktivtVedtak = nyttVedtak
            )
        }

        vedtakRepository.save(nyttVedtak)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VedtakService::class.java)
    }
}
