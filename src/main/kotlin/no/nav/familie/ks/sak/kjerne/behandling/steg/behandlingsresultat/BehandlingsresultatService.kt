package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.convertDataClassToJson
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
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
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService
) {

    fun utledBehandlingsresultat(behandling: Behandling): Behandlingsresultat {
        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val forrigeAndelerMedEndringer = forrigeBehandling?.let {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(it.id)
        } ?: emptyList()

        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        val personerFremstiltKravFor = hentBarna(behandling)

        val behandlingsresultatPersoner = lagBehandlingsresulatPersoner(
            behandling,
            personerFremstiltKravFor,
            andelerMedEndringer,
            forrigeAndelerMedEndringer,
            vilkårsvurdering
        )
        secureLogger.info("Behandlingsresultatpersoner: ${behandlingsresultatPersoner.convertDataClassToJson()}")

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(
            behandlingsresultatPersoner = behandlingsresultatPersoner,
            uregistrerteBarn = søknadGrunnlagService.finnAktiv(behandling.id)?.hentUregistrerteBarn()?.map { it.ident }
                ?: emptyList()
        )

        val alleAndelerHar0IUtbetaling = andelerMedEndringer.all { it.kalkulertUtbetalingsbeløp == 0 }

        YtelsePersonUtils.validerYtelsePersoner(ytelsePersonerMedResultat)

        secureLogger.info("Utledet YtelsePersonResultat på behandling $behandling: $ytelsePersonerMedResultat")

        vilkårsvurderingService.oppdater(
            vilkårsvurdering.also {
                it.ytelsePersoner = ytelsePersonerMedResultat.writeValueAsString()
            }
        )

        val ytelsePersonResultater =
            YtelsePersonUtils.oppdaterYtelsePersonResultaterVedOpphør(ytelsePersonerMedResultat)
        val behandlingsresultat =
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersonResulater(
                ytelsePersonResultater,
                alleAndelerHar0IUtbetaling
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
        vilkårsvurdering: Vilkårsvurdering
    ) =
        personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
            .personer.filter { it.type == PersonType.BARN }.map {
                val harEksplisittAvslagIVilkårsvurderingen =
                    vilkårsvurdering.personResultater.find { personResultat -> personResultat.aktør == it.aktør }
                        ?.harEksplisittAvslag() ?: false

                val harEksplisittAvslagIEndreteUtbetalinger =
                    andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                        behandling.id
                    ).any { endretUtbetalingAndel ->
                        endretUtbetalingAndel.erEksplisittAvslagPåSøknad == true && endretUtbetalingAndel.person?.aktør == it.aktør
                    }

                BehandlingsresultatUtils.utledBehandlingsresultatDataForPerson(
                    person = it,
                    personerFremstiltKravFor = personerFremslitKravFor,
                    andelerMedEndringer = andelerMedEndringer,
                    forrigeAndelerMedEndringer = forrigeAndelerMedEndringer,
                    erEksplisittAvslag = harEksplisittAvslagIVilkårsvurderingen || harEksplisittAvslagIEndreteUtbetalinger
                )
            }

    private fun hentBarna(behandling: Behandling): List<Aktør> {
        // Søknad kan ha flere barn som er inkludert i søknaden og folkeregistert, men ikke i behandling
        val barnFraSøknad = if (behandling.erSøknad()) {
            søknadGrunnlagService.hentAktiv(behandling.id).tilSøknadDto()
                .barnaMedOpplysninger.filter { it.inkludertISøknaden && it.erFolkeregistrert }
                .map { personidentService.hentAktør(it.ident) }
        } else emptyList()
        // barn som allerede finnes i behandling
        val barnSomErLagtTil = personopplysningGrunnlagService.hentBarna(behandling.id)?.map { it.aktør }
            ?: throw Feil("Barn finnes ikke for behandling ${behandling.id}")

        return (barnFraSøknad + barnSomErLagtTil).distinctBy { it.aktørId }
    }

    private fun List<YtelsePerson>.writeValueAsString(): String = objectMapper.writeValueAsString(this)

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatService::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
