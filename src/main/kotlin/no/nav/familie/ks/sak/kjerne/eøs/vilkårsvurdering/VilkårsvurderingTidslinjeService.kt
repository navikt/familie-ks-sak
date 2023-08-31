package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {

    fun lagVilkårsvurderingTidslinjer(behandlingId: Long): VilkårsvurderingTidslinjer {
        val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)
            ?: throw Feil("Finnes ikke aktiv vilkårsvurdering for behandlingId=$behandlingId")
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId)

        return VilkårsvurderingTidslinjer(vilkårsvurdering, personopplysningGrunnlag)
    }
}
