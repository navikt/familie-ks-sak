package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat

/**
 * Lager tidslinje av VilkårRegelverkResultat for ett vilkår og én aktør
 * For beregning er vi strengt tatt bare interessert i oppfylte vilkår, og her fjernes alle andre vilkårsresultater
 * Antakelsen er at IKKE_OPPFYLT/IKKE_AKTUELT i ALLE tilfeller kan ignoreres for beregning,
 * og evt bare brukes for info i brev
 * Løser problemet med BOR_MED_SØKER-vilkår som kan være oppfylt mens undervilkåret DELT_BOSTED ikke er oppfylt.
 * Ikke oppfylt DELT_BOSTED er løst funksjonelt ved at BOR_MED_SØKER settes til IKKE_OPPFYLT med fom og tom lik null.
 * fom og tom lik null tolkes som fra uendelig lenge siden til uendelig lenge til, som ville skapt overlapp med oppfylt vilkår
 * Overlapp er ikke støttet av tidsliner, og ville gitt exception
 */
fun Iterable<VilkårResultat>.tilVilkårRegelverkResultatTidslinje() = this.filter { it.erOppfylt() }
    .map { it.tilPeriode() }

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat> =
    Periode(
        fom = periodeFom?.førsteDagINesteMåned(),
        tom = periodeTom?.sisteDagIMåned(),
        verdi = VilkårRegelverkResultat(vilkårType, this.tilRegelverkResultat())
    )
