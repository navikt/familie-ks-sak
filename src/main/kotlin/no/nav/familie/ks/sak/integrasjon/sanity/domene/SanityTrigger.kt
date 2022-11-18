package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.TriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType

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
    ALLTID_AUTOMATISK,
    ENDRET_UTBETALING,
    GJELDER_FØRSTE_PERIODE,
    GJELDER_FRA_INNVILGELSESTIDSPUNKT,
    BARN_DØD
}

enum class VilkårTrigger {
    VURDERING_ANNET_GRUNNLAG,
    MEDLEMSKAP,
    DELT_BOSTED,
    DELT_BOSTED_SKAL_IKKE_DELES
}

fun SanityBegrunnelse.tilTriggesAv(): TriggesAv {
    return TriggesAv(
        vilkår = this.vilkår?.toSet() ?: emptySet(),
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
        personerManglerOpplysninger = this.inneholderØvrigTrigger(ØvrigTrigger.MANGLER_OPPLYSNINGER),
        vurderingAnnetGrunnlag = (
            this.inneholderLovligOppholdTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderBosattIRiketTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderBorMedSøkerTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG)
            ),
        deltbosted = this.inneholderBorMedSøkerTrigger(VilkårTrigger.DELT_BOSTED),
        endretUtbetalingSkalUtbetales = this.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger
            ?: EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT,
        gjelderFørstePeriode = this.inneholderØvrigTrigger(ØvrigTrigger.GJELDER_FØRSTE_PERIODE),
        gjelderFraInnvilgelsestidspunkt = this.inneholderØvrigTrigger(ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT),
        barnDød = this.inneholderØvrigTrigger(ØvrigTrigger.BARN_DØD)
    )
}

fun SanityBegrunnelse.inneholderØvrigTrigger(øvrigTrigger: ØvrigTrigger) =
    this.ovrigeTriggere?.contains(øvrigTrigger) ?: false

fun SanityBegrunnelse.inneholderVilkår(vilkår: Vilkår) =
    this.vilkår?.contains(vilkår) ?: false

fun SanityBegrunnelse.inneholderLovligOppholdTrigger(vilkårTrigger: VilkårTrigger) =
    this.lovligOppholdTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderBosattIRiketTrigger(vilkårTrigger: VilkårTrigger) =
    this.bosattIRiketTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderBorMedSøkerTrigger(vilkårTrigger: VilkårTrigger) =
    this.borMedSokerTriggere?.contains(vilkårTrigger) ?: false
