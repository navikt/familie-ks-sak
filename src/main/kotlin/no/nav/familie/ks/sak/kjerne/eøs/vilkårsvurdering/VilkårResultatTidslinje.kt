package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.forskyvVilkårResultater
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje

/**
 * Lager tidslinje av VilkårRegelverkResultat for ett vilkår og én aktør
 * For beregning er vi strengt tatt bare interessert i OPPFYLT/IKKE_AKTUELT vilkår, og her fjernes alle andre vilkårsresultater
 * Antakelsen er at IKKE_OPPFYLT i ALLE tilfeller kan ignoreres for beregning,
 * og evt bare brukes for info i brev
 * Løser problemet med BOR_MED_SØKER-vilkår som kan være oppfylt mens undervilkåret DELT_BOSTED ikke er oppfylt.
 * Ikke oppfylt DELT_BOSTED er løst funksjonelt ved at BOR_MED_SØKER settes til IKKE_OPPFYLT med fom og tom lik null.
 * fom og tom lik null tolkes som fra uendelig lenge siden til uendelig lenge til, som ville skapt overlapp med oppfylt vilkår
 * Overlapp er ikke støttet av tidsliner, og ville gitt exception
 */
fun tilVilkårRegelverkResultatTidslinje(
    vilkår: Vilkår,
    alleVilkårResultater: List<VilkårResultat>,
): Tidslinje<VilkårRegelverkResultat> {
    val oppfyltEllerIkkeAktueltVilkårer = alleVilkårResultater.filter { it.erOppfylt() || it.erIkkeAktuelt() }

    val forskjøvetVilkårResultatPerioder = forskyvVilkårResultater(vilkår, oppfyltEllerIkkeAktueltVilkårer)

    return forskjøvetVilkårResultatPerioder.map { it.tilVilkårRegelverkResultatPeriode() }.tilTidslinje()
}

fun Periode<VilkårResultat>.tilVilkårRegelverkResultatPeriode(): Periode<VilkårRegelverkResultat> {
    val vilkårResultat = this.verdi

    return Periode(
        fom = this.fom,
        tom = this.tom,
        verdi = VilkårRegelverkResultat(vilkårResultat.vilkårType, vilkårResultat.tilRegelverkResultat(), utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger),
    )
}
