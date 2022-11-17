package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService {

    fun hentBrevperiodeDtoer(
        utvidetVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        vedtak: Vedtak
    ): List<BrevPeriodeDto> {
        val grunnlagForBrevperioder = hentGrunnlagForBrevperioder(
            vedtaksperioderId = utvidetVedtaksperioderMedBegrunnelser.map { it.id },
            behandlingId = vedtak.behandling.id
        )

        return grunnlagForBrevperioder
            .sorted()
            .mapNotNull {
                BrevPeriodeGenerator(it).genererBrevPeriode()
            }
    }

    fun hentGrunnlagForBrevperioder(
        vedtaksperioderId: List<Long>,
        behandlingId: Long
    ): List<GrunnlagForBrevperiode> {
        return emptyList()
    }
}
