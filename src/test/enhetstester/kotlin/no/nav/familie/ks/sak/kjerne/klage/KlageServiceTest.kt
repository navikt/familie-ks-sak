package no.nav.familie.ks.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.kontrakter.felles.enhet.Enhet
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagKlagebehandlingDto
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val klageClient = mockk<KlageClient>()
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtakService = mockk<VedtakService>()
    private val tilbakekrevingKlient = mockk<TilbakekrevingKlient>()
    private val klagebehandlingHenter = mockk<KlagebehandlingHenter>()

    private val klageService =
        KlageService(
            fagsakService = fagsakService,
            klageClient = klageClient,
            integrasjonClient = integrasjonClient,
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            tilbakekrevingKlient = tilbakekrevingKlient,
            klagebehandlingHenter = klagebehandlingHenter,
        )

    @Nested
    inner class HentKlagebehandlingerPåFagsak {
        @Test
        fun `skal hente alle klagebehandlinger på fagsak`() {
            // Arrange
            val fagsakId = 1L

            val klagebehandlinger =
                listOf(
                    lagKlagebehandlingDto(),
                    lagKlagebehandlingDto(),
                    lagKlagebehandlingDto(),
                )

            every { klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsakId) } returns klagebehandlinger

            // Act
            val resultat = klageService.hentKlagebehandlingerPåFagsak(fagsakId)

            // Assert
            assertThat(resultat).isEqualTo(klagebehandlinger)
        }
    }

    @Nested
    inner class HentForrigeVedtatteKlagebehandling {
        @Test
        fun `skal hente forrige vedtatte klagebehandling`() {
            // Arrange
            val behandling = lagBehandling()

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = LocalDateTime.now(),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            every { klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling) } returns klagebehandlingDto

            // Act
            val forrigeVedtatteKlagebehandling = klageService.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isEqualTo(klagebehandlingDto)
        }
    }

    @Nested
    inner class SettRiktigEnhetVedOpprettelseAvKlage {
        @Test
        fun `skal sette enheten til saksbehandlers enhet ved opprettelse av klagebehandling`() {
            // Arrange
            val fagsak = lagFagsak()
            val forventetEnhet = Enhet("1234", "testRiktig")
            val enhetSomIkkeSkalVelges = Enhet("5432", "testFeil")

            every { integrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(any()) } returns listOf(forventetEnhet, enhetSomIkkeSkalVelges)

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns UUID.randomUUID()

            // Act
            klageService.opprettKlage(fagsak, LocalDate.now())

            // Assert
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(forventetEnhet.enhetsnummer)
        }
    }
}
