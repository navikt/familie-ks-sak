package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.ks.sak.api.dto.PersonResultatDto
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
    private val personidentService: PersonidentService,
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
        vilkårId: Long,
        personResultatDto: PersonResultatDto
    ): List<PersonResultatDto> {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId)
        val vilkårSomSkalOppdateres = personResultatDto.vilkårResultater.singleOrNull { it.id == vilkårId }
            ?: throw Feil("Fant ikke vilkårResultat med id $vilkårId ved opppdatering av vikår")

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, personResultatDto.personIdent)

        muterPersonVilkårResultaterPut(personResultat, vilkårSomSkalOppdateres)

        val vilkårResultat = personResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
            ?: error("Finner ikke vilkår med vilkårId $vilkårId på personResultat ${personResultat.id}")

        vilkårResultat.also {
            it.standardbegrunnelser = vilkårSomSkalOppdateres.avslagBegrunnelser ?: emptyList()
        }

        val migreringsdatoPåFagsak =
            behandlingService.hentMigreringsdatoPåFagsak(fagsakId = vilkårsvurdering.behandling.fagsak.id)
        validerVilkårStarterIkkeFørMigreringsdatoForMigreringsbehandling(
            vilkårsvurdering,
            vilkårResultat,
            migreringsdatoPåFagsak
        )

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
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
