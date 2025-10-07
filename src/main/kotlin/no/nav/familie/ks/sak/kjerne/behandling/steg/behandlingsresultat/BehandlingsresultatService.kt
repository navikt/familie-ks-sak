package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.ClockProvider
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatUtils.skalUtledeSøknadsresultatForBehandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BehandlingsresultatService(
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personidentService: PersonidentService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val kompetanseService: KompetanseService,
    private val clockProvider: ClockProvider,
) {
    internal fun utledBehandlingsresultat(behandlingId: Long): Behandlingsresultat {
        val behandling = behandlingService.hentBehandling(behandlingId)

        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)

        val søknadGrunnlag = søknadGrunnlagService.finnAktiv(behandlingId = behandling.id)
        val søknadDto = søknadGrunnlag?.tilSøknadDto()

        val forrigeAndelerTilkjentYtelse = forrigeBehandling?.let { andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = it.id) } ?: emptyList()
        val andelerTilkjentYtelse = andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val forrigeEndretUtbetalingAndeler = forrigeBehandling?.let { endretUtbetalingAndelService.hentEndredeUtbetalingAndeler(behandlingId = it.id) } ?: emptyList()
        val endretUtbetalingAndeler = endretUtbetalingAndelService.hentEndredeUtbetalingAndeler(behandlingId = behandlingId)

        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)

        val personerIForrigeBehandling = forrigeBehandling?.let { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = forrigeBehandling.id).personer.toSet() } ?: emptySet()

        val forrigeVilkårsvurdering = forrigeBehandling?.id?.let { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = it) }
        val forrigePersonResultat = forrigeVilkårsvurdering?.personResultater ?: emptySet()

        val personerFremstiltKravFor =
            finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDto = søknadDto,
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
                    nåværendePersonResultater = nåværendePersonResultat,
                    forrigePersonResultater = forrigePersonResultat,
                    nåværendeKompetanser = kompetanser.toList(),
                    forrigeKompetanser = forrigeKompetanser.toList(),
                    personerFremstiltKravFor = personerFremstiltKravFor,
                    personerIForrigeBehandling = personerIForrigeBehandling,
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
                nåMåned = YearMonth.now(clockProvider.get()),
            )

        // KOMBINER
        val behandlingsresultat = BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(søknadsresultat, endringsresultat, opphørsresultat)

        return behandlingsresultat
    }

    internal fun finnPersonerFremstiltKravFor(
        behandling: Behandling,
        søknadDto: SøknadDto?,
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
}
