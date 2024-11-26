package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpprettSammensattKontrollsakBrevDtoService(
    private val brevmalService: BrevmalService,
    private val opprettOpphørtSammensattKontrollsakDtoService: OpprettOpphørtSammensattKontrollsakDtoService,
    private val opprettOpphørMedEndringSammensattKontrollsakDtoService: OpprettOpphørMedEndringSammensattKontrollsakDtoService,
    private val opprettVedtakEndringSammensattKontrollsakDtoService: OpprettVedtakEndringSammensattKontrollsakDtoService,
) {
    private val logger = LoggerFactory.getLogger(OpprettSammensattKontrollsakBrevDtoService::class.java)

    fun opprett(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): BrevDto {
        logger.debug("Oppretter sammensatt kontrollsak ${BrevDto::class.simpleName} for vedtak ${vedtak.id}")
        return when (val brevmal = brevmalService.hentVedtaksbrevmal(behandling = vedtak.behandling)) {
            Brevmal.VEDTAK_OPPHØRT ->
                opprettOpphørtSammensattKontrollsakDtoService.opprett(
                    vedtak = vedtak,
                    sammensattKontrollsak = sammensattKontrollsak,
                )

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING ->
                opprettOpphørMedEndringSammensattKontrollsakDtoService.opprett(
                    vedtak = vedtak,
                    sammensattKontrollsak = sammensattKontrollsak,
                )

            Brevmal.VEDTAK_ENDRING ->
                opprettVedtakEndringSammensattKontrollsakDtoService.opprett(
                    vedtak = vedtak,
                    sammensattKontrollsak = sammensattKontrollsak,
                )

            Brevmal.INFORMASJONSBREV_LOVENDRING_JULI_2024,
            Brevmal.INFORMASJONSBREV_OVERGANGSORDNING_NOVEMBER_2024,
            Brevmal.INFORMASJONSBREV_DELT_BOSTED,
            Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER,
            Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_REVURDERING,
            Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER,
            Brevmal.INNHENTE_OPPLYSNINGER,
            Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            Brevmal.INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT,
            Brevmal.HENLEGGE_TRUKKET_SØKNAD,
            Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR,
            Brevmal.VARSEL_OM_REVURDERING,
            Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            Brevmal.VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT,
            Brevmal.SVARTIDSBREV,
            Brevmal.FORLENGET_SVARTIDSBREV,
            Brevmal.INFORMASJONSBREV_KAN_SØKE,
            Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS,
            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
            Brevmal.VEDTAK_AVSLAG,
            Brevmal.VEDTAK_FORTSATT_INNVILGET,
            Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV,
            Brevmal.VEDTAK_OPPHØR_DØDSFALL,
            Brevmal.VEDTAK_OVERGANGSORDNING,
            Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
            Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
            -> throw Feil("Brevmalen $brevmal er ikke støttet for sammensatte kontrollsaker")
        }
    }
}
