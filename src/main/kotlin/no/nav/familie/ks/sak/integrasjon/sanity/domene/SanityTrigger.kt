package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår

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

fun SanityBegrunnelse.inneholderØvrigTrigger(øvrigTrigger: ØvrigTrigger) =
    this.ovrigeTriggere?.contains(øvrigTrigger) ?: false

fun SanityBegrunnelse.inneholderVilkår(vilkår: Vilkår) =
    this.vilkaar?.contains(vilkår) ?: false

fun SanityBegrunnelse.inneholderLovligOppholdTrigger(vilkårTrigger: VilkårTrigger) =
    this.lovligOppholdTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderBosattIRiketTrigger(vilkårTrigger: VilkårTrigger) =
    this.bosattIRiketTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderBorMedSøkerTrigger(vilkårTrigger: VilkårTrigger) =
    this.borMedSokerTriggere?.contains(vilkårTrigger) ?: false
