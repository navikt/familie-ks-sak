package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.sanity.domene.erOvergangsordningBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Component

@Component
class HjemmeltekstUtleder(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val sanityService: SanityService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val refusjonEøsService: RefusjonEøsService,
) {
    fun utledHjemmeltekst(
        behandlingId: Long,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean,
        utvidetVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
    ): String {
        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)

        val sanityBegrunnelser =
            utvidetVedtaksperioderMedBegrunnelser
                .flatMap<UtvidetVedtaksperiodeMedBegrunnelser, IBegrunnelse> { vedtaksperiode ->
                    vedtaksperiode.begrunnelser.map { it.nasjonalEllerFellesBegrunnelse } + vedtaksperiode.eøsBegrunnelser.map { it.begrunnelse }
                }.mapNotNull { it.tilSanityBegrunnelse(sanityService.hentSanityBegrunnelser()) }

        val alleHjemlerForBegrunnelser =
            kombinerHjemler(
<<<<<<< Updated upstream
=======
                ordinæreHjemler =
                    utledOrdinæreHjemler(
                        sanityBegrunnelser = sanityBegrunnelser,
                        opplysningspliktHjemlerSkalMedIBrev = vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT,
                    ),
>>>>>>> Stashed changes
                målform = personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandlingId),
                separasjonsavtaleStorbritanniaHjemler = utledSeprasjonsavtaleStorbritanniaHjemler(sanityBegrunnelser = sanityBegrunnelser),
                ordinæreHjemler = utledOrdinæreHjemler(sanityBegrunnelser = sanityBegrunnelser, opplysningspliktHjemlerSkalMedIBrev = !vilkårsvurdering.erOpplysningspliktVilkårOppfylt()),
                eøsForordningen883Hjemler = utledEØSForordningen883Hjemler(sanityBegrunnelser = sanityBegrunnelser),
<<<<<<< Updated upstream
                eøsForordningen987Hjemler = utledEØSForordningen987Hjemler(sanityBegrunnelser = sanityBegrunnelser, refusjonEøsHjemmelSkalMedIBrev = refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId)),
                forvaltningslovenHjemler = utledForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev),
=======
                eøsForordningen987Hjemler =
                    utledEØSForordningen987Hjemler(
                        sanityBegrunnelser = sanityBegrunnelser,
                        refusjonEøsHjemmelSkalMedIBrev = refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId),
                    ),
>>>>>>> Stashed changes
            )

        val hjemlerOgOvergangsordning =
            if (sanityBegrunnelser.any { it.erOvergangsordningBegrunnelse() }) {
                alleHjemlerForBegrunnelser + "forskrift om overgangsregler"
            } else {
                alleHjemlerForBegrunnelser
            }

        return slåSammenHjemlerAvUlikeTyper(hjemlerOgOvergangsordning)
    }

    private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) =
        when (hjemler.size) {
            0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. Du må velge minst én begrunnelse som er knyttet til en hjemmel.")
            1 -> hjemler.single()
            else -> slåSammenListeMedHjemler(hjemler)
        }

    private fun slåSammenListeMedHjemler(hjemler: List<String>): String =
        hjemler.reduceIndexed { index, acc, s ->
            when (index) {
                0 -> acc + s
                hjemler.size - 1 -> "$acc og $s"
                else -> "$acc, $s"
            }
        }
}
