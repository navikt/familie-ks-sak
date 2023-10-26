package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.IKKE_FULLT_VURDERT
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.IKKE_OPPFYLT
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_NASJONALE_REGLER
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT

data class VilkårRegelverkResultat(
    val vilkår: Vilkår,
    val regelverkResultat: RegelverkResultat,
) {
    val resultat get() = regelverkResultat.resultat
    val regelverk get() = regelverkResultat.regelverk
}

fun VilkårRegelverkResultat.medRegelverk(regelverk: Regelverk) =
    VilkårRegelverkResultat(
        this.vilkår,
        RegelverkResultat.values().first { it.regelverk == regelverk && it.resultat == this.resultat },
    )

enum class RegelverkResultat(val regelverk: Regelverk?, val resultat: Resultat?) {
    OPPFYLT_EØS_FORORDNINGEN(Regelverk.EØS_FORORDNINGEN, Resultat.OPPFYLT),
    OPPFYLT_NASJONALE_REGLER(Regelverk.NASJONALE_REGLER, Resultat.OPPFYLT),
    OPPFYLT_REGELVERK_IKKE_SATT(null, Resultat.OPPFYLT),
    OPPFYLT_BLANDET_REGELVERK(null, Resultat.OPPFYLT),
    IKKE_OPPFYLT(null, Resultat.IKKE_OPPFYLT),
    IKKE_FULLT_VURDERT(null, Resultat.IKKE_VURDERT),
}

fun VilkårResultat.tilRegelverkResultat() =
    when (this.resultat) {
        Resultat.OPPFYLT, Resultat.IKKE_AKTUELT ->
            when (this.vurderesEtter) {
                Regelverk.EØS_FORORDNINGEN -> OPPFYLT_EØS_FORORDNINGEN
                Regelverk.NASJONALE_REGLER -> OPPFYLT_NASJONALE_REGLER
                null -> OPPFYLT_REGELVERK_IKKE_SATT
            }
        Resultat.IKKE_OPPFYLT -> IKKE_OPPFYLT
        Resultat.IKKE_VURDERT -> IKKE_FULLT_VURDERT
    }

fun RegelverkResultat?.kombinerMed(resultat: RegelverkResultat?) =
    when (this) {
        null ->
            when (resultat) {
                null -> null
                else -> IKKE_FULLT_VURDERT
            }
        OPPFYLT_EØS_FORORDNINGEN ->
            when (resultat) {
                null -> IKKE_FULLT_VURDERT
                OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_EØS_FORORDNINGEN
                OPPFYLT_NASJONALE_REGLER -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_BLANDET_REGELVERK
                IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
                IKKE_OPPFYLT -> IKKE_OPPFYLT
            }
        OPPFYLT_NASJONALE_REGLER ->
            when (resultat) {
                null -> IKKE_FULLT_VURDERT
                OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_NASJONALE_REGLER -> OPPFYLT_NASJONALE_REGLER
                OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_BLANDET_REGELVERK
                IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
                IKKE_OPPFYLT -> IKKE_OPPFYLT
            }
        OPPFYLT_BLANDET_REGELVERK ->
            when (resultat) {
                null -> IKKE_FULLT_VURDERT
                OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_NASJONALE_REGLER -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_BLANDET_REGELVERK
                IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
                IKKE_OPPFYLT -> IKKE_OPPFYLT
            }
        OPPFYLT_REGELVERK_IKKE_SATT ->
            when (resultat) {
                null -> IKKE_FULLT_VURDERT
                OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_NASJONALE_REGLER -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
                OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_REGELVERK_IKKE_SATT
                IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
                IKKE_OPPFYLT -> IKKE_OPPFYLT
            }
        IKKE_OPPFYLT -> IKKE_OPPFYLT
        IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
    }

fun VilkårRegelverkResultat?.erOppfylt() = this?.resultat == Resultat.OPPFYLT
