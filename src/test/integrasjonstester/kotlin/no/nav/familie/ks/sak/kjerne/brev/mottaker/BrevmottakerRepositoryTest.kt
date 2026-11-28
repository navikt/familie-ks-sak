package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.datagenerator.lagBrevmottakerDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class BrevmottakerRepositoryTest(
    @Autowired private val brevmottakerRepository: BrevmottakerRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : OppslagSpringRunnerTest() {
    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling()
    }

    @Nested
    inner class FinnBrevmottakerePåBehandling {
        @Test
        fun `skal returnere brevmottakere sortert på opprettet tidspunkt selv om id-rekkefølgen er en annen`() {
            // Arrange
            val brevmottakerMedLavestId = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.FULLMEKTIG))
            val brevmottakerMedHøyestId = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.VERGE))

            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", LocalDateTime.now(), brevmottakerMedLavestId.id)
            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", LocalDateTime.now().minusDays(1), brevmottakerMedHøyestId.id)

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottakerMedHøyestId.id, brevmottakerMedLavestId.id)
        }

        @Test
        fun `skal returnere brevmottakere sortert på id når opprettet tidspunkt er likt`() {
            // Arrange
            val opprettetTidspunkt = LocalDateTime.now()
            val brevmottaker1 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.FULLMEKTIG))
            val brevmottaker2 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.VERGE))

            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", opprettetTidspunkt, brevmottaker1.id)
            jdbcTemplate.update("UPDATE brevmottaker SET opprettet_tid = ? WHERE id = ?", opprettetTidspunkt, brevmottaker2.id)

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottaker1.id, brevmottaker2.id)
        }

        @Test
        fun `skal returnere brevmottakere i rekkefølgen de ble opprettet`() {
            // Arrange
            val brevmottaker1 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.FULLMEKTIG))
            val brevmottaker2 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.VERGE))
            val brevmottaker3 = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.DØDSBO))

            // Oppdateringen gjør at Postgres normalt returnerer raden sist uten ORDER BY.
            // Ikke garantert (f.eks. ved indeksskanning etter HOT-oppdatering), men gjør testen mer treffsikker.
            brevmottakerRepository.saveAndFlush(brevmottaker1.also { it.navn = "Oppdatert navn" })

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottaker1.id, brevmottaker2.id, brevmottaker3.id)
        }

        @Test
        fun `skal kun returnere brevmottakere for gitt behandling`() {
            // Arrange
            val annenFagsak = lagreFagsak(lagFagsak(aktør = lagreAktør(randomAktør())))
            val annenBehandling = lagreBehandling(lagBehandling(fagsak = annenFagsak))

            val brevmottaker = brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = behandling.id))
            brevmottakerRepository.saveAndFlush(lagBrevmottakerDb(behandlingId = annenBehandling.id))

            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere.map { it.id }).containsExactly(brevmottaker.id)
        }

        @Test
        fun `skal returnere tom liste når behandlingen ikke har brevmottakere`() {
            // Act
            val brevmottakere = brevmottakerRepository.finnBrevmottakerePåBehandling(behandling.id)

            // Assert
            assertThat(brevmottakere).isEmpty()
        }
    }
}
