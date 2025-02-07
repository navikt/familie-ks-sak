package no.nav.familie.ks.sak.kjerne.praksisendring

import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.inkluderer
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle.SKAL_GENERERE_ANDELER_FOR_PRAKSISENDRING_2024
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.ForskyvVilkårFørFebruar2025
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class Praksisendring2024Service(
    private val unleashService: UnleashNextMedContextService,
) {
    private val gyldigeMånederForPraksisendring = (8..12).map { YearMonth.of(2024, it) }

    fun genererAndelerForPraksisendring2024(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> =
        if (unleashService.isEnabled(SKAL_GENERERE_ANDELER_FOR_PRAKSISENDRING_2024)) {
            personopplysningGrunnlag.barna.mapNotNull {
                genererAndelerForPraksisendring2024(it, vilkårsvurdering, tilkjentYtelse)
            }
        } else {
            emptyList()
        }

    private fun genererAndelerForPraksisendring2024(
        barn: Person,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): AndelTilkjentYtelse? {
        val barnetsVilkårResultater =
            vilkårsvurdering.personResultater.find { it.aktør == barn.aktør }?.vilkårResultater
                ?: error("Finner ikke vilkårresultater for barn")

        if (!skalHaAndel(barn, barnetsVilkårResultater, tilkjentYtelse.andelerTilkjentYtelse)) {
            return null
        }

        val barn13Måneder = barn.fødselsdato.plusMonths(13).toYearMonth()
        val satsperiode =
            hentGyldigSatsFor(
                antallTimer = BigDecimal.ZERO,
                erDeltBosted = erDeltBostedIMåned(barn, barnetsVilkårResultater),
                stønadFom = barn13Måneder,
                stønadTom = barn13Måneder,
            )

        val kalkulertUtbetalingsbeløp = satsperiode.sats.prosent(satsperiode.prosent)

        return AndelTilkjentYtelse(
            behandlingId = tilkjentYtelse.behandling.id,
            tilkjentYtelse = tilkjentYtelse,
            aktør = barn.aktør,
            stønadFom = barn13Måneder,
            stønadTom = barn13Måneder,
            kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
            nasjonaltPeriodebeløp = kalkulertUtbetalingsbeløp,
            type = YtelseType.PRAKSISENDRING_2024,
            sats = satsperiode.sats,
            prosent = satsperiode.prosent,
        )
    }

    private fun skalHaAndel(
        barn: Person,
        vilkårResultater: Set<VilkårResultat>,
        andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
    ): Boolean {
        val barn13Måneder = barn.fødselsdato.plusMonths(13).toYearMonth()
        if (barn13Måneder !in gyldigeMånederForPraksisendring) {
            return false
        }

        val harOrdinærAndelISammeMånedSom13Måneder = andelerTilkjentYtelse.any { it.aktør == barn.aktør && it.stønadsPeriode().inkluderer(barn13Måneder) }
        if (harOrdinærAndelISammeMånedSom13Måneder) {
            return false
        }

        val starterIBarnehageSammeMånedSom13Måneder =
            vilkårResultater.any {
                it.periodeFom?.toYearMonth() == barn13Måneder && it.vilkårType == Vilkår.BARNEHAGEPLASS && it.resultat == Resultat.IKKE_OPPFYLT
            }

        val vilkårResultaterUtenBarnehageplass = vilkårResultater.filter { it.vilkårType != Vilkår.BARNEHAGEPLASS }.toSet()
        val forskøvedeVilkår = ForskyvVilkårFørFebruar2025.forskyvVilkårResultater(vilkårResultaterUtenBarnehageplass)

        val andreVilkårErOppfyltSammeMånedSom13Måneder =
            forskøvedeVilkår.all {
                it.value.any {
                    val fom = (it.fom ?: TIDENES_MORGEN).toYearMonth()
                    val tom = (it.tom ?: TIDENES_MORGEN).toYearMonth()
                    barn13Måneder in fom..tom && it.verdi.resultat == Resultat.OPPFYLT
                }
            }

        // TODO: Sjekk at fagsak og barn ligger i uttrekk

        return starterIBarnehageSammeMånedSom13Måneder && andreVilkårErOppfyltSammeMånedSom13Måneder
    }

    private fun erDeltBostedIMåned(
        barn: Person,
        barnetsVilkårResultater: Collection<VilkårResultat>,
    ): Boolean =
        barnetsVilkårResultater.any { vilkårResultat ->

            val barn13Måneder = barn.fødselsdato.plusMonths(13).toYearMonth()

            val fom = vilkårResultat.periodeFom?.toYearMonth() ?: error("Finner ikke fom for vilkårresultat")
            val tom = vilkårResultat.periodeTom?.toYearMonth() ?: barn13Måneder
            val barnEr13MånederIPeriode = barn13Måneder in fom..tom

            vilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER &&
                vilkårResultat.utdypendeVilkårsvurderinger.any { it == UtdypendeVilkårsvurdering.DELT_BOSTED } &&
                vilkårResultat.resultat == Resultat.OPPFYLT &&
                barnEr13MånederIPeriode
        }
}
