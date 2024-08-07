package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse

sealed interface IBegrunnelse {
    val sanityApiNavn: String
    val begrunnelseType: BegrunnelseType

    fun enumnavnTilString(): String

    fun støtterFritekst(sanityBegrunnelser: List<SanityBegrunnelse>) =
        sanityBegrunnelser.firstOrNull { it.apiNavn == this.sanityApiNavn }?.støtterFritekst == true ||
            (this.begrunnelseType == BegrunnelseType.REDUKSJON && this !== NasjonalEllerFellesBegrunnelse.REDUKSJON_SATSENDRING) // Alle reduksjons-begrunnelser med unntak av REDUKSJON_SATSENDRING støtter fritekst

    companion object {
        fun konverterTilEnumVerdi(string: String): IBegrunnelse {
            val splittet = string.split('$')
            val type = splittet[0]
            val enumNavn = splittet[1]
            return when (type) {
                EØSBegrunnelse::class.simpleName -> EØSBegrunnelse.valueOf(enumNavn)
                NasjonalEllerFellesBegrunnelse::class.simpleName -> NasjonalEllerFellesBegrunnelse.valueOf(enumNavn)
                else -> throw Feil("Fikk en begrunnelse med ugyldig type: hverken EØSBegrunnelse eller NasjonalEllerFellesBegrunnelse: $this")
            }
        }
    }
}

class IBegrunnelseDeserializer : StdDeserializer<List<IBegrunnelse>>(List::class.java) {
    override fun deserialize(
        jsonParser: JsonParser?,
        p1: DeserializationContext?,
    ): List<IBegrunnelse> {
        val node: ArrayNode = jsonParser!!.codec.readTree(jsonParser)
        return node
            .map { it.asText() }
            .map { IBegrunnelse.konverterTilEnumVerdi(it) }
    }
}

@Converter
class IBegrunnelseListConverter : AttributeConverter<List<IBegrunnelse>, String> {
    override fun convertToDatabaseColumn(begrunnelser: List<IBegrunnelse>) = begrunnelser.joinToString(";") { it.enumnavnTilString() }

    override fun convertToEntityAttribute(string: String?): List<IBegrunnelse> =
        if (string.isNullOrBlank()) {
            emptyList()
        } else {
            string
                .split(";")
                .map { IBegrunnelse.konverterTilEnumVerdi(it) }
        }
}
