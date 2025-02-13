package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.eøs.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.junit.jupiter.api.Test

class RegistrerPersongrunnlagEnhetTest {
    private val behandlingRepository: BehandlingRepository = mockk()
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk(relaxed = true)
    private val vilkårsvurderingService: VilkårsvurderingService = mockk(relaxed = true)
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService = mockk(relaxed = true)
    private val overgangsordningAndelService: OvergangsordningAndelService = mockk(relaxed = true)
    private val kompetanseService: KompetanseService = mockk(relaxed = true)
    private val valutakursService: ValutakursService = mockk(relaxed = true)
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService = mockk(relaxed = true)
    private val adopsjonService: AdopsjonService = mockk(relaxed = true)

    private val registrerPersongrunnlagSteg =
        RegistrerPersonGrunnlagSteg(
            behandlingRepository = behandlingRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            overgangsordningAndelService = overgangsordningAndelService,
            eøsSkjemaerForNyBehandlingService =
                EøsSkjemaerForNyBehandlingService(
                    kompetanseService = kompetanseService,
                    utenlandskPeriodebeløpService = utenlandskPeriodebeløpService,
                    valutakursService = valutakursService,
                ),
            adopsjonService = adopsjonService,
        )

    @Test
    fun `Kopierer kompetanser, valutakurser og utenlandsk periodebeløp til ny behandling`() {
        val sisteVedtatteBehandling = lagBehandling(status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)
        val behandling2 = lagBehandling()

        every { behandlingRepository.hentBehandling(behandling2.id) } returns behandling2
        every { behandlingRepository.finnBehandlinger(behandling2.fagsak.id) } returns listOf(sisteVedtatteBehandling)

        registrerPersongrunnlagSteg.utførSteg(behandlingId = behandling2.id)

        verify(exactly = 1) {
            kompetanseService.kopierOgErstattKompetanser(
                BehandlingId(sisteVedtatteBehandling.id),
                BehandlingId(behandling2.id),
            )
            valutakursService.kopierOgErstattValutakurser(
                BehandlingId(sisteVedtatteBehandling.id),
                BehandlingId(behandling2.id),
            )
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                BehandlingId(sisteVedtatteBehandling.id),
                BehandlingId(behandling2.id),
            )
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling = behandling2,
                forrigeBehandling = sisteVedtatteBehandling,
            )
            overgangsordningAndelService.kopierOvergangsordningAndelFraForrigeBehandling(
                behandling = behandling2,
                forrigeBehandling = sisteVedtatteBehandling,
            )
        }
    }

    @Test
    fun `Skal ikke kopierer kompetanser, valutakurser og utenlandsk periodebeløp for man ikke har noen siste behandling er henlagt`() {
        val sisteVedtatteBehandling = lagBehandling(status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET)
        val behandling2 = lagBehandling()

        every { behandlingRepository.hentBehandling(behandling2.id) } returns behandling2
        every { behandlingRepository.finnBehandlinger(behandling2.fagsak.id) } returns listOf(sisteVedtatteBehandling)

        registrerPersongrunnlagSteg.utførSteg(behandlingId = behandling2.id)

        verify(exactly = 0) {
            kompetanseService.kopierOgErstattKompetanser(
                BehandlingId(sisteVedtatteBehandling.id),
                BehandlingId(behandling2.id),
            )
            valutakursService.kopierOgErstattValutakurser(
                BehandlingId(sisteVedtatteBehandling.id),
                BehandlingId(behandling2.id),
            )
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                BehandlingId(sisteVedtatteBehandling.id),
                BehandlingId(behandling2.id),
            )
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling = behandling2,
                forrigeBehandling = sisteVedtatteBehandling,
            )
            overgangsordningAndelService.kopierOvergangsordningAndelFraForrigeBehandling(
                behandling = behandling2,
                forrigeBehandling = sisteVedtatteBehandling,
            )
        }
    }
}
