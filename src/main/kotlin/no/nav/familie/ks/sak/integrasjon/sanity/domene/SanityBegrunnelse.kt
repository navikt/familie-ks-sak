package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.enums.EnumEntries

data class SanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String,
    val type: SanityBegrunnelseType,
    val vilkår: List<Vilkår>,
    val rolle: List<VilkårRolle>,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val triggere: List<Trigger>,
    val hjemler: List<String>,
    val endringsårsaker: List<Årsak>,
    val endretUtbetalingsperiode: List<EndretUtbetalingsperiodeTrigger>,
    val støtterFritekst: Boolean,
    val skalAlltidVises: Boolean,
    val ikkeIBruk: Boolean,
    // EØS
    val annenForeldersAktivitet: List<KompetanseAktivitet> = emptyList(),
    val barnetsBostedsland: List<BarnetsBostedsland> = emptyList(),
    val kompetanseResultat: List<KompetanseResultat> = emptyList(),
    val hjemlerFolketrygdloven: List<String> = emptyList(),
    val hjemlerEØSForordningen883: List<String> = emptyList(),
    val hjemlerEØSForordningen987: List<String> = emptyList(),
    val hjemlerSeperasjonsavtalenStorbritannina: List<String> = emptyList(),
    val resultat: SanityResultat,
)

fun SanityBegrunnelse.erOvergangsordningBegrunnelse() =
    apiNavn == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING.sanityApiNavn ||
        apiNavn == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_DELT_BOSTED.sanityApiNavn ||
        apiNavn == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING.sanityApiNavn ||
        apiNavn == NasjonalEllerFellesBegrunnelse.OPPHØR_OVERGANGSORDNING_OPPHØR.sanityApiNavn

enum class SanityBegrunnelseType {
    STANDARD,
    TILLEGGSTEKST,
    ENDRINGSPERIODE,
}

enum class BarnetsBostedsland {
    NORGE,
    IKKE_NORGE,
}

fun landkodeTilBarnetsBostedsland(landkode: String): BarnetsBostedsland =
    when (landkode) {
        "NO" -> BarnetsBostedsland.NORGE
        else -> BarnetsBostedsland.IKKE_NORGE
    }

data class SanityBegrunnelserResponsDto(
    val ms: Int,
    val query: String,
    val result: List<SanityBegrunnelseDto>,
    val endringsaarsaker: List<String>? = emptyList(),
)

enum class Trigger {
    SATSENDRING,
    BARN_DØD,
    DELTID_BARNEHAGEPLASS,
    GJELDER_FØRSTE_PERIODE,
    ;

    fun erOppfylt(
        vilkårResultater: List<VilkårResultat>,
        person: Person,
        erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger: Boolean,
    ) = when (this) {
        DELTID_BARNEHAGEPLASS -> {
            vilkårResultater.mapNotNull { it.antallTimer }.maxByOrNull { it }?.let {
                @Suppress("ktlint:standard:multiline-expression-wrapping")
                it in BigDecimal.valueOf(0.01)..BigDecimal.valueOf(
                    32.99,
                )
            } ?: false
        }

        SATSENDRING -> {
            false
        }

        GJELDER_FØRSTE_PERIODE -> {
            vilkårResultater.isNotEmpty() && erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger
        }

        BARN_DØD -> {
            person.erDød() && person.type == PersonType.BARN
        }
    }
}

fun SanityBegrunnelse.begrunnelseGjelderOpphørFraForrigeBehandling() = Trigger.GJELDER_FØRSTE_PERIODE in this.triggere

enum class SanityResultat {
    INNVILGET,
    REDUKSJON,
    AVSLAG,
    OPPHØR,
    FORTSATT_INNVILGET,
    ENDRET_UTBETALING,
    ETTER_ENDRET_UTBETALINGSPERIODE,
}

data class SanityBegrunnelseDto(
    val apiNavn: String?,
    val navnISystem: String,
    val type: String,
    val resultat: SanityResultat,
    val vilkaar: List<String> = emptyList(),
    val eosVilkaar: List<String> = emptyList(),
    val rolle: List<String> = emptyList(),
    val utdypendeVilkaarsvurderinger: List<String> = emptyList(),
    val endringsaarsaker: List<String> = emptyList(),
    val endretUtbetalingsperiode: List<String> = emptyList(),
    val triggere: List<String> = emptyList(),
    val hjemler: List<String> = emptyList(),
    val stotterFritekst: Boolean?,
    val skalAlltidVises: Boolean?,
    val ikkeIBruk: Boolean?,
    val annenForeldersAktivitet: List<String> = emptyList(),
    val barnetsBostedsland: List<String> = emptyList(),
    val kompetanseResultat: List<String> = emptyList(),
    val hjemlerFolketrygdloven: List<String> = emptyList(),
    val hjemlerEOSForordningen883: List<String> = emptyList(),
    val hjemlerEOSForordningen987: List<String> = emptyList(),
    val hjemlerSeperasjonsavtalenStorbritannina: List<String> = emptyList(),
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse =
        SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            resultat = resultat,
            type = finnEnumverdi(type, SanityBegrunnelseType.entries, apiNavn) ?: SanityBegrunnelseType.TILLEGGSTEKST,
            vilkår =
                (vilkaar + eosVilkaar).mapNotNull {
                    finnEnumverdi(it, Vilkår.entries, apiNavn)
                },
            rolle = rolle.mapNotNull { finnEnumverdi(it, VilkårRolle.entries, apiNavn) },
            utdypendeVilkårsvurderinger =
                utdypendeVilkaarsvurderinger.mapNotNull {
                    finnEnumverdi(
                        it,
                        UtdypendeVilkårsvurdering.entries,
                        apiNavn,
                    )
                },
            triggere = triggere.mapNotNull { finnEnumverdi(it, Trigger.entries, apiNavn) },
            hjemler = hjemler,
            endringsårsaker = endringsaarsaker.mapNotNull { finnEnumverdi(it, Årsak.entries, apiNavn) },
            endretUtbetalingsperiode =
                endretUtbetalingsperiode.mapNotNull {
                    finnEnumverdi(it, EndretUtbetalingsperiodeTrigger.entries, apiNavn)
                },
            støtterFritekst = stotterFritekst ?: false,
            skalAlltidVises = skalAlltidVises ?: false,
            ikkeIBruk = ikkeIBruk ?: false,
            annenForeldersAktivitet = annenForeldersAktivitet.mapNotNull { finnEnumverdi(it, KompetanseAktivitet.entries, apiNavn) },
            barnetsBostedsland = barnetsBostedsland.mapNotNull { finnEnumverdi(it, BarnetsBostedsland.entries, apiNavn) },
            kompetanseResultat = kompetanseResultat.mapNotNull { finnEnumverdi(it, KompetanseResultat.entries, apiNavn) },
            hjemlerFolketrygdloven = hjemlerFolketrygdloven,
            hjemlerEØSForordningen883 = hjemlerEOSForordningen883,
            hjemlerEØSForordningen987 = hjemlerEOSForordningen987,
            hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina,
        )
}

private val logger: Logger = LoggerFactory.getLogger(SanityBegrunnelseDto::class.java)

fun SanityBegrunnelse.inneholderGjelderFørstePeriodeTrigger() = this.triggere.contains(Trigger.GJELDER_FØRSTE_PERIODE)

fun <T : Enum<T>> finnEnumverdi(
    verdi: String,
    enumverdier: EnumEntries<T>,
    apiNavn: String?,
): T? {
    val enumverdi = enumverdier.firstOrNull { verdi == it.name }
    if (enumverdi == null) {
        logger.error(
            "$verdi på begrunnelsen $apiNavn er ikke blant verdiene til enumen ${enumverdier.javaClass.simpleName}",
        )
    }
    return enumverdi
}

enum class VilkårRolle {
    SOKER,
    BARN,
}

enum class EndretUtbetalingsperiodeTrigger {
    ETTER_ENDRET_UTBETALINGSPERIODE,
}
