package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.ks.sak.common.tidslinje.validerIngenOverlapp
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.finnHøyesteKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.tilPeriodeResultater
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
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
    private val beregningService: BeregningService,
    private val kompetanseService: KompetanseService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.VILKÅRSVURDERING

    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")

        val behandling = behandlingService.hentBehandling(behandlingId)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val søknadDto = søknadGrunnlagService.finnAktiv(behandlingId = behandling.id)?.tilSøknadDto()
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        validerVilkårsvurdering(vilkårsvurdering, personopplysningGrunnlag, søknadDto, behandling)

        settBehandlingstemaBasertPåVilkårsvurdering(behandling, vilkårsvurdering)

        beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, personopplysningGrunnlag, vilkårsvurdering)

        // sjekker og tilpasser kompetanse skjema når vilkårer er vurdert etter EØS forordingen
        // eller det ligger allerede en kompetanse
        val finnesKompetanserEllerVilkårVurdertEtterEøs = vilkårsvurdering.personResultater.any {
            it.vilkårResultater.any { vilkårResultat -> vilkårResultat.vurderesEtter == Regelverk.EØS_FORORDNINGEN }
        } || kompetanseService.hentKompetanser(behandlingId).isNotEmpty()

        if (finnesKompetanserEllerVilkårVurdertEtterEøs) {
            logger.info("Oppretter/Tilpasser kompetanse perioder for behandlingId=$behandlingId")
            kompetanseService.tilpassKompetanse(behandlingId)
        }
    }

    private fun settBehandlingstemaBasertPåVilkårsvurdering(
        nåværendeBehandling: Behandling,
        nåværendeVilkårsvurdering: Vilkårsvurdering
    ) {
        val nåværendeVilkårVurderesEtterEøs = nåværendeVilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
            .filter { it.behandlingId == nåværendeBehandling.id }
            .any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }

        val kategoriForNåværendeBehandling =
            if (nåværendeVilkårVurderesEtterEøs) BehandlingKategori.EØS else BehandlingKategori.NASJONAL

        val forrigeVedtatteBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(nåværendeBehandling.fagsak.id)

        val kategoriFraForrigeVedtatteBehandling =
            forrigeVedtatteBehandling?.let { forrigeBehandling ->

                // Vi sjekker om vi har løpende EØS vilkårresultater fra forrige behandling
                val harLøpendeEøsUtbetalingIForrigeVedtattBehandling =
                    vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(forrigeBehandling.id)
                        .personResultater.flatMap { it.vilkårResultater }
                        .filter {
                            (it.periodeTom ?: TIDENES_ENDE).isAfter(LocalDate.now().sisteDagIMåned())
                        }
                        .any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }

                if (harLøpendeEøsUtbetalingIForrigeVedtattBehandling) BehandlingKategori.EØS else BehandlingKategori.NASJONAL
            } ?: BehandlingKategori.NASJONAL

        val kategoriSomSkalBrukes =
            listOf(kategoriForNåværendeBehandling, kategoriFraForrigeVedtatteBehandling).finnHøyesteKategori()

        behandlingService.endreBehandlingstemaPåBehandling(nåværendeBehandling.id, kategoriSomSkalBrukes)
    }

    fun validerVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        søknadGrunnlagDto: SøknadDto?,
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
        validerAtPerioderIBarnehageplassSamsvarerMedPeriodeIBarnetsAlderVilkår(
            vilkårsvurdering,
            personopplysningGrunnlag
        )
        validerAtDetIkkeFinnesMerEnn2EndringerISammeMånedIBarnehageplassVilkår(vilkårsvurdering)
        validerAtDatoErKorrektIBarnasVilkår(vilkårsvurdering, personopplysningGrunnlag.barna)
        validerIkkeBlandetRegelverk(personopplysningGrunnlag, vilkårsvurdering)
    }

    private fun validerAtDetFinnesBarnIPersonopplysningsgrunnlaget(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        søknadGrunnlagDto: SøknadDto?,
        behandling: Behandling
    ) {
        val barna = personopplysningGrunnlag.barna
        val uregistrerteBarn = søknadGrunnlagDto?.barnaMedOpplysninger?.filter {
            !it.erFolkeregistrert && it.inkludertISøknaden
        } ?: emptyList()

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

    private fun validerAtPerioderIBarnehageplassSamsvarerMedPeriodeIBarnetsAlderVilkår(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        vilkårsvurdering.personResultater.filter { !it.erSøkersResultater() }.forEach { personResultat ->
            val person =
                personopplysningGrunnlag.personer.single { it.aktør.aktivFødselsnummer() == personResultat.aktør.aktivFødselsnummer() }

            val barnehageplassVilkårResultater = personResultat.vilkårResultater.filter {
                it.vilkårType == Vilkår.BARNEHAGEPLASS
            }

            val minFraOgMedDatoIBarnehageplassVilkårResultater =
                barnehageplassVilkårResultater.sortedBy { it.periodeFom }.first().periodeFom
                    ?: error("Mangler fom dato")
            val maksTilOmMedDatoIBarnehageplassVilkårResultater =
                barnehageplassVilkårResultater.sortedWith(compareBy(nullsLast()) { it.periodeTom }).last().periodeTom
                    ?: TIDENES_ENDE

            val barnetsAlderVilkårResultater =
                personResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BARNETS_ALDER }

            val minFraOgMedDatoIBarnetsAlderVilkårResultater =
                barnetsAlderVilkårResultater.sortedBy { it.periodeFom }.first().periodeFom
                    ?: person.fødselsdato.plusYears(1)

            val maksTilOmMedDatoIBarnetsAlderVilkårResultater =
                barnetsAlderVilkårResultater.sortedBy { it.periodeTom }.last().periodeTom
                    ?: person.fødselsdato.plusYears(2)

            if (minFraOgMedDatoIBarnehageplassVilkårResultater.isAfter(minFraOgMedDatoIBarnetsAlderVilkårResultater) ||
                maksTilOmMedDatoIBarnehageplassVilkårResultater.isBefore(maksTilOmMedDatoIBarnetsAlderVilkårResultater)
            ) {
                throw FunksjonellFeil(
                    "Det mangler vurdering på vilkåret ${Vilkår.BARNEHAGEPLASS.beskrivelse}. " +
                        "Hele eller deler av perioden der barnet er mellom 1 og 2 år er ikke vurdert."
                )
            }
            if (barnehageplassVilkårResultater.any {
                it.periodeFom?.isAfter(maksTilOmMedDatoIBarnetsAlderVilkårResultater) == true
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

    private fun validerIkkeBlandetRegelverk(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(vilkårsvurdering, personopplysningGrunnlag)
        if (vilkårsvurderingTidslinjer.harBlandetRegelverk()) {
            throw FunksjonellFeil(
                melding = "Det er forskjellig regelverk for en eller flere perioder for søker eller barna"
            )
        }
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(VilkårsvurderingSteg::class.java)
    }
}
