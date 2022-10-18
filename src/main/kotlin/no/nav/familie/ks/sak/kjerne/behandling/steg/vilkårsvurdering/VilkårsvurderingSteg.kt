package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingSteg(
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingService: VilkårsvurderingService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.VILKÅRSVURDERING

    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingService.hentBehandling(behandlingId)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL) {
            validerAtIngenVilkårErSattEtterSøkersDød(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering
            )
        }

        validerAtDetIkkeErOverlappMellomGradertBarnehageplassOgDeltBosted(vilkårsvurdering)

        //TODO: Kommer etter vi har fått inn behandlignsresultat.
        // beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }

    private fun validerAtDetIkkeErOverlappMellomGradertBarnehageplassOgDeltBosted(vilkårsvurdering: Vilkårsvurdering) {
        vilkårsvurdering.personResultater.forEach {

            val relevanteVilkårResultater = it.vilkårResultater.filter { vilkårResultat ->
                vilkårResultat.antallTimer != null || vilkårResultat.utdypendeVilkårsvurderinger.contains(
                    UtdypendeVilkårsvurdering.DELT_BOSTED
                )
            }
            relevanteVilkårResultater.tilTidslinje()
        }
    }

    fun validerAtIngenVilkårErSattEtterSøkersDød(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        val vilkårResultaterSøker =
            vilkårsvurdering.hentPersonResultaterTilAktør(personopplysningGrunnlag.søker.aktør.aktørId)
        val søkersDød = personopplysningGrunnlag.søker.dødsfall?.dødsfallDato!!

        val vilkårSomEnderEtterSøkersDød =
            vilkårResultaterSøker
                .groupBy { it.vilkårType }
                .mapNotNull { (vilkårType, vilkårResultater) ->
                    vilkårType.takeIf {
                        vilkårResultater.any {
                            it.periodeTom?.isAfter(søkersDød) ?: true
                        }
                    }
                }

        if (vilkårSomEnderEtterSøkersDød.isNotEmpty()) {
            throw FunksjonellFeil(
                "Ved behandlingsårsak \"Dødsfall\" må vilkårene på søker avsluttes " +
                        "senest dagen søker døde, men " +
                        slåSammen(vilkårSomEnderEtterSøkersDød.map { "\"" + it.beskrivelse + "\"" }) +
                        " vilkåret til søker slutter etter søkers død."
            )
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VilkårsvurderingSteg::class.java)
    }
}
