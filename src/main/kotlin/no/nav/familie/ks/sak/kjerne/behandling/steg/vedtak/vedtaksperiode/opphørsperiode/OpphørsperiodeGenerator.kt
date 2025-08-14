package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.opphørsperiode

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringstidspunktService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Component

@Component
class OpphørsperiodeGenerator(
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val adopsjonService: AdopsjonService,
    private val endringstidspunktService: EndringstidspunktService,
) {
    fun genererOpphørsperioder(
        behandling: Behandling,
    ): List<Opphørsperiode> {
        if (behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) return emptyList()

        val iverksatteBehandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling =
            iverksatteBehandlinger
                .filter { it.aktivertTidspunkt.isBefore(behandling.aktivertTidspunkt) && it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }
                .maxByOrNull { it.aktivertTidspunkt }

        val forrigePersonopplysningGrunnlag =
            if (forrigeIverksatteBehandling != null) {
                personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId = forrigeIverksatteBehandling.id)
            } else {
                null
            }

        val forrigeAndelerMedEndringer =
            if (forrigeIverksatteBehandling != null) {
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                    forrigeIverksatteBehandling.id,
                )
            } else {
                emptyList()
            }

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId = behandling.id)
                ?: return emptyList()

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val vilkårsvurdering =
            vilkårsvurderingRepository.finnAktivForBehandling(behandling.id)
                ?: throw Feil("Fant ikke vilkårsvurdering på behandling $behandling")

        val adopsjonerIBehandling =
            adopsjonService.hentAlleAdopsjonerForBehandling(behandlingId = BehandlingId(behandling.id))

        val endringstidspunktForBehandling = endringstidspunktService.finnEndringstidspunktForBehandling(behandling)

        return mapTilOpphørsperioder(
            forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
            forrigeAndelerTilkjentYtelse = forrigeAndelerMedEndringer,
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vilkårsvurdering = vilkårsvurdering,
            adopsjonerIBehandling = adopsjonerIBehandling,
            endringstidspunktForBehandling = endringstidspunktForBehandling,
        )
    }
}
