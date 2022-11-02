package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.integrasjon.sanity.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.BrevEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.BrevVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.Årsak
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.math.BigDecimal

data class TriggesAv(
    val vilkår: Set<Vilkår>,
    val personTyper: Set<PersonType>,
    val endringsaarsaker: Set<Årsak>,
    val satsendring: Boolean,
    val valgbar: Boolean,
    val personerManglerOpplysninger: Boolean,
    val etterEndretUtbetaling: Boolean,
    val gjelderFraInnvilgelsestidspunkt: Boolean,
    val endretUtbetalingSkalUtbetales: EndretUtbetalingsperiodeDeltBostedTriggere,
    val barnDød: Boolean,
    val gjelderFørstePeriode: Boolean,
    val vurderingAnnetGrunnlag: Boolean,
    val deltbosted: Boolean
    ) {
    fun erEndret() = endringsaarsaker.isNotEmpty()


    fun erUtdypendeVilkårsvurderingOppfylt(
        vilkårResultat: BrevVilkårResultat
    ): Boolean {
        return erDeltBostedOppfylt(vilkårResultat) &&
                erSkjønnsmessigVurderingOppfylt(vilkårResultat)
    }

    private fun erSkjønnsmessigVurderingOppfylt(vilkårResultat: BrevVilkårResultat): Boolean {
        val vilkårResultatInneholderVurderingAnnetGrunnlag =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG)

        return this.vurderingAnnetGrunnlag == vilkårResultatInneholderVurderingAnnetGrunnlag
    }


    private fun erDeltBostedOppfylt(vilkårResultat: BrevVilkårResultat): Boolean {
        val vilkårResultatInneholderDeltBosted =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)

        return this.deltbosted == vilkårResultatInneholderDeltBosted
    }

}


fun BrevEndretUtbetalingAndel.oppfyllerSkalUtbetalesTrigger(
    triggesAv: TriggesAv
): Boolean {
    val inneholderAndelSomSkalUtbetales = this.prosent!! != BigDecimal.ZERO
    return when (triggesAv.endretUtbetalingSkalUtbetales) {
        EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT -> true
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES -> inneholderAndelSomSkalUtbetales
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES -> !inneholderAndelSomSkalUtbetales
    }
}