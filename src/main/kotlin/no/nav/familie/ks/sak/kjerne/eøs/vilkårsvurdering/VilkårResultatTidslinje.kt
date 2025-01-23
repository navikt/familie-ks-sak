package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.forskyvVilkårResultater
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje

/**
 * Lager tidslinje av VilkårRegelverkResultat for ett vilkår og én aktør
 * For beregning er vi strengt tatt bare interessert i OPPFYLT/IKKE_AKTUELT vilkår, og her fjernes alle andre vilkårsresultater
 * Antakelsen er at IKKE_OPPFYLT i ALLE tilfeller kan ignoreres for beregning,
 * og evt bare brukes for info i brev
 */
fun PersonResultat.tilVilkårRegelverkResultatTidslinje(): List<Tidslinje<VilkårRegelverkResultat>> =
    this
        .forskyvVilkårResultater()
        .values
        .map {
            it
                .filter { periode -> periode.verdi.erOppfylt() || periode.verdi.erIkkeAktuelt() }
                .map { periode -> periode.tilVilkårRegelverkResultatPeriode() }
                .tilTidslinje()
        }

fun Periode<VilkårResultat>.tilVilkårRegelverkResultatPeriode(): Periode<VilkårRegelverkResultat> {
    val vilkårResultat = this.verdi

    return Periode(
        fom = this.fom,
        tom = this.tom,
        verdi = VilkårRegelverkResultat(vilkårResultat.vilkårType, vilkårResultat.tilRegelverkResultat(), utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger),
    )
}
