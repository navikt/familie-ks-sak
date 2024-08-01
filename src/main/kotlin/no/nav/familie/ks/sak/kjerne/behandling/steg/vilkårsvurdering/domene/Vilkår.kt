package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType

enum class Vilkår(
    val parterDetteGjelderFor: List<PersonType>,
    val ytelseType: YtelseType,
    val beskrivelse: String,
    val harRegelverk: Boolean,
    val eøsSpesifikt: Boolean,
) {
    BOSATT_I_RIKET(
        parterDetteGjelderFor = listOf(PersonType.SØKER, PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Bosatt i riket",
        harRegelverk = true,
        eøsSpesifikt = false,
    ),
    LOVLIG_OPPHOLD(
        parterDetteGjelderFor = listOf(PersonType.SØKER),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Lovlig opphold",
        harRegelverk = true,
        eøsSpesifikt = true,
    ),
    MEDLEMSKAP(
        parterDetteGjelderFor = listOf(PersonType.SØKER),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Medlemskap",
        harRegelverk = true,
        eøsSpesifikt = false,
    ),
    BARNEHAGEPLASS(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Barnehageplass",
        harRegelverk = false,
        eøsSpesifikt = false,
    ),
    MEDLEMSKAP_ANNEN_FORELDER(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Medlemskap annen forelder",
        harRegelverk = true,
        eøsSpesifikt = false,
    ),
    BOR_MED_SØKER(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Bor fast hos søker",
        harRegelverk = true,
        eøsSpesifikt = false,
    ),
    BARNETS_ALDER(
        parterDetteGjelderFor = listOf(PersonType.BARN),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        beskrivelse = "Mellom 1 og 2 år eller adoptert",
        harRegelverk = false,
        eøsSpesifikt = false,
    ),
    ;

    companion object {
        fun hentVilkårFor(
            personType: PersonType,
            skalHenteEøsSpesifikkeVilkår: Boolean = false,
        ): Set<Vilkår> =
            entries.filter {
                personType in it.parterDetteGjelderFor && (skalHenteEøsSpesifikkeVilkår || !it.eøsSpesifikt)
            }.toSet()
    }

    fun defaultRegelverk(behandlingKategori: BehandlingKategori): Regelverk? {
        if (this.harRegelverk) {
            return if (behandlingKategori == BehandlingKategori.EØS) {
                Regelverk.EØS_FORORDNINGEN
            } else {
                Regelverk.NASJONALE_REGLER
            }
        }
        return null
    }
}
