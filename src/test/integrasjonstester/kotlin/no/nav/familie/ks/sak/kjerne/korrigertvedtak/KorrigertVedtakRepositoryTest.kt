package no.nav.familie.ks.sak.kjerne.korrigertvedtak

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtak
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class KorrigertVedtakRepositoryTest(
    @Autowired private val korrigertVedtakRepository: KorrigertVedtakRepository,
) : OppslagSpringRunnerTest() {
    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `finnAktivtKorrigertVedtakPåBehandling skal returnere null dersom det ikke eksisterer en aktiv korrigering av vedtak på behandling`() {
        // Arrange
        val inaktivKorrigertVedtak =
            KorrigertVedtak(
                vedtaksdato = LocalDate.now().minusDays(6),
                begrunnelse = "Test på inaktiv korrigering",
                behandling = behandling,
                aktiv = false,
            )

        korrigertVedtakRepository.saveAndFlush(inaktivKorrigertVedtak)

        // Act
        val ikkeEksisterendeKorrigertVedtak =
            korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id)

        // Assert
        Assertions.assertNull(ikkeEksisterendeKorrigertVedtak, "Skal ikke finnes aktiv korrigert vedtak på behandling")
    }

    @Test
    fun `finnAktivtKorrigertVedtakPåBehandling skal returnere aktiv korrigert vedtak når det eksisterer en aktiv korrigering av vedtak på behandling`() {
        // Arrange
        val aktivKorrigertVedtak =
            KorrigertVedtak(
                vedtaksdato = LocalDate.now().minusDays(6),
                begrunnelse = "Test på aktiv korrigering",
                behandling = behandling,
                aktiv = true,
            )

        korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak)

        // Act
        val eksisterendeKorrigertVedtak =
            korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id)

        // Assert
        Assertions.assertNotNull(
            eksisterendeKorrigertVedtak,
            "Skal finnes aktiv korrigert vedtak på behandling",
        )
    }

    @Test
    fun `Det skal kastes DataIntegrityViolationException dersom det forsøkes å lagre aktivt korrigert vedtak når det allerede finnes en`() {
        // Arrange
        val aktivKorrigertVedtak1 =
            KorrigertVedtak(
                begrunnelse = "Test på aktiv korrigering",
                vedtaksdato = LocalDate.now().minusDays(6),
                behandling = behandling,
                aktiv = true,
            )

        val aktivKorrigertVedtak2 =
            KorrigertVedtak(
                begrunnelse = "Test på aktiv korrigering",
                vedtaksdato = LocalDate.now().minusDays(3),
                behandling = behandling,
                aktiv = true,
            )

        korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak1)

        // Act & Assert
        assertThrows<DataIntegrityViolationException> {
            korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak2)
        }
    }
}
