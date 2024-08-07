package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.brev.GenererBrevService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val genererBrevService: GenererBrevService,
) {
    fun hentVedtak(vedtakId: Long): Vedtak = vedtakRepository.hentVedtak(vedtakId)

    fun hentAktivVedtakForBehandling(behandlingId: Long): Vedtak =
        vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)
            ?: throw Feil("Fant ikke aktiv vedtak for behandling $behandlingId")

    fun oppdaterVedtak(vedtak: Vedtak) = vedtakRepository.saveAndFlush(vedtak)

    fun oppdaterVedtakMedDatoOgStønadsbrev(behandling: Behandling): Vedtak {
        val vedtak = hentAktivVedtakForBehandling(behandling.id)

        vedtak.vedtaksdato = LocalDateTime.now()
        if (behandling.skalSendeVedtaksbrev()) {
            val brev = genererBrevService.genererBrevForBehandling(vedtak)
            vedtak.stønadBrevPdf = brev
        }

        return oppdaterVedtak(vedtak)
    }

    fun opprettOgInitierNyttVedtakForBehandling(
        behandling: Behandling,
        kopierVedtakBegrunnelser: Boolean = false,
    ) {
        behandling.steg.takeUnless { it !== BehandlingSteg.BESLUTTE_VEDTAK && it !== BehandlingSteg.REGISTRERE_PERSONGRUNNLAG }
            ?: throw Feil("Forsøker å initiere vedtak på steg ${behandling.steg}")

        val deaktivertVedtak =
            vedtakRepository
                .findByBehandlingAndAktivOptional(behandlingId = behandling.id)
                ?.let { vedtakRepository.saveAndFlush(it.also { it.aktiv = false }) }

        val nyttVedtak = Vedtak(behandling = behandling)

        if (kopierVedtakBegrunnelser && deaktivertVedtak != null) {
            vedtaksperiodeService.kopierOverVedtaksperioder(
                deaktivertVedtak = deaktivertVedtak,
                aktivtVedtak = nyttVedtak,
            )
        }

        vedtakRepository.save(nyttVedtak)
    }
}
