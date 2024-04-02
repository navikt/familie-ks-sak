package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.LocalDateProvider
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.convertDataClassToJson
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatUtils.skalUtledeSøknadsresultatForBehandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandlingsresultatService(
    private val behandlingService: BehandlingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personidentService: PersonidentService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val kompetanseService: KompetanseService,
    private val localDateProvider: LocalDateProvider,
) {
    @Deprecated("Erstattes av ny behandlingsresultat logikk og kan fjernes sammen med relevant kode når featuretoggle slettes.")
    fun utledBehandlingsresultat(behandling: Behandling): Behandlingsresultat {
        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        val andelerMedEndringer =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val forrigeAndelerMedEndringer =
            forrigeBehandling?.let {
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(it.id)
            } ?: emptyList()

        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        val personerFremstiltKravFor = hentBarna(behandling)

        val behandlingsresultatPersoner =
            lagBehandlingsresulatPersoner(
                behandling,
                personerFremstiltKravFor,
                andelerMedEndringer,
                forrigeAndelerMedEndringer,
                vilkårsvurdering,
            )
        secureLogger.info("Behandlingsresultatpersoner: ${behandlingsresultatPersoner.convertDataClassToJson()}")

        val ytelsePersonerMedResultat =
            YtelsePersonUtils.utledYtelsePersonerMedResultat(
                behandlingsresultatPersoner = behandlingsresultatPersoner,
                uregistrerteBarn = søknadGrunnlagService.finnAktiv(behandling.id)?.hentUregistrerteBarn()?.map { it.personnummer } ?: emptyList(),
            )

        val alleAndelerHar0IUtbetaling = andelerMedEndringer.all { it.kalkulertUtbetalingsbeløp == 0 }

        YtelsePersonUtils.validerYtelsePersoner(ytelsePersonerMedResultat)

        secureLogger.info("Utledet YtelsePersonResultat på behandling $behandling: $ytelsePersonerMedResultat")

        vilkårsvurderingService.oppdater(
            vilkårsvurdering.also {
                it.ytelsePersoner = ytelsePersonerMedResultat.writeValueAsString()
            },
        )

        val ytelsePersonResultater =
            YtelsePersonUtils.oppdaterYtelsePersonResultaterVedOpphør(ytelsePersonerMedResultat)
        val behandlingsresultat =
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersonResulater(
                ytelsePersonResultater,
                alleAndelerHar0IUtbetaling,
            )

        logger.info("Utledet behandlingsresulat på behandling er $behandling: $behandlingsresultat")
        secureLogger.info("Utledet behandlingsresulat på behandling er $behandling: $behandlingsresultat")

        return behandlingsresultat
    }

    fun lagBehandlingsresulatPersoner(
        behandling: Behandling,
        personerFremslitKravFor: List<Aktør>,
        andelerMedEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        forrigeAndelerMedEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        vilkårsvurdering: Vilkårsvurdering,
    ) = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        .personer.filter { it.type == PersonType.BARN }.map {
            val personresultat =
                vilkårsvurdering.personResultater.find { personResultat -> personResultat.aktør == it.aktør }
            val harEksplisittAvslagIVilkårsvurderingen = personresultat?.harEksplisittAvslag() ?: false

            val harEksplisittAvslagIEndreteUtbetalinger =
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                    behandling.id,
                ).any { endretUtbetalingAndel ->
                    endretUtbetalingAndel.erEksplisittAvslagPåSøknad == true && endretUtbetalingAndel.person?.aktør == it.aktør
                }

            val erDetFramtidigOpphørPåBarnehagevilkåret =
                personresultat?.let { erDetFramtidigOpphørPåBarnehagevilkåret(personresultat) } ?: false

            BehandlingsresultatUtils.utledBehandlingsresultatDataForPerson(
                person = it,
                personerFremstiltKravFor = personerFremslitKravFor,
                andelerMedEndringer = andelerMedEndringer,
                forrigeAndelerMedEndringer = forrigeAndelerMedEndringer,
                erEksplisittAvslag = harEksplisittAvslagIVilkårsvurderingen || harEksplisittAvslagIEndreteUtbetalinger,
                erDetFramtidigOpphørPåBarnehagevilkåret = erDetFramtidigOpphørPåBarnehagevilkåret,
            )
        }

    private fun erDetFramtidigOpphørPåBarnehagevilkåret(personresultat: PersonResultat): Boolean {
        val oppfylteVilkårsresultater = personresultat.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
        val vilkårResultaterForBarnehagevilkår =
            oppfylteVilkårsresultater.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }
        val vilkårResultaterForBarnetsAlder = oppfylteVilkårsresultater.filter { it.vilkårType == Vilkår.BARNETS_ALDER }

        val tidBarnehagevilkåretAvsluttesPgaBarnehageplass =
            vilkårResultaterForBarnehagevilkår.filter { it.søkerHarMeldtFraOmBarnehageplass == true }
                .maxOfOrNull { it.periodeTom ?: TIDENES_ENDE }

        val tidAlderVilkåretAvsluttes = vilkårResultaterForBarnetsAlder.maxOf { it.periodeTom ?: TIDENES_ENDE }

        return if (tidBarnehagevilkåretAvsluttesPgaBarnehageplass == null) {
            false
        } else {
            tidBarnehagevilkåretAvsluttesPgaBarnehageplass < tidAlderVilkåretAvsluttes
        }
    }

    internal fun utledBehandlingsresultatNy(behandlingId: Long): Behandlingsresultat {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)

        val søknadGrunnlag = søknadGrunnlagService.finnAktiv(behandlingId = behandling.id)
        val søknadDto = søknadGrunnlag?.tilSøknadDto()

        val forrigeAndelerTilkjentYtelse = forrigeBehandling?.let { andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = it.id) } ?: emptyList()
        val andelerTilkjentYtelse = andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val forrigeEndretUtbetalingAndeler = forrigeBehandling?.let { endretUtbetalingAndelService.hentEndredeUtbetalingAndeler(behandlingId = it.id) } ?: emptyList()
        val endretUtbetalingAndeler = endretUtbetalingAndelService.hentEndredeUtbetalingAndeler(behandlingId = behandlingId)

        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)

        val personerIBehandling = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id).personer.toSet()
        val personerIForrigeBehandling = forrigeBehandling?.let { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = forrigeBehandling.id).personer.toSet() } ?: emptySet()

        val forrigeVilkårsvurdering = forrigeBehandling?.id?.let { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = it) }
        val forrigePersonResultat = forrigeVilkårsvurdering?.personResultater ?: emptySet()

        val personerFremstiltKravFor =
            finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDto = søknadDto,
                forrigeBehandling = forrigeBehandling,
            )

        val nåværendePersonResultat = vilkårsvurdering.personResultater
        BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(personerFremstiltKravFor = personerFremstiltKravFor, personResultater = nåværendePersonResultat)

        // 1 SØKNAD
        val søknadsresultat =
            if (skalUtledeSøknadsresultatForBehandling(behandling)) {
                BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
                    nåværendeAndeler = andelerTilkjentYtelse,
                    forrigeAndeler = forrigeAndelerTilkjentYtelse,
                    endretUtbetalingAndeler = endretUtbetalingAndeler,
                    personerFremstiltKravFor = personerFremstiltKravFor,
                    nåværendePersonResultater = nåværendePersonResultat,
                    behandlingÅrsak = behandling.opprettetÅrsak,
                    finnesUregistrerteBarn = søknadGrunnlag?.hentUregistrerteBarn()?.isNotEmpty() ?: false,
                )
            } else {
                null
            }

        // 2 ENDRINGER
        val endringsresultat =
            if (forrigeBehandling != null) {
                val kompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(behandlingId))
                val forrigeKompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(forrigeBehandling.id))

                BehandlingsresultatEndringUtils.utledEndringsresultat(
                    nåværendeAndeler = andelerTilkjentYtelse,
                    forrigeAndeler = forrigeAndelerTilkjentYtelse,
                    nåværendeEndretAndeler = endretUtbetalingAndeler,
                    forrigeEndretAndeler = forrigeEndretUtbetalingAndeler,
                    nåværendePersonResultat = nåværendePersonResultat,
                    forrigePersonResultat = forrigePersonResultat,
                    nåværendeKompetanser = kompetanser.toList(),
                    forrigeKompetanser = forrigeKompetanser.toList(),
                    personerFremstiltKravFor = personerFremstiltKravFor,
                    personerIBehandling = personerIBehandling,
                    personerIForrigeBehandling = personerIForrigeBehandling,
                    nåDato = localDateProvider.now(),
                )
            } else {
                Endringsresultat.INGEN_ENDRING
            }

        // 3 OPPHØR
        val opphørsresultat =
            BehandlingsresultatOpphørUtils.hentOpphørsresultatPåBehandling(
                nåværendeAndeler = andelerTilkjentYtelse,
                forrigeAndeler = forrigeAndelerTilkjentYtelse,
                nåværendeEndretAndeler = endretUtbetalingAndeler,
                forrigeEndretAndeler = forrigeEndretUtbetalingAndeler,
                nåværendePersonResultaterPåBarn = nåværendePersonResultat.filter { !it.erSøkersResultater() },
                forrigePersonResultaterPåBarn = forrigePersonResultat.filter { !it.erSøkersResultater() },
                nåMåned = localDateProvider.now().toYearMonth(),
            )

        // KOMBINER
        val behandlingsresultat = BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(søknadsresultat, endringsresultat, opphørsresultat)

        return behandlingsresultat
    }

    internal fun finnPersonerFremstiltKravFor(
        behandling: Behandling,
        søknadDto: SøknadDto?,
        forrigeBehandling: Behandling?,
    ): List<Aktør> {
        val personerFremstiltKravFor =
            when (behandling.opprettetÅrsak) {
                BehandlingÅrsak.SØKNAD -> {
                    // alle barna som er krysset av på søknad
                    søknadDto?.barnaMedOpplysninger?.filter { it.erFolkeregistrert && it.inkludertISøknaden }?.map { personidentService.hentAktør(it.ident) } ?: emptyList()
                }

                BehandlingÅrsak.KLAGE -> personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id).personer.map { it.aktør }
                else -> emptyList()
            }

        return personerFremstiltKravFor.distinct()
    }

    private fun hentBarna(behandling: Behandling): List<Aktør> {
        // Søknad kan ha flere barn som er inkludert i søknaden og folkeregistert, men ikke i behandling
        val barnFraSøknad =
            if (behandling.erSøknad()) {
                søknadGrunnlagService.hentAktiv(behandling.id).tilSøknadDto()
                    .barnaMedOpplysninger.filter { it.inkludertISøknaden && it.erFolkeregistrert }
                    .mapNotNull { it.personnummer }
                    .map { personidentService.hentAktør(it) }
            } else {
                emptyList()
            }
        // barn som allerede finnes i behandling
        val barnSomErLagtTil =
            personopplysningGrunnlagService.hentBarna(behandling.id)?.map { it.aktør }
                ?: throw Feil("Barn finnes ikke for behandling ${behandling.id}")

        return (barnFraSøknad + barnSomErLagtTil).distinctBy { it.aktørId }
    }

    private fun List<YtelsePerson>.writeValueAsString(): String = objectMapper.writeValueAsString(this)

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatService::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
