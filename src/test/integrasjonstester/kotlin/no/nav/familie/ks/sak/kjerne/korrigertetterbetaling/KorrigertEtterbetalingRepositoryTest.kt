package no.nav.familie.ks.sak.kjerne.korrigertetterbetaling

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.hamcrest.CoreMatchers.`is` as Is

class KorrigertEtterbetalingRepositoryTest(
    @Autowired private val korrigertEtterbetalingRepository: KorrigertEtterbetalingRepository,
) : OppslagSpringRunnerTest() {
    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `finnAktivtKorrigeringPåBehandling skal returnere null dersom det ikke eksisterer en aktiv etterbetaling korrigering på behandling`() {
        val inaktivKorrigertEtterbetaling =
            KorrigertEtterbetaling(
                id = 10000001,
                årsak = KorrigertEtterbetalingÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
                begrunnelse = "Test på inaktiv korrigering",
                beløp = 1000,
                behandling = behandling,
                aktiv = false,
            )

        korrigertEtterbetalingRepository.saveAndFlush(inaktivKorrigertEtterbetaling)

        val ikkeEksisterendeKorrigertEtterbetaling =
            korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id)

        assertThat(ikkeEksisterendeKorrigertEtterbetaling, Is(nullValue()))
    }

    @Test
    fun `finnAktivtKorrigeringPåBehandling skal returnere aktiv korrigering på behandling dersom det finnes`() {
        val aktivKorrigertEtterbetaling =
            KorrigertEtterbetaling(
                id = 10000002,
                årsak = KorrigertEtterbetalingÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
                begrunnelse = "Test på aktiv korrigering",
                beløp = 1000,
                behandling = behandling,
                aktiv = true,
            )

        korrigertEtterbetalingRepository.saveAndFlush(aktivKorrigertEtterbetaling)

        val eksisterendeKorrigertEtterbetaling =
            korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id)!!

        assertThat(eksisterendeKorrigertEtterbetaling.begrunnelse, Is("Test på aktiv korrigering"))
        assertThat(eksisterendeKorrigertEtterbetaling.beløp, Is(1000))
    }

    @Test
    fun `Det skal kastes DataIntegrityViolationException dersom det forsøkes å lagre aktivt korrigering når det allerede finnes en`() {
        val aktivKorrigertEtterbetaling =
            KorrigertEtterbetaling(
                id = 10000007,
                årsak = KorrigertEtterbetalingÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
                begrunnelse = "Test på aktiv korrigering",
                beløp = 1000,
                behandling = behandling,
                aktiv = true,
            )

        val aktivKorrigertEtterbetaling2 =
            KorrigertEtterbetaling(
                id = 10000008,
                årsak = KorrigertEtterbetalingÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
                begrunnelse = "Test på aktiv korrigering",
                beløp = 1000,
                behandling = behandling,
                aktiv = true,
            )

        korrigertEtterbetalingRepository.saveAndFlush(aktivKorrigertEtterbetaling)

        assertThrows<DataIntegrityViolationException> {
            korrigertEtterbetalingRepository.saveAndFlush(aktivKorrigertEtterbetaling2)
        }
    }

    @Test
    fun `hentAlleKorrigeringPåBehandling skal returnere alle KorrigertEtterbetaling på behandling`() {
        val aktivKorrigertEtterbetaling =
            KorrigertEtterbetaling(
                id = 10000003,
                årsak = KorrigertEtterbetalingÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
                begrunnelse = "1",
                beløp = 1000,
                behandling = behandling,
                aktiv = true,
            )

        val inaktivKorrigertEtterbetaling =
            KorrigertEtterbetaling(
                id = 10000004,
                årsak = KorrigertEtterbetalingÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
                begrunnelse = "2",
                beløp = 1000,
                behandling = behandling,
                aktiv = false,
            )

        korrigertEtterbetalingRepository.saveAndFlush(aktivKorrigertEtterbetaling)
        korrigertEtterbetalingRepository.saveAndFlush(inaktivKorrigertEtterbetaling)

        val eksisterendeKorrigertEtterbetaling =
            korrigertEtterbetalingRepository.finnAlleKorrigeringerPåBehandling(behandling.id)

        assertThat(eksisterendeKorrigertEtterbetaling.size, Is(2))
        assertThat(eksisterendeKorrigertEtterbetaling.map { it.begrunnelse }, containsInAnyOrder("1", "2"))
    }
}
