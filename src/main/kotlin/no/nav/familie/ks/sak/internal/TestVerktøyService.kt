package no.nav.familie.ks.sak.internal

import no.nav.familie.ba.sak.internal.vedtak.begrunnelser.lagBrevTest
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service

@Service
class TestVerktøyService(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårsvurderingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingRepository: EndretUtbetalingAndelRepository,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakService: VedtakService,
    private val kompetanseRepository: KompetanseRepository,
) {
    fun hentBrevTest(behandlingId: Long): String {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val forrigeBehandling =
            behandlingService.hentBehandlingerPåFagsak(behandling.fagsak.id)
                .filter { it.erAvsluttet() }.filter { !it.erHenlagt() }.filter { it.opprettetTidspunkt < behandling.opprettetTidspunkt }
                .maxByOrNull { it.opprettetTidspunkt }

        val persongrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!
        val persongrunnlagForrigeBehandling =
            forrigeBehandling?.let { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)!! }

        val personResultater = vilkårService.hentAktivVilkårsvurderingForBehandling(behandlingId).personResultater
        val personResultaterForrigeBehandling =
            forrigeBehandling?.let { vilkårService.hentAktivVilkårsvurderingForBehandling(it.id).personResultater }

        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        val andelerForrigeBehandling =
            forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }

        val endredeUtbetalinger = endretUtbetalingRepository.hentEndretUtbetalingerForBehandling(behandlingId)
        val endredeUtbetalingerForrigeBehandling =
            forrigeBehandling?.let { endretUtbetalingRepository.hentEndretUtbetalingerForBehandling(it.id) }

        val kompetanse = kompetanseRepository.findByBehandlingId(behandlingId)
        val kompetanseForrigeBehandling =
            forrigeBehandling?.let { kompetanseRepository.findByBehandlingId(it.id) }

        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtakService.hentVedtak(behandlingId))

        return lagBrevTest(
            behandling = behandling,
            forrigeBehandling = forrigeBehandling,
            persongrunnlag = persongrunnlag,
            persongrunnlagForrigeBehandling = persongrunnlagForrigeBehandling,
            personResultater = personResultater,
            personResultaterForrigeBehandling = personResultaterForrigeBehandling,
            andeler = andeler,
            andelerForrigeBehandling = andelerForrigeBehandling,
            vedtaksperioder = vedtaksperioder,
            endredeUtbetalinger = endredeUtbetalinger,
            endredeUtbetalingerForrigeBehandling = endredeUtbetalingerForrigeBehandling,
            kompetanse = kompetanse,
            kompetanseForrigeBehandling = kompetanseForrigeBehandling,
        )
    }
}
