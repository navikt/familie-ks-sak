package no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ArbeidsfordelingPåBehandlingRepositoryTest(
    @Autowired private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) : OppslagSpringRunnerTest() {
    @Nested
    inner class HentArbeidsfordelingPåBehandlingTest {
        @Test
        fun `skal hente arbeidsfordeling på behandling`() {
            // Arrange
            val søker = opprettOgLagreSøker()
            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søker))
            val behandling = opprettOgLagreBehandling(lagBehandling(fagsak = fagsak))

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandling.id,
                )

            arbeidsfordelingPåBehandlingRepository.save(arbeidsfordelingPåBehandling)

            // Act
            val lagretArbeidsfordelingPåBehandling =
                arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandling.id)

            // Assert
            assertThat(lagretArbeidsfordelingPåBehandling).isEqualTo(arbeidsfordelingPåBehandling)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling på behandling returnerer null`() {
            // Act & assert
            val exception =
                assertThrows<Feil> {
                    arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(1L)
                }
            assertThat(exception.message).isEqualTo("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling 1")
        }
    }
}
