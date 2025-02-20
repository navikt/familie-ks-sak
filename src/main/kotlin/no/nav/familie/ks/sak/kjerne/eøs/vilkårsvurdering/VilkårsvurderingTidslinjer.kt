package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.inneholder
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.LocalDate

class VilkårsvurderingTidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
) {
    private val barnasTidslinjer: Map<Person, BarnetsTidslinjer> =
        personopplysningGrunnlag.barna.associateWith { barn ->
            BarnetsTidslinjer(
                barn = barn,
                adopsjonsdato = adopsjonerIBehandling.firstOrNull { it.aktør == barn.aktør }?.adopsjonsdato,
                personResultater = vilkårsvurdering.personResultater,
            )
        }

    fun barnasTidslinjer(): Map<Aktør, BarnetsTidslinjer> = barnasTidslinjer.entries.associate { it.key.aktør to it.value }

    class BarnetsTidslinjer(
        barn: Person,
        adopsjonsdato: LocalDate?,
        personResultater: Set<PersonResultat>,
    ) {
        private val lovverk =
            LovverkUtleder.utledLovverkForBarn(
                fødselsdato = barn.fødselsdato,
                adopsjonsdato = adopsjonsdato,
            )
        private val søkersTidslinje = personResultater.single { it.erSøkersResultater() }.tilVilkårRegelverkResultatTidslinje(lovverk = lovverk)
        private val barnetsTidslinje = personResultater.single { it.aktør == barn.aktør }.tilVilkårRegelverkResultatTidslinje(lovverk = lovverk)

        val barnetsRegelverkResultatTidslinje =
            barnetsTidslinje.kombiner {
                kombinerVilkårResultaterTilRegelverkResultat(PersonType.BARN, it)
            }

        val søkersRegelverkResultatTidslinje =
            søkersTidslinje.kombiner {
                kombinerVilkårResultaterTilRegelverkResultat(PersonType.SØKER, it)
            }
        val kombinertRegelverkResultatTidslinje =
            barnetsRegelverkResultatTidslinje
                .kombinerMed(søkersRegelverkResultatTidslinje) { barnetsResultat, søkersResultat ->
                    barnetsResultat.kombinerMed(søkersResultat)
                }.beskjærEtter(søkersRegelverkResultatTidslinje)
    }

    fun harBlandetRegelverk(): Boolean =
        barnasTidslinjer().values.any {
            it.barnetsRegelverkResultatTidslinje.inneholder(RegelverkResultat.OPPFYLT_BLANDET_REGELVERK) ||
                it.søkersRegelverkResultatTidslinje.inneholder(RegelverkResultat.OPPFYLT_BLANDET_REGELVERK)
        }
}
