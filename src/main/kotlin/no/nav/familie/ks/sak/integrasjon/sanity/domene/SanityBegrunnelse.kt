package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.TriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType

import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class SanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkaar: List<Vilkår>? = null,
    val rolle: List<PersonType> = emptyList()
)

data class SanityBegrunnelserResponsDto(
    val ms: Int,
    val query: String,
    val result: List<SanityBegrunnelseDto>
)

// TODO: Har fjernet de fleste av feltene som brukes i ba-sak, så her må vi finne ut hvilke felter vi skal ha for KS
data class SanityBegrunnelseDto(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkaar: List<String>? = emptyList(),
    val rolle: List<String>? = emptyList()
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse {
        return SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            vilkaar = vilkaar?.mapNotNull {
                finnEnumverdi(it, Vilkår.values(), apiNavn)
            },
            rolle = rolle?.mapNotNull { finnEnumverdi(it, PersonType.values(), apiNavn) } ?: emptyList()
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

fun SanityBegrunnelse.tilTriggesAv(): TriggesAv {
    return TriggesAv(
        vilkår = this.vilkaar?.toSet() ?: emptySet(),
        personTyper = if (this.rolle.isEmpty()) {
            when {
                this.inneholderVilkår(Vilkår.BOSATT_I_RIKET) -> Vilkår.BOSATT_I_RIKET.parterDetteGjelderFor.toSet()
                this.inneholderVilkår(Vilkår.MEDLEMSKAP) -> Vilkår.MEDLEMSKAP.parterDetteGjelderFor.toSet()
                this.inneholderVilkår(Vilkår.BARNEHAGEPLASS) -> Vilkår.BARNEHAGEPLASS.parterDetteGjelderFor.toSet()
                this.inneholderVilkår(Vilkår.MEDLEMSKAP_ANNEN_FORELDER) -> Vilkår.MEDLEMSKAP_ANNEN_FORELDER.parterDetteGjelderFor.toSet()
                this.inneholderVilkår(Vilkår.BOR_MED_SØKER) -> Vilkår.BOR_MED_SØKER.parterDetteGjelderFor.toSet()
                this.inneholderVilkår(Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT) -> Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT.parterDetteGjelderFor.toSet()
                else -> setOf(PersonType.BARN, PersonType.SØKER)
            }
        } else {
            this.rolle.toSet()
        }
    )
}

fun SanityBegrunnelse.inneholderVilkår(vilkår: Vilkår) =
    this.vilkaar?.contains(vilkår) ?: false
