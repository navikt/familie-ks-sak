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
        val inaktivKorrigertVedtak =
            KorrigertVedtak(
                vedtaksdato = LocalDate.now().minusDays(6),
                begrunnelse = "Test på inaktiv korrigering",
                behandling = behandling,
                aktiv = false,
            )

        korrigertVedtakRepository.saveAndFlush(inaktivKorrigertVedtak)

        val ikkeEksisterendeKorrigertVedtak =
            korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id)

        Assertions.assertNull(ikkeEksisterendeKorrigertVedtak, "Skal ikke finnes aktiv korrigert vedtak på behandling")
    }

    @Test
    fun `finnAktivtKorrigertVedtakPåBehandling skal returnere aktiv korrigert vedtak når det eksisterer en aktiv korrigering av vedtak på behandling`() {
        val aktivKorrigertVedtak =
            KorrigertVedtak(
                vedtaksdato = LocalDate.now().minusDays(6),
                begrunnelse = "Test på aktiv korrigering",
                behandling = behandling,
                aktiv = true,
            )

        korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak)

        val eksisterendeKorrigertVedtak =
            korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id)

        Assertions.assertNotNull(
            eksisterendeKorrigertVedtak,
            "Skal finnes aktiv korrigert vedtak på behandling",
        )
    }

    @Test
    fun `Det skal kastes DataIntegrityViolationException dersom det forsøkes å lagre aktivt korrigert vedtak når det allerede finnes en`() {
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

        assertThrows<DataIntegrityViolationException> {
            korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak2)
        }
    }
}
