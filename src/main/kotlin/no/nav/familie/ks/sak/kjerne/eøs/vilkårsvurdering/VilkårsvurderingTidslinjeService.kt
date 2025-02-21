package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val adopsjonService: AdopsjonService,
) {
    fun lagVilkårsvurderingTidslinjer(behandlingId: Long): VilkårsvurderingTidslinjer {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId)
        val adopsjonerIBehandling = adopsjonService.hentAlleAdopsjonerForBehandling(BehandlingId(behandlingId))

        return VilkårsvurderingTidslinjer(vilkårsvurdering, personopplysningGrunnlag, adopsjonerIBehandling)
    }

    fun hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId: Long): Tidslinje<Boolean> {
        val søker = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId = behandlingId).søker
        val personResultater =
            vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)
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

    fun hentBarnasRegelverkResultatTidslinjer(behandlingId: BehandlingId): Map<Aktør, Tidslinje<RegelverkResultat>> =
        lagVilkårsvurderingTidslinjer(behandlingId.id)
            .barnasTidslinjer()
            .mapValues { (_, tidslinjer) ->
                tidslinjer.kombinertRegelverkResultatTidslinje
            }
}
