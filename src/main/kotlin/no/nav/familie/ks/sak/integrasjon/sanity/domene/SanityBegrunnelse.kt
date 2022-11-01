package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.TriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertEndretAndel
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertVedtaksperiode
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
    val endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger>? = null,
    )

enum class EndretUtbetalingsperiodeTrigger {
    ETTER_ENDRET_UTBETALINGSPERIODE
}

enum class EndretUtbetalingsperiodeDeltBostedTriggere {
    SKAL_UTBETALES,
    SKAL_IKKE_UTBETALES,
    UTBETALING_IKKE_RELEVANT
}

enum class ØvrigTrigger {
    MANGLER_OPPLYSNINGER,
    SATSENDRING,
    BARN_MED_6_ÅRS_DAG,
    ALLTID_AUTOMATISK,
    ETTER_ENDRET_UTBETALING,
    ENDRET_UTBETALING,
    GJELDER_FØRSTE_PERIODE,
    GJELDER_FRA_INNVILGELSESTIDSPUNKT,
    BARN_DØD
}

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
    val endringsaarsaker: List<String>? = emptyList(),
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
        },
        endringsaarsaker = this.endringsaarsaker?.toSet() ?: emptySet(),
        satsendring = this.inneholderØvrigTrigger(ØvrigTrigger.SATSENDRING),
        valgbar = !this.inneholderØvrigTrigger(ØvrigTrigger.ALLTID_AUTOMATISK),
        etterEndretUtbetaling = this.endretUtbetalingsperiodeTriggere
            ?.contains(EndretUtbetalingsperiodeTrigger.ETTER_ENDRET_UTBETALINGSPERIODE) ?: false,


        )
}

fun SanityBegrunnelse.inneholderØvrigTrigger(øvrigTrigger: ØvrigTrigger) =
    this.ovrigeTriggere?.contains(øvrigTrigger) ?: false

fun SanityBegrunnelse.inneholderVilkår(vilkår: Vilkår) =
    this.vilkaar?.contains(vilkår) ?: false

private fun erEtterEndretPeriodeAvSammeÅrsak(
    endretUtbetalingAndeler: List<MinimertEndretAndel>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    aktuellePersoner: List<MinimertPerson>,
    triggesAv: TriggesAv
) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
    endretUtbetalingAndel.månedPeriode().tom.sisteDagIInneværendeMåned()
        .erDagenFør(minimertVedtaksperiode.fom) &&
            aktuellePersoner.any { person -> person.aktørId == endretUtbetalingAndel.aktørId } &&
            triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}