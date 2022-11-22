package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

data class SanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkår: List<Vilkår>,
    val rolle: List<PersonType>,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val triggere: List<Trigger>,
    val hjemler: List<String>
)

data class SanityBegrunnelserResponsDto(
    val ms: Int,
    val query: String,
    val result: List<SanityBegrunnelseDto>,
    val endringsaarsaker: List<String>? = emptyList()
)

enum class Trigger {
    SATSENDRING,
    BARN_DØD,
    DELTID;

    fun erOppfylt(vilkårResultater: List<VilkårResultat>, person: Person) = when (this) {
        DELTID -> vilkårResultater.mapNotNull { it.antallTimer }.maxByOrNull { it }?.let {
            it in BigDecimal.valueOf(0.01)..BigDecimal.valueOf(
                32.99
            )
        } ?: false

        SATSENDRING -> false
        BARN_DØD -> person.erDød() && person.type == PersonType.BARN
    }
}

// TODO: Har fjernet de fleste av feltene som brukes i ba-sak, så her må vi finne ut hvilke felter vi skal ha for KS
data class SanityBegrunnelseDto(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkaar: List<String> = emptyList(),
    val rolle: List<String> = emptyList(),
    val utdypendeVilkaarsvurderinger: List<String> = emptyList(),
    val triggere: List<String> = emptyList(),
    val hjemler: List<String> = emptyList()
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse {
        return SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            vilkår = vilkaar.mapNotNull {
                finnEnumverdi(it, Vilkår.values(), apiNavn)
            },
            rolle = rolle.mapNotNull { finnEnumverdi(it, PersonType.values(), apiNavn) },
            utdypendeVilkårsvurderinger = utdypendeVilkaarsvurderinger.mapNotNull {
                finnEnumverdi(
                    it,
                    UtdypendeVilkårsvurdering.values(),
                    apiNavn
                )
            },
            triggere = triggere.mapNotNull { finnEnumverdi(it, Trigger.values(), apiNavn) },
            hjemler = hjemler
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
