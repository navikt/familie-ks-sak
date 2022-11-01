package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat


/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class MinimertRestPersonResultat(
    val personIdent: String,
    val minimerteVilkårResultater: List<MinimertVilkårResultat>,
    val minimerteAndreVurderinger: List<MinimertAnnenVurdering>
)

data class MinimertAnnenVurdering(
    val type: AnnenVurderingType,
    val resultat: Resultat
)

fun AnnenVurdering.tilMinimertAnnenVurdering(): MinimertAnnenVurdering {
    return MinimertAnnenVurdering(type = this.type, resultat = this.resultat)
}

fun PersonResultat.tilMinimertPersonResultat() =
    MinimertRestPersonResultat(
        personIdent = this.aktør.aktivFødselsnummer(),
        minimerteVilkårResultater = this.vilkårResultater.map { it.tilMinimertVilkårResultat() },
        minimerteAndreVurderinger = this.andreVurderinger.map { it.tilMinimertAnnenVurdering() }
    )

fun List<MinimertRestPersonResultat>.harPersonerSomManglerOpplysninger(): Boolean =
    this.any { personResultat ->
        personResultat.minimerteAndreVurderinger.any {
            it.type == AnnenVurderingType.OPPLYSNINGSPLIKT && it.resultat == Resultat.IKKE_OPPFYLT
        }
    }
