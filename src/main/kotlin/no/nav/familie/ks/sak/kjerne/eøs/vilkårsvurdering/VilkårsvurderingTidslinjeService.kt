package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {
    fun lagVilkårsvurderingTidslinjer(behandlingId: Long): VilkårsvurderingTidslinjer {
        val vilkårsvurdering =
            vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)
                ?: throw Feil("Finnes ikke aktiv vilkårsvurdering for behandlingId=$behandlingId")
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId)

        return VilkårsvurderingTidslinjer(vilkårsvurdering, personopplysningGrunnlag)
    }

    fun hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId: Long): Tidslinje<Boolean> {
        val søker = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId = behandlingId).søker
        val personResultater =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)
                .personResultater
        val søkerPersonresultater = personResultater.single { it.aktør == søker.aktør }

        val erAnnenForelderOmfattetAvNorskLovgivingTidslinjeMedKunPerioderSomStrekkerSegOver1MånedForskyvetTidslinje =
            søkerPersonresultater.vilkårResultater
                .filter { it.vilkårType === Vilkår.BOSATT_I_RIKET && it.erOppfylt() }
                .filter { it.periodeFom?.month != it.periodeTom?.month }
                .map {
                    Periode(
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                        it.periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned(),
                        it.periodeTom?.sisteDagIMåned(),
                    )
                }.filtrerIkkeNull()
                .tilTidslinje()

        return erAnnenForelderOmfattetAvNorskLovgivingTidslinjeMedKunPerioderSomStrekkerSegOver1MånedForskyvetTidslinje
    }
}
