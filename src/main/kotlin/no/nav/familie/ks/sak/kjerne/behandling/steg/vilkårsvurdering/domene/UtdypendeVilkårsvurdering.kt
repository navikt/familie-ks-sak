package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import no.nav.familie.ks.sak.common.util.konverterEnumsTilString
import no.nav.familie.ks.sak.common.util.konverterStringTilEnums
import javax.persistence.AttributeConverter
import javax.persistence.Converter

enum class UtdypendeVilkårsvurdering {
    VURDERING_ANNET_GRUNNLAG,
    DELT_BOSTED,
    ADOPSJON,
    SOMMERFERIE
}

@Converter
class UtdypendeVilkårsvurderingerConverter : AttributeConverter<List<UtdypendeVilkårsvurdering>, String> {

    override fun convertToDatabaseColumn(enumListe: List<UtdypendeVilkårsvurdering>) =
        konverterEnumsTilString(enumListe)

    override fun convertToEntityAttribute(string: String?): List<UtdypendeVilkårsvurdering> =
        konverterStringTilEnums(string)
}
