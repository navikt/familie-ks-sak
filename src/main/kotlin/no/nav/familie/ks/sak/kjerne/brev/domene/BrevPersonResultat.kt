package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat


/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class BrevPersonResultat(
    val personIdent: String,
    val brevVilkårResultater: List<BrevVilkårResultat>,
    val brevAnnenVurderinger: List<BrevAnnenVurdering>
)

data class BrevAnnenVurdering(
    val type: AnnenVurderingType,
    val resultat: Resultat
)

fun AnnenVurdering.tilBrevAnnenVurdering(): BrevAnnenVurdering {
    return BrevAnnenVurdering(type = this.type, resultat = this.resultat)
}

fun PersonResultat.tilBrevPersonResultat() =
    BrevPersonResultat(
        personIdent = this.aktør.aktivFødselsnummer(),
        brevVilkårResultater = this.vilkårResultater.map { it.tilBrevVilkårResultat() },
        brevAnnenVurderinger = this.andreVurderinger.map { it.tilBrevAnnenVurdering() }
    )

fun List<BrevPersonResultat>.harPersonerSomManglerOpplysninger(): Boolean =
    this.any { personResultat ->
        personResultat.brevAnnenVurderinger.any {
            it.type == AnnenVurderingType.OPPLYSNINGSPLIKT && it.resultat == Resultat.IKKE_OPPFYLT
        }
    }
