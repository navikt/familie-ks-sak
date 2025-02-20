package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate

object LovverkTidslinjeGenerator {
    fun generer(
        barnasForskjøvedeVilkårResultater: Map<Aktør, Map<Vilkår, List<Periode<VilkårResultat>>>>,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        adopsjonerIBehandling: List<Adopsjon>,
    ): Tidslinje<Lovverk> =
        barnasForskjøvedeVilkårResultater
            .map { (aktør, forskjøvedeVilkårResultater) ->
                // Konverterer forskjøvede VilkårResultater til Lovverk-tidslinje per barn
                forskjøvedeVilkårResultater.tilLovverkTidslinje(
                    barn = personopplysningGrunnlag.barna.single { it.aktør == aktør },
                    adopsjonsdato = adopsjonerIBehandling.firstOrNull { it.aktør == aktør }?.adopsjonsdato,
                )
            }
            // Kombinerer alle Lovverk-tidslinjer til en felles Lovverk-tidslinje.
            // Lovverk-tidslinjene kan overlappe, men lovverket i de overlappende periodene må være det samme.
            .kombiner {
                val lovverkIPeriode = it.toSet()
                if (lovverkIPeriode.size > 1) {
                    throw Feil("Støtter ikke overlappende lovverk")
                }
                lovverkIPeriode.single()
            }.tilPerioderIkkeNull()
            .sortedBy { it.fom }
            .erstattFørsteFomOgSisteTomMedNull()
            .kombinerEtterfølgendeElementer()
            // Sørger for at et lovverk strekker seg helt frem til neste startdato for neste lovverk
            .map { (lovverkPeriode, nesteLovverkPeriode) -> Periode(verdi = lovverkPeriode.verdi, fom = lovverkPeriode.fom, tom = nesteLovverkPeriode?.fom?.minusDays(1)) }
            .tilTidslinje()

    private fun Map<Vilkår, List<Periode<VilkårResultat>>>.tilLovverkTidslinje(
        barn: Person,
        adopsjonsdato: LocalDate?,
    ): Tidslinje<Lovverk> {
        val lovverkForBarn =
            LovverkUtleder.utledLovverkForBarn(
                fødselsdato = barn.fødselsdato,
                adopsjonsdato = adopsjonsdato,
            )
        return this
            .getOrElse(Vilkår.BARNETS_ALDER) { throw Feil("Finner ikke vilkår for barnets alder") }
            .map { periode ->
                Periode(
                    fom = periode.fom,
                    tom = periode.tom,
                    verdi = lovverkForBarn,
                )
            }.tilTidslinje()
            .slåSammenLikePerioder()
    }

    private fun List<Periode<Lovverk>>.erstattFørsteFomOgSisteTomMedNull(): List<Periode<Lovverk>> =
        // Sørger for at lovverk-tidslinje strekker seg fra TIDENES_MORGEN til TIDENES_ENDE.
        // Da er vi sikre på at tidslinja dekker søkers vilkår.
        this.mapIndexed { index, periode ->
            when (index) {
                0 -> Periode(verdi = periode.verdi, fom = null, tom = periode.tom)
                this.lastIndex -> Periode(verdi = periode.verdi, fom = periode.fom, tom = null)
                else -> periode
            }
        }

    private fun List<Periode<Lovverk>>.kombinerEtterfølgendeElementer(): List<Pair<Periode<Lovverk>, Periode<Lovverk>?>> {
        if (this.isEmpty()) return emptyList()

        return this.zipWithNext() + Pair(this.last(), null)
    }
}
