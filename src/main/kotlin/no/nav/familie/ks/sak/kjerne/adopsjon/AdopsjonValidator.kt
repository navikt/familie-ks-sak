package no.nav.familie.ks.sak.kjerne.adopsjon

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Component

@Component
class AdopsjonValidator {
    fun validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(
        vilkårsvurdering: Vilkårsvurdering,
        adopsjonerIBehandling: List<Adopsjon>,
        støtterAdopsjonILøsningen: Boolean,
    ) {
        if (!støtterAdopsjonILøsningen) {
            return
        }
        vilkårsvurdering.personResultater.forEach { personResultat ->
            val adopsjonForPerson = adopsjonerIBehandling.firstOrNull { it.aktør == personResultat.aktør }

            personResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BARNETS_ALDER }.forEach {
                val erAdopsjon = it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.ADOPSJON)

                if (erAdopsjon && adopsjonForPerson == null) {
                    throw FunksjonellFeil(
                        melding = "Adopsjon er valgt i utdypende vilkårsvurdering, men det mangler adopsjonsdato for barn ${personResultat.aktør.aktivFødselsnummer()} i behandling ${vilkårsvurdering.behandling.id}",
                        frontendFeilmelding = "Du må legge til adopsjonsdato for barn ${personResultat.aktør.aktivFødselsnummer()} på 'barnets alder'-vilkåret eller fjerne adopsjon i utdypende vilkårsvurdering",
                    )
                }
                if (!erAdopsjon && adopsjonForPerson != null) {
                    throw FunksjonellFeil(
                        melding = "Adopsjon er ikke valgt i utdypende vilkårsvurdering, men det er lagret en adopsjonsdato for barn ${personResultat.aktør.aktivFødselsnummer()} i behandling ${vilkårsvurdering.behandling.id}",
                        frontendFeilmelding = "Du må fjerne adopsjonsdato for barn ${personResultat.aktør.aktivFødselsnummer()} på 'barnets alder'-vilkåret eller legge til adopsjon i utdypende vilkårsvurdering",
                    )
                }
            }
        }
    }
}
