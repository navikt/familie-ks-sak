package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import no.nav.familie.ks.sak.common.exception.Feil
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

object LovverkTidslinjeGenerator {
    fun generer(
        barnasForskjøvedeVilkårResultater: Map<Aktør, Map<Vilkår, List<Periode<VilkårResultat>>>>,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ): Tidslinje<Lovverk> =
        barnasForskjøvedeVilkårResultater
            .map { entry ->
                entry.value.tilLovverkTidslinje(personopplysningGrunnlag.barna.single { it.aktør == entry.key })
            }.kombiner {
                val lovverk = it.toSet()
                if (lovverk.size > 1) {
                    throw Feil("Støtter ikke overlappende lovverk")
                }
                lovverk.single()
            }.tilPerioderIkkeNull()
            .sortedBy { it.fom }
            .erstattFørsteFomOgSisteTomMedNull()
            .kombinerEtterfølgendeElementer()
            .map { (lovverkPeriode, nesteLovverkPeriode) -> Periode(verdi = lovverkPeriode.verdi, fom = lovverkPeriode.fom, tom = nesteLovverkPeriode?.fom?.minusDays(1)) }
            .tilTidslinje()

    private fun Map<Vilkår, List<Periode<VilkårResultat>>>.tilLovverkTidslinje(barn: Person): Tidslinje<Lovverk> =
        // Konverterer alle VilkårResultatPerioder for alle barn til Lovverk-tidslinjer og kombinerer disse til en felles tidslinje.
        // Sørger for at vi kaster feil dersom det finnes mer enn ett lovverk i en periode.
        this.values
            .map { perioder ->
                perioder
                    .map { periode ->
                        Periode(
                            fom = periode.fom,
                            tom = periode.tom,
                            verdi =
                                LovverkUtleder.utledLovverkForBarn(
                                    barn.fødselsdato,
                                    skalBestemmeLovverkBasertPåFødselsdato = false,
                                ),
                        )
                    }.tilTidslinje()
                    .slåSammenLikePerioder()
            }.kombiner {
                it.toSet().single()
            }

    fun List<Periode<Lovverk>>.erstattFørsteFomOgSisteTomMedNull(): List<Periode<Lovverk>> =
        this.mapIndexed { index, periode ->
            when (index) {
                0 -> Periode(verdi = periode.verdi, null, tom = periode.tom)
                this.lastIndex -> Periode(verdi = periode.verdi, periode.fom, tom = null)
                else -> periode
            }
        }

    fun List<Periode<Lovverk>>.kombinerEtterfølgendeElementer(): List<Pair<Periode<Lovverk>, Periode<Lovverk>?>> {
        if (this.isEmpty()) return emptyList()

        return this.zipWithNext() + Pair(this.last(), null)
    }
}
