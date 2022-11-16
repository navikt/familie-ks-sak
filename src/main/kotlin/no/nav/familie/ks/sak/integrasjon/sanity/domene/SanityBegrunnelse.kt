package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class SanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkaar: List<Vilkår>? = null,
    val rolle: List<PersonType> = emptyList(),
    val endringsaarsaker: List<Årsak>? = null,
    val ovrigeTriggere: List<ØvrigTrigger>? = null,
    val lovligOppholdTriggere: List<VilkårTrigger>? = null,
    val bosattIRiketTriggere: List<VilkårTrigger>? = null,
    val borMedSokerTriggere: List<VilkårTrigger>? = null,
    val endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger>? = null,
    val endretUtbetalingsperiodeDeltBostedUtbetalingTrigger: EndretUtbetalingsperiodeDeltBostedTriggere? = null,
    val hjemler: List<String>
)

data class SanityBegrunnelserResponsDto(
    val ms: Int,
    val query: String,
    val result: List<SanityBegrunnelseDto>,
    val endringsaarsaker: List<String>? = emptyList()
)

// TODO: Har fjernet de fleste av feltene som brukes i ba-sak, så her må vi finne ut hvilke felter vi skal ha for KS
data class SanityBegrunnelseDto(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkaar: List<String>? = emptyList(),
    val rolle: List<String>? = emptyList(),
    val endringsaarsaker: List<String>? = emptyList()
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse {
        return SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            vilkaar = vilkaar?.mapNotNull {
                finnEnumverdi(it, Vilkår.values(), apiNavn)
            },
            rolle = rolle?.mapNotNull { finnEnumverdi(it, PersonType.values(), apiNavn) } ?: emptyList(),
            endringsaarsaker = endringsaarsaker?.mapNotNull {
                finnEnumverdi(it, Årsak.values(), apiNavn)
            },
            hjemler = emptyList() // TODO legg til hjemler
        )
    }
}

private val logger: Logger = LoggerFactory.getLogger(SanityBegrunnelseDto::class.java)

fun <T : Enum<T>> finnEnumverdi(verdi: String, enumverdier: Array<T>, apiNavn: String?): T? {
    val enumverdi = enumverdier.firstOrNull { verdi == it.name }
    if (enumverdi == null) {
        logger.error(
            "$verdi på begrunnelsen $apiNavn er ikke blant verdiene til enumen ${enumverdier.javaClass.simpleName}"
        )
    }
    return enumverdi
}

enum class VilkårRolle {
    SOKER,
    BARN
}
