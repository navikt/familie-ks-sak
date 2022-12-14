package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

data class SanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String,
    val type: SanityBegrunnelseType,
    val vilkår: List<Vilkår>,
    val rolle: List<VilkårRolle>,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val triggere: List<Trigger>,
    val hjemler: List<String>,
    val endringsaarsaker: List<Årsak>,
    val endretUtbetalingsperiode: List<EndretUtbetalingsperiodeTrigger>
)

enum class SanityBegrunnelseType {
    STANDARD,
    TILLEGGSTEKST,
    ENDRINGSPERIODE
}

data class SanityBegrunnelserResponsDto(
    val ms: Int,
    val query: String,
    val result: List<SanityBegrunnelseDto>,
    val endringsaarsaker: List<String>? = emptyList()
)

enum class Trigger {
    SATSENDRING,
    BARN_DØD,
    DELTID_BARNEHAGEPLASS;

    fun erOppfylt(vilkårResultater: List<VilkårResultat>, person: Person) = when (this) {
        DELTID_BARNEHAGEPLASS -> vilkårResultater.mapNotNull { it.antallTimer }.maxByOrNull { it }?.let {
            it in BigDecimal.valueOf(0.01)..BigDecimal.valueOf(
                32.99
            )
        } ?: false

        SATSENDRING -> false
        BARN_DØD -> person.erDød() && person.type == PersonType.BARN
    }
}

data class SanityBegrunnelseDto(
    val apiNavn: String?,
    val navnISystem: String,
    val type: String,
    val vilkaar: List<String> = emptyList(),
    val rolle: List<String> = emptyList(),
    val utdypendeVilkaarsvurderinger: List<String> = emptyList(),
    val endringsaarsaker: List<String> = emptyList(),
    val endretUtbetalingsperiode: List<String> = emptyList(),
    val triggere: List<String> = emptyList(),
    val hjemler: List<String> = emptyList()
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse {
        return SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            type = finnEnumverdi(type, SanityBegrunnelseType.values(), apiNavn) ?: SanityBegrunnelseType.TILLEGGSTEKST,
            vilkår = vilkaar.mapNotNull {
                finnEnumverdi(it, Vilkår.values(), apiNavn)
            },
            rolle = rolle.mapNotNull { finnEnumverdi(it, VilkårRolle.values(), apiNavn) },
            utdypendeVilkårsvurderinger = utdypendeVilkaarsvurderinger.mapNotNull {
                finnEnumverdi(
                    it,
                    UtdypendeVilkårsvurdering.values(),
                    apiNavn
                )
            },
            triggere = triggere.mapNotNull { finnEnumverdi(it, Trigger.values(), apiNavn) },
            hjemler = hjemler,
            endringsaarsaker = endringsaarsaker.mapNotNull { finnEnumverdi(it, Årsak.values(), apiNavn) },
            endretUtbetalingsperiode = endretUtbetalingsperiode.mapNotNull {
                finnEnumverdi(it, EndretUtbetalingsperiodeTrigger.values(), apiNavn)
            }
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

enum class EndretUtbetalingsperiodeTrigger {
    ETTER_ENDRET_UTBETALINGSPERIODE
}
