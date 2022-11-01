package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode


import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeRepository
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeHentOgPersisterService(private val vedtaksperiodeRepository: VedtaksperiodeRepository) {

    fun hentVedtaksperiodeThrows(vedtaksperiodeId: Long): VedtaksperiodeMedBegrunnelser =
        vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)
            ?: throw Feil(
                message = "Fant ingen vedtaksperiode med id $vedtaksperiodeId",
                frontendFeilmelding = "Fant ikke vedtaksperiode"
            )

    fun lagre(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): VedtaksperiodeMedBegrunnelser {
        validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser)
        return vedtaksperiodeRepository.save(vedtaksperiodeMedBegrunnelser)
    }

    fun lagre(vedtaksperiodeMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>): List<VedtaksperiodeMedBegrunnelser> {
        vedtaksperiodeMedBegrunnelser.forEach { validerVedtaksperiodeMedBegrunnelser(it) }
        return vedtaksperiodeRepository.saveAll(vedtaksperiodeMedBegrunnelser)
    }

    fun slettVedtaksperioderFor(vedtak: Vedtak) {
        vedtaksperiodeRepository.slettVedtaksperioderFor(vedtak)
    }

    fun finnVedtaksperioderFor(vedtakId: Long): List<VedtaksperiodeMedBegrunnelser> =
        vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId)
}
