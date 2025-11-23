package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.familie.ks.sak.common.util.konverterEnumsTilString
import no.nav.familie.ks.sak.common.util.konverterStringTilEnums

enum class UtdypendeVilkårsvurdering {
    VURDERING_ANNET_GRUNNLAG,
    BOSATT_PÅ_SVALBARD,
    DELT_BOSTED,
    DELT_BOSTED_SKAL_IKKE_DELES,
    ADOPSJON,
    SOMMERFERIE,

    // EØS
    OMFATTET_AV_NORSK_LOVGIVNING, // Bosatt i riket SØKER vilkår
    OMFATTET_AV_NORSK_LOVGIVNING_UTLAND, // Bosatt i riket SØKER vilkår
    SØKER_OMFATTET_AV_UTENLANDSK_LOVGIVNING_BOSATT_I_NORGE, // Bosatt i riket SØKER vilkår
    BARN_BOR_I_NORGE, // Bosatt i riket BARN vilkår
    BARN_BOR_I_EØS, // Bosatt i riket BARN vilkår
    BARN_BOR_I_STORBRITANNIA, // Bosatt i riket BARN vilkår
    BARN_BOR_I_EØS_MED_SØKER, // Bor med søker vilkår
    BARN_BOR_I_EØS_MED_ANNEN_FORELDER, // Bor med søker vilkår
    BARN_BOR_I_NORGE_MED_SØKER, // Bor med søker vilkår
    BARN_BOR_I_STORBRITANNIA_MED_SØKER, // Bor med søker vilkår
    BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER, // Bor med søker vilkår
    BARN_BOR_ALENE_I_ANNET_EØS_LAND, // Bor med søker vilkår
    ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING,
}

@Converter
class UtdypendeVilkårsvurderingerConverter : AttributeConverter<List<UtdypendeVilkårsvurdering>, String> {
    override fun convertToDatabaseColumn(enumListe: List<UtdypendeVilkårsvurdering>) = konverterEnumsTilString(enumListe)

    override fun convertToEntityAttribute(string: String?): List<UtdypendeVilkårsvurdering> = konverterStringTilEnums(string)
}
