package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagToTrinnskontroll
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ks.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OpprettGrunnlagOgSignaturDataServiceTest {
    private val mockedPersonopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    private val mockedTotrinnskontrollService: TotrinnskontrollService = mockk()
    private val mockedArbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val mockedSaksbehandlerContext: SaksbehandlerContext = mockk()
    private val opprettGrunnlagOgSignaturDataService: OpprettGrunnlagOgSignaturDataService =
        OpprettGrunnlagOgSignaturDataService(
            personopplysningGrunnlagService = mockedPersonopplysningGrunnlagService,
            totrinnskontrollService = mockedTotrinnskontrollService,
            arbeidsfordelingService = mockedArbeidsfordelingService,
            saksbehandlerContext = mockedSaksbehandlerContext,
        )

    @Nested
    inner class OpprettGrunnlagOgSignaturData {
        @Test
        fun `skal bruke saksbehandler og beslutter fra godkjent totrinnskontroll`() {
            // Arrange
            val vedtak = lagVedtak()
            val behandling = vedtak.behandling

            val totrinnskontroll =
                lagToTrinnskontroll(
                    behandling = vedtak.behandling,
                    saksbehandler = "Saksbehandler Navn",
                    beslutter = "Beslutter Navn",
                    godkjent = true,
                )

            every { mockedPersonopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns lagPersonopplysningGrunnlag(behandlingId = behandling.id)
            every { mockedArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            every { mockedTotrinnskontrollService.finnAktivForBehandling(behandlingId = vedtak.behandling.id) } returns totrinnskontroll
            every { mockedSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Innlogget Saksbehandler"

            // Act
            val result = opprettGrunnlagOgSignaturDataService.opprett(vedtak)

            // Assert
            assertThat(result.saksbehandler).isEqualTo("Saksbehandler Navn")
            assertThat(result.beslutter).isEqualTo("Beslutter Navn")
            assertThat(result.enhet).isEqualTo("Test enhet")
        }

        @Test
        fun `skal bruke SYSTEM_NAVN for automatisk behandling`() {
            // Arrange
            val behandling =
                lagBehandling(
                    opprettetÅrsak = BehandlingÅrsak.LOVENDRING_2024,
                )
            val vedtak = lagVedtak(behandling = behandling)

            every { mockedPersonopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns lagPersonopplysningGrunnlag(behandlingId = behandling.id)
            every { mockedArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            every { mockedTotrinnskontrollService.finnAktivForBehandling(behandlingId = behandling.id) } returns null
            every { mockedSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Innlogget Saksbehandler"

            // Act
            val result = opprettGrunnlagOgSignaturDataService.opprett(vedtak)

            // Assert
            assertThat(result.saksbehandler).isEqualTo(SikkerhetContext.SYSTEM_NAVN)
            assertThat(result.beslutter).isEqualTo(SikkerhetContext.SYSTEM_NAVN)
        }

        @Test
        fun `skal bruke innlogget saksbehandler og Beslutter når steg er før BESLUTTE_VEDTAK`() {
            // Arrange
            val behandling =
                lagBehandling(
                    lagBehandlingStegTilstander = {
                        setOf(
                            BehandlingStegTilstand(
                                behandling = it,
                                behandlingSteg = BehandlingSteg.VEDTAK,
                                behandlingStegStatus = BehandlingStegStatus.KLAR,
                            ),
                        )
                    },
                )
            val vedtak = lagVedtak(behandling = behandling)

            val totrinnskontroll =
                lagToTrinnskontroll(
                    behandling = behandling,
                    godkjent = false,
                )

            every { mockedPersonopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns lagPersonopplysningGrunnlag(behandlingId = behandling.id)
            every { mockedArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            every { mockedTotrinnskontrollService.finnAktivForBehandling(behandlingId = behandling.id) } returns totrinnskontroll
            every { mockedSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Innlogget Saksbehandler"

            // Act
            val result = opprettGrunnlagOgSignaturDataService.opprett(vedtak)

            // Assert
            assertThat(result.saksbehandler).isEqualTo("Innlogget Saksbehandler")
            assertThat(result.beslutter).isEqualTo("Beslutter")
        }

        @Test
        fun `skal bruke saksbehandler fra totrinnskontroll og innlogget bruker som beslutter når de er forskjellige i BESLUTTE_VEDTAK steg`() {
            // Arrange
            val behandling =
                lagBehandling(
                    lagBehandlingStegTilstander = {
                        setOf(
                            BehandlingStegTilstand(
                                behandling = it,
                                behandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK,
                                behandlingStegStatus = BehandlingStegStatus.KLAR,
                            ),
                        )
                    },
                )
            val vedtak = lagVedtak(behandling = behandling)

            val totrinnskontroll =
                lagToTrinnskontroll(
                    behandling = behandling,
                    saksbehandler = "Opprinnelig Saksbehandler",
                    godkjent = false,
                )

            every { mockedPersonopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns lagPersonopplysningGrunnlag(behandlingId = behandling.id)
            every { mockedArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            every { mockedTotrinnskontrollService.finnAktivForBehandling(behandlingId = behandling.id) } returns totrinnskontroll
            every { mockedSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Beslutter Person"

            // Act
            val result = opprettGrunnlagOgSignaturDataService.opprett(vedtak)

            // Assert
            assertThat(result.saksbehandler).isEqualTo("Opprinnelig Saksbehandler")
            assertThat(result.beslutter).isEqualTo("Beslutter Person")
        }

        @Test
        fun `skal bruke Beslutter som placeholder når innlogget bruker er samme som saksbehandler i BESLUTTE_VEDTAK steg`() {
            // Arrange
            val behandling =
                lagBehandling(
                    lagBehandlingStegTilstander = {
                        setOf(
                            BehandlingStegTilstand(
                                behandling = it,
                                behandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK,
                                behandlingStegStatus = BehandlingStegStatus.KLAR,
                            ),
                        )
                    },
                )
            val vedtak = lagVedtak(behandling = behandling)

            val totrinnskontroll =
                lagToTrinnskontroll(
                    behandling = behandling,
                    saksbehandler = "Samme Person",
                    godkjent = false,
                )

            every { mockedPersonopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns lagPersonopplysningGrunnlag(behandlingId = behandling.id)
            every { mockedArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            every { mockedTotrinnskontrollService.finnAktivForBehandling(behandlingId = behandling.id) } returns totrinnskontroll
            every { mockedSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Samme Person"

            // Act
            val result = opprettGrunnlagOgSignaturDataService.opprett(vedtak)

            // Assert
            assertThat(result.saksbehandler).isEqualTo("Samme Person")
            assertThat(result.beslutter).isEqualTo("Beslutter")
        }

        @Test
        fun `skal kaste Feil når behandling er i et steg etter BESLUTTE_VEDTAK, behandling er ikke automatisk og totrinnskontroll er ikke godkjent`() {
            // Arrange
            val behandling =
                lagBehandling(
                    lagBehandlingStegTilstander = {
                        setOf(
                            BehandlingStegTilstand(
                                behandling = it,
                                behandlingSteg = BehandlingSteg.AVSLUTT_BEHANDLING,
                                behandlingStegStatus = BehandlingStegStatus.KLAR,
                            ),
                        )
                    },
                )
            val vedtak = lagVedtak(behandling = behandling)
            every { mockedPersonopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns lagPersonopplysningGrunnlag(behandlingId = behandling.id)
            every { mockedArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            every { mockedTotrinnskontrollService.finnAktivForBehandling(behandlingId = behandling.id) } returns
                Totrinnskontroll(
                    behandling = behandling,
                    saksbehandler = "Saksbehandler Navn",
                    godkjent = false,
                    saksbehandlerId = "SAK",
                )
            every { mockedSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "Innlogget Saksbehandler"

            // Act & Assert
            assertThatThrownBy { opprettGrunnlagOgSignaturDataService.opprett(vedtak) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("Kunne ikke utlede signatur for behandling")
        }
    }
}
