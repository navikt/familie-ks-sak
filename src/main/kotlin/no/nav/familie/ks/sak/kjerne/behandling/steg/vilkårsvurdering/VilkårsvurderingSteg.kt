package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.ks.sak.common.tidslinje.validerIngenOverlapp
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class VilkårsvurderingSteg(
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val behandlingService: BehandlingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val beregningService: BeregningService
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

        validerAtDetFinnesBarnIPersonopplysningsgrunnlaget(personopplysningGrunnlag, behandling)
        validerAtDetIkkeErOverlappMellomGradertBarnehageplassOgDeltBosted(vilkårsvurdering)

        beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, personopplysningGrunnlag, vilkårsvurdering)
    }

    private fun validerAtDetIkkeErOverlappMellomGradertBarnehageplassOgDeltBosted(vilkårsvurdering: Vilkårsvurdering) {
        vilkårsvurdering.personResultater.forEach {
            it.vilkårResultater.filter { vilkårResultat ->
                val gradertBarnehageplass =
                    vilkårResultat.antallTimer != null &&
                        vilkårResultat.antallTimer > BigDecimal(0) &&
                        vilkårResultat.vilkårType == Vilkår.BARNEHAGEPLASS

                val deltBosted =
                    vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)

                gradertBarnehageplass || deltBosted
            }.map { vilkårResultat ->
                TidslinjePeriodeMedDato(
                    verdi = vilkårResultat,
                    fom = vilkårResultat.periodeFom,
                    tom = vilkårResultat.periodeTom
                )
            }.validerIngenOverlapp("Det er lagt inn gradert barnehageplass og delt bosted for samme periode.")
        }
    }

    private fun validerAtIngenVilkårErSattEtterSøkersDød(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        val vilkårResultaterSøker =
            vilkårsvurdering.hentPersonResultaterTilAktør(personopplysningGrunnlag.søker.aktør.aktørId)
        val søkersDød = personopplysningGrunnlag.søker.dødsfall?.dødsfallDato ?: LocalDate.MAX

        val vilkårSomEnderEtterSøkersDød =
            vilkårResultaterSøker.groupBy { it.vilkårType }.mapNotNull { (vilkårType, vilkårResultater) ->
                vilkårType.takeIf {
                    vilkårResultater.any {
                        it.periodeTom?.isAfter(søkersDød) ?: true
                    }
                }
            }

        if (vilkårSomEnderEtterSøkersDød.isNotEmpty()) {
            throw FunksjonellFeil(
                "Ved behandlingsårsak \"Dødsfall\" må vilkårene på søker avsluttes " + "senest dagen søker døde, men " + slåSammen(
                    vilkårSomEnderEtterSøkersDød.map { "\"" + it.beskrivelse + "\"" }
                ) + " vilkåret til søker slutter etter søkers død."
            )
        }
    }

    fun validerAtDetFinnesBarnIPersonopplysningsgrunnlaget(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        behandling: Behandling
    ) {
        val barna = personopplysningGrunnlag.barna
        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id).tilSøknadDto()
        val uregistrerteBarn =
            søknadGrunnlag.barnaMedOpplysninger.filter { !it.erFolkeregistrert && it.inkludertISøknaden }

        if (barna.isEmpty() && uregistrerteBarn.isEmpty()) {
            throw FunksjonellFeil(
                melding = "Ingen barn i personopplysningsgrunnlag ved validering av vilkårsvurdering på behandling ${behandling.id}",
                frontendFeilmelding = "Barn må legges til for å gjennomføre vilkårsvurdering."
            )
        }
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(VilkårsvurderingSteg::class.java)
    }
}
