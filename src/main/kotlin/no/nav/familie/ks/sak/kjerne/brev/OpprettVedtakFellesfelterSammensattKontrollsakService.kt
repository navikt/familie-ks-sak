package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VedtakFellesfelterSammensattKontrollsak
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpprettVedtakFellesfelterSammensattKontrollsakService(
    private val opprettGrunnlagOgSignaturDataService: OpprettGrunnlagOgSignaturDataService,
    private val korrigertVedtakService: KorrigertVedtakService,
) {
    private val logger = LoggerFactory.getLogger(OpprettVedtakFellesfelterSammensattKontrollsakService::class.java)

    fun opprett(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): VedtakFellesfelterSammensattKontrollsak {
        logger.debug("Oppretter ${VedtakFellesfelterSammensattKontrollsak::class.simpleName} for vedtak ${vedtak.id}")
        val grunnlagOgSignaturData = opprettGrunnlagOgSignaturDataService.opprett(vedtak)
        return VedtakFellesfelterSammensattKontrollsak(
            enhet = grunnlagOgSignaturData.enhet,
            saksbehandler = grunnlagOgSignaturData.saksbehandler,
            beslutter = grunnlagOgSignaturData.beslutter,
            søkerNavn = grunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer =
                grunnlagOgSignaturData.grunnlag.søker.aktør
                    .aktivFødselsnummer(),
            korrigertVedtakData = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(vedtak.behandling.id)?.let { KorrigertVedtakData(datoKorrigertVedtak = it.vedtaksdato.tilDagMånedÅr()) },
            sammensattKontrollsakFritekst = sammensattKontrollsak.fritekst,
        )
    }
}
