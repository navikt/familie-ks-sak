package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeggTilBarnService(
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val beregningService: BeregningService,
) {
    @Transactional
    fun leggTilBarn(
        behandlingId: Long,
        nyttBarnIdent: String,
    ) {
        val behandling = behandlingService.hentAktivtBehandling(behandlingId)

        // oppdater personopplysninggrunnlag med nytt barn og opprett historikkinnslag
        personopplysningGrunnlagService.leggTilBarnIPersonopplysningGrunnlagOgOpprettLogg(behandling, nyttBarnIdent)

        // opprett vilkårsvurdering på nytt
        vilkårsvurderingService.opprettVilkårsvurdering(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id),
        )

        // slett tilkjent ytelse og sett behandling steg til vilkårsvurdering
        beregningService.slettTilkjentYtelseForBehandling(behandling)
        tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingId)
    }
}
