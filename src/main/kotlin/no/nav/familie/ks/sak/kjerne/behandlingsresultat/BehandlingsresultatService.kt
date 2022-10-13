package no.nav.familie.ks.sak.kjerne.behandlingsresultat

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandlingsresultatService(
    private val behandlingService: BehandlingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personidentService: PersonidentService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {

    fun utledBehandlingsresultat(behandlingId: Long): Behandlingsresultat {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)
        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val forrigeAndelerMedEndringer = forrigeBehandling?.let {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(it.id)
        } ?: emptyList()

        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil("Finner ikke aktiv vilkårsvurdering")

        val personerFremstiltKravFor =
            hentPersonerFramstiltKravFor(
                behandling = behandling,
                søknadDTO = søknadGrunnlagService.finnAktiv(behandlingId = behandlingId)?.tilSøknadDto(),
                forrigeBehandling = forrigeBehandling
            )
        val grunnlag = persongrunnlagService.hentAktivPersonopplysningGrunnlag(behandling.id)
        val behandlingsresultatPersoner = grunnlag?.personer?.map {
            BehandlingsresultatUtils.utledBehandlingsresultatDataForPerson(
                person = it,
                personerFremstiltKravFor = personerFremstiltKravFor,
                andelerFraForrigeTilkjentYtelse = forrigeAndelerMedEndringer,
                andelerTilkjentYtelse = andelerMedEndringer,
                erEksplisittAvslag = vilkårsvurdering.personResultater.find { personResultat -> personResultat.aktør == it.aktør }
                    ?.vilkårResultater?.find { vilkårResultat -> vilkårResultat.erEksplisittAvslagPåSøknad == true }
                    ?.erEksplisittAvslagPåSøknad
                    ?: false
            )
        } ?: emptyList()

        secureLogger.info(
            "Behandlingsresultatpersoner: $behandlingsresultatPersoner"
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(
            behandlingsresultatPersoner = behandlingsresultatPersoner
        )

        validerYtelsePersoner(behandling, ytelsePersoner = ytelsePersonerMedResultat)

        val behandlingsresultat =
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)
        secureLogger.info("Resultater fra vilkårsvurdering på behandling $behandling: $ytelsePersonerMedResultat")
        logger.info("Resultat fra vilkårsvurdering på behandling $behandling: $behandlingsresultat")

        return behandlingsresultat
    }

    private fun validerYtelsePersoner(behandling: Behandling, ytelsePersoner: List<YtelsePerson>) {
        val søkerAktør = persongrunnlagService.hentSøker(behandling.id)?.aktør
            ?: throw Feil("Fant ikke søker på behandling")

        if (ytelsePersoner.any { it.ytelseType == YtelseType.ORDINÆR_KONTANTSTØTTE && it.aktør == søkerAktør }) {
            throw Feil(
                "Søker kan ikke ha ytelsetype ordinær"
            )
        }
    }

    private fun hentPersonerFramstiltKravFor(
        behandling: Behandling,
        søknadDTO: SøknadDto? = null,
        forrigeBehandling: Behandling?
    ): List<Aktør> {
        val barnFraSøknad = søknadDTO?.barnaMedOpplysninger
            ?.filter { it.inkludertISøknaden && it.erFolkeregistrert }
            ?.map { personidentService.hentAktør(it.ident) }
            ?: emptyList()

        // hensol TODO trenger vi denne eller vil vi alltid finne barna i søknaden?
        // val nyeBarn = persongrunnlagService.finnNyeBarn(forrigeBehandling = forrigeBehandling, behandling = behandling)
        //    .map { it.aktør }

        return (
            barnFraSøknad // + nyeBarn
            ).distinct()
    }

    private fun List<YtelsePerson>.writeValueAsString(): String = objectMapper.writeValueAsString(this)

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatService::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
