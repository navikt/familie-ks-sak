package no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType

enum class Vilkår(
    val parterDetteGjelderFor: List<PersonType>,
    val ytelseType: YtelseType,
    val beskrivelse: String,
    val harRegelverk: Boolean
) {
    BOSATT_I_RIKET(
        parterDetteGjelderFor = listOf(PersonType.SØKER, PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Bosatt i riket",
        true
    ),
    MEDLEMSSKAP(
        parterDetteGjelderFor = listOf(PersonType.SØKER),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Medlemskap",
        true
    ),
    BARNEHAGEPLASS(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Barnehageplass",
        false
    ),
    MEDLEMSKAP_ANNEN_FORELDER(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Medlemskap annen forelder",
        true
    ),
    BOR_MED_SØKER(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Bor fast hos søker",
        true
    ),
    MELLOM_1_OG_2_ELLER_ADOPTERT(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Mellom 1 og 2 år eller adoptert",
        false
    );

    companion object {
        fun hentVilkårFor(personType: PersonType): Set<Vilkår> {
            return values().filter {
                personType in it.parterDetteGjelderFor
            }.toSet()
        }
    }
}
