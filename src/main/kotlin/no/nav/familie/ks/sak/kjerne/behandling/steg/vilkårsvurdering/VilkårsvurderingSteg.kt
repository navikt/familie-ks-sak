package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.ks.sak.common.tidslinje.validerIngenOverlapp
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.tilPeriodeResultater
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
        val søknadDto = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id).tilSøknadDto()
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        validerVilkårsvurdering(vilkårsvurdering, personopplysningGrunnlag, søknadDto, behandling)

        beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, personopplysningGrunnlag, vilkårsvurdering)
    }

    fun validerVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        søknadGrunnlagDto: SøknadDto,
        behandling: Behandling
    ) {
        validerAtDetFinnesBarnIPersonopplysningsgrunnlaget(personopplysningGrunnlag, søknadGrunnlagDto, behandling)
        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL) {
            validerAtIngenVilkårErSattEtterSøkersDød(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering
            )
        }
        validerAtDetIkkeErOverlappMellomGradertBarnehageplassOgDeltBosted(vilkårsvurdering)
        validerAtPerioderIBarnehageplassSamsvarerMedPeriodeIMellom1og2ÅrVilkår(vilkårsvurdering)
        validerAtDetIkkeFinnesMerEnn2EndringerISammeMånedIBarnehageplassVilkår(vilkårsvurdering)
    }

    private fun validerAtDetFinnesBarnIPersonopplysningsgrunnlaget(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        søknadGrunnlagDto: SøknadDto,
        behandling: Behandling
    ) {
        val barna = personopplysningGrunnlag.barna
        val uregistrerteBarn =
            søknadGrunnlagDto.barnaMedOpplysninger.filter { !it.erFolkeregistrert && it.inkludertISøknaden }

        if (barna.isEmpty() && uregistrerteBarn.isEmpty()) {
            throw FunksjonellFeil(
                melding = "Ingen barn i personopplysningsgrunnlag ved validering av vilkårsvurdering på behandling ${behandling.id}",
                frontendFeilmelding = "Barn må legges til for å gjennomføre vilkårsvurdering."
            )
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

    private fun validerAtPerioderIBarnehageplassSamsvarerMedPeriodeIMellom1og2ÅrVilkår(
        vilkårsvurdering: Vilkårsvurdering
    ) {
        vilkårsvurdering.personResultater.filter { !it.erSøkersResultater() }.forEach { personResultat ->
            val barnehageplassVilkårResultater = personResultat.vilkårResultater.filter {
                it.vilkårType == Vilkår.BARNEHAGEPLASS
            }

            val minFraOgMedDatoIBarnehageplassVilkårResultater =
                barnehageplassVilkårResultater.sortedBy { it.periodeFom }.first().periodeFom
                    ?: error("Mangler fom dato")
            val maksTilOmMedDatoIBarnehageplassVilkårResultater =
                barnehageplassVilkårResultater.sortedWith(compareBy(nullsLast()) { it.periodeTom }).last().periodeTom
                    ?: TIDENES_ENDE

            val mellom1ÅrOg2ÅrVilkårResultater = personResultat.vilkårResultater.filter {
                it.vilkårType == Vilkår.BARNETS_ALDER && it.resultat == Resultat.OPPFYLT
            }
            val minFraOgMedDatoIMellom1ÅrOg2ÅrVilkårResultater =
                mellom1ÅrOg2ÅrVilkårResultater.sortedBy { it.periodeFom }.first().periodeFom
                    ?: error("Mangler fom dato")
            val maksTilOmMedDatoIMellom1ÅrOg2ÅrVilkårResultater =
                mellom1ÅrOg2ÅrVilkårResultater.sortedBy { it.periodeTom }.last().periodeTom
                    ?: error("Mangler tom dato")
            if (minFraOgMedDatoIBarnehageplassVilkårResultater.isAfter(minFraOgMedDatoIMellom1ÅrOg2ÅrVilkårResultater) ||
                maksTilOmMedDatoIBarnehageplassVilkårResultater.isBefore(maksTilOmMedDatoIMellom1ÅrOg2ÅrVilkårResultater)
            ) {
                throw FunksjonellFeil(
                    "Det mangler vurdering på vilkåret ${Vilkår.BARNEHAGEPLASS.beskrivelse}. " +
                        "Hele eller deler av perioden der barnet er mellom 1 og 2 år er ikke vurdert."
                )
            }
            if (barnehageplassVilkårResultater.any {
                it.periodeFom?.isAfter(maksTilOmMedDatoIMellom1ÅrOg2ÅrVilkårResultater) == true
            }
            ) {
                throw FunksjonellFeil(
                    "Du har lagt til en periode på vilkåret ${Vilkår.BARNEHAGEPLASS.beskrivelse}" +
                        " som starter etter at barnet har fylt 2 år eller startet på skolen. " +
                        "Du må fjerne denne perioden for å kunne fortsette"
                )
            }
        }
    }

    private fun validerAtDetIkkeFinnesMerEnn2EndringerISammeMånedIBarnehageplassVilkår(vilkårsvurdering: Vilkårsvurdering) {
        vilkårsvurdering.personResultater.filter { !it.erSøkersResultater() }.forEach { personResultat ->
            if (personResultat.tilPeriodeResultater().any { periodeResultat ->
                periodeResultat.vilkårResultater.count { it.vilkårType == Vilkår.BARNEHAGEPLASS } > 2
            }
            ) {
                throw FunksjonellFeil(
                    "Du har lagt inn flere enn 2 endringer i barnehagevilkåret i samme måned. " +
                        "Dette er ikke støttet enda. Ta kontakt med Team Familie."
                )
            }
        }
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(VilkårsvurderingSteg::class.java)
    }
}
