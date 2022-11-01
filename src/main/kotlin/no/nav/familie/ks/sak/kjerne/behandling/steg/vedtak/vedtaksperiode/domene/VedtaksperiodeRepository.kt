package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface VedtaksperiodeRepository : JpaRepository<VedtaksperiodeMedBegrunnelser, Long> {

    @Modifying
    @Query("DELETE FROM Vedtaksperiode v WHERE v.vedtak = :vedtak")
    fun slettVedtaksperioderFor(vedtak: Vedtak)

    @Query(value = "SELECT v FROM Vedtaksperiode v WHERE v.id = :vedtaksperiodeId")
    fun hentVedtaksperiode(vedtaksperiodeId: Long): VedtaksperiodeMedBegrunnelser?

    @Query("SELECT vp FROM Vedtaksperiode vp JOIN vp.vedtak v WHERE v.id = :vedtakId")
    fun finnVedtaksperioderFor(vedtakId: Long): List<VedtaksperiodeMedBegrunnelser>
}
