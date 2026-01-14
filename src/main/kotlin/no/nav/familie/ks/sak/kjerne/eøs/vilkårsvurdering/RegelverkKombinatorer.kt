package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.IKKE_FULLT_VURDERT
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_NASJONALE_REGLER
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType

fun kombinerVilkårResultaterTilRegelverkResultat(
    personType: PersonType,
    // vilkår resultater som er OPPFYLT/IKKE_AKTUELT
    alleVilkårResultater: Iterable<VilkårRegelverkResultat>,
): RegelverkResultat? {
    val skalHenteEøsSpesifikkeVilkår = alleVilkårResultater.any { it.regelverk == Regelverk.EØS_FORORDNINGEN && it.vilkår == Vilkår.BOSATT_I_RIKET }
    val vilkårer = Vilkår.hentVilkårFor(personType, skalHenteEøsSpesifikkeVilkår)

    val regelverkVilkår = vilkårer.filter { it.harRegelverk }

    val alleEøsVilkårResultater =
        alleVilkårResultater
            .filter { it.regelverk == Regelverk.EØS_FORORDNINGEN }
            .map { it.vilkår }

    val alleNasjonaleVilkårResultater =
        alleVilkårResultater
            .filter { it.regelverk == Regelverk.NASJONALE_REGLER }
            .map { it.vilkår }

    val erAlleVilkårUtenResultat = alleVilkårResultater.all { it.resultat == null }

    val erAlleVilkårOppfyltEllerIkkeAktuelt = alleVilkårResultater.map { it.vilkår }.distinct().containsAll(vilkårer)

    return when {
        erAlleVilkårUtenResultat -> {
            null
        }

        erAlleVilkårOppfyltEllerIkkeAktuelt -> {
            when {
                alleEøsVilkårResultater.containsAll(regelverkVilkår) -> {
                    OPPFYLT_EØS_FORORDNINGEN
                }

                alleNasjonaleVilkårResultater.containsAll(regelverkVilkår) -> {
                    OPPFYLT_NASJONALE_REGLER
                }

                (alleEøsVilkårResultater + alleNasjonaleVilkårResultater).isNotEmpty() -> {
                    OPPFYLT_BLANDET_REGELVERK
                }

                else -> {
                    OPPFYLT_REGELVERK_IKKE_SATT
                }
            }
        }

        else -> {
            IKKE_FULLT_VURDERT
        }
    }
}
