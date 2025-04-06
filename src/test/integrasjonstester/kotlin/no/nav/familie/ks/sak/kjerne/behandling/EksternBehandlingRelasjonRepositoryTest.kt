package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEksternBehandlingRelasjon
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class EksternBehandlingRelasjonRepositoryTest(
    @Autowired private val eksternBehandlingRelasjonRepository: EksternBehandlingRelasjonRepository,
) : OppslagSpringRunnerTest() {
    @Nested
    inner class FindAllByInternBehandlingId {
        @Test
        fun `skal finne alle eksterne behandling relasjoner for intern behandling id`() {
            // Arrange
            val søker = opprettOgLagreSøker()
            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søker))
            val behandling = opprettOgLagreBehandling(lagBehandling(fagsak = fagsak))

            val eksternBehandlingRelasjon1 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            val eksternBehandlingRelasjon2 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.TILBAKEKREVING,
                )

            eksternBehandlingRelasjonRepository.saveAll(
                listOf(
                    eksternBehandlingRelasjon1,
                    eksternBehandlingRelasjon2,
                ),
            )

            // Act
            val eksternBehandlingRelasjoner =
                eksternBehandlingRelasjonRepository.findAllByInternBehandlingId(
                    internBehandlingId = behandling.id,
                )

            // Assert
            assertThat(eksternBehandlingRelasjoner).hasSize(2)
            assertThat(eksternBehandlingRelasjoner).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.internBehandlingId).isEqualTo(behandling.id)
                assertThat(it.eksternBehandlingId).isEqualTo(eksternBehandlingRelasjon1.eksternBehandlingId)
                assertThat(it.eksternBehandlingFagsystem).isEqualTo(eksternBehandlingRelasjon1.eksternBehandlingFagsystem)
                assertThat(it.opprettetTidspunkt).isNotNull()
            }
            assertThat(eksternBehandlingRelasjoner).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.internBehandlingId).isEqualTo(behandling.id)
                assertThat(it.eksternBehandlingId).isEqualTo(eksternBehandlingRelasjon2.eksternBehandlingId)
                assertThat(it.eksternBehandlingFagsystem).isEqualTo(eksternBehandlingRelasjon2.eksternBehandlingFagsystem)
                assertThat(it.opprettetTidspunkt).isNotNull()
            }
        }

        @Test
        fun `skal returnere en tom liste om ingen ekstern behandling relasjon finnes for den etterspurte interne behandling iden`() {
            // Arrange
            val søker = opprettOgLagreSøker()
            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søker))
            val behandling1 =
                opprettOgLagreBehandling(
                    lagBehandling(
                        fagsak = fagsak,
                        aktiv = false,
                        resultat = Behandlingsresultat.INNVILGET,
                        status = BehandlingStatus.AVSLUTTET,
                    ),
                )
            val behandling2 =
                opprettOgLagreBehandling(
                    lagBehandling(
                        fagsak = fagsak,
                        aktiv = false,
                        resultat = Behandlingsresultat.INNVILGET,
                        status = BehandlingStatus.AVSLUTTET,
                    ),
                )

            val eksternBehandlingRelasjon1 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling2.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            val eksternBehandlingRelasjon2 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling2.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.TILBAKEKREVING,
                )

            eksternBehandlingRelasjonRepository.saveAll(
                listOf(
                    eksternBehandlingRelasjon1,
                    eksternBehandlingRelasjon2,
                ),
            )

            // Act
            val eksternBehandlingRelasjoner =
                eksternBehandlingRelasjonRepository.findAllByInternBehandlingId(
                    internBehandlingId = behandling1.id,
                )

            // Assert
            assertThat(eksternBehandlingRelasjoner).isEmpty()
        }
    }
}
