package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.ks.sak.api.dto.EndreVilkårResultatDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårService(
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personidentService: PersonidentService
) {

    fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(
        behandlingId = behandlingId
    ) ?: throw Feil(
        message = "Fant ikke aktiv vilkårsvurdering for behandling $behandlingId",
        frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
    )

    @Transactional
    fun endreVilkår(
        behandlingId: Long,
        endreVilkårResultatDto: EndreVilkårResultatDto
    ) {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId)

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, endreVilkårResultatDto.personIdent)

        val nyeVilkårResultater =
            endreVilkårResultat(personResultat.vilkårResultater.toList(), endreVilkårResultatDto.endretVilkårResultat)

        personResultat.vilkårResultater.clear()
        personResultat.vilkårResultater.addAll(nyeVilkårResultater)
    }

    private fun finnPersonResultatForPersonThrows(
        personResultater: Set<PersonResultat>,
        personIdent: String
    ): PersonResultat {
        val aktør = personidentService.hentAktør(personIdent)

        return personResultater.find { it.aktør == aktør } ?: throw Feil(
            message = fantIkkeVilkårsvurderingForPersonFeilmelding,
            frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident $personIdent"
        )
    }

    companion object {

        const val fantIkkeAktivVilkårsvurderingFeilmelding = "Fant ikke aktiv vilkårsvurdering"
        const val fantIkkeVilkårsvurderingForPersonFeilmelding = "Fant ikke vilkårsvurdering for person"
    }
}

fun SIVILSTAND.somForventetHosBarn() = this == SIVILSTAND.UOPPGITT || this == SIVILSTAND.UGIFT
