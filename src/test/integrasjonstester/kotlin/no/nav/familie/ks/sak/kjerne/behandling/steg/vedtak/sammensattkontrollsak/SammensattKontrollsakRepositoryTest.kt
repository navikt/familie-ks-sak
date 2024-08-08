package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SammensattKontrollsakRepositoryTest(
    @Autowired private val sammensattKontrollsakRepository: SammensattKontrollsakRepository,
) : OppslagSpringRunnerTest() {
    @Test
    fun `skal finne sammensatt kontrollsak for behandling`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()

        val lagretSammensattKontrollsak =
            SammensattKontrollsak(
                id = 0L,
                behandlingId = behandling.id,
                fritekst = "blabla",
            )

        sammensattKontrollsakRepository.save(lagretSammensattKontrollsak)

        // Act
        val sammensattKontrollsakForBehandling =
            sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(
                behandlingId = lagretSammensattKontrollsak.behandlingId,
            )

        // Assert
        assertThat(sammensattKontrollsakForBehandling).isNotNull()
        assertThat(sammensattKontrollsakForBehandling?.id).isEqualTo(lagretSammensattKontrollsak.id)
        assertThat(sammensattKontrollsakForBehandling?.behandlingId).isEqualTo(lagretSammensattKontrollsak.behandlingId)
        assertThat(sammensattKontrollsakForBehandling?.fritekst).isEqualTo(lagretSammensattKontrollsak.fritekst)
    }

    @Test
    fun `skal finne sammensatt kontrollsak`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()

        val lagretSammensattKontrollsak =
            SammensattKontrollsak(
                id = 0L,
                behandlingId = behandling.id,
                fritekst = "blabla",
            )

        sammensattKontrollsakRepository.save(lagretSammensattKontrollsak)

        // Act
        val funnetSammensattKontrollsak =
            sammensattKontrollsakRepository.finnSammensattKontrollsak(
                sammensattKontrollsakId = lagretSammensattKontrollsak.id,
            )

        // Assert
        assertThat(funnetSammensattKontrollsak).isNotNull()
        assertThat(funnetSammensattKontrollsak?.id).isEqualTo(lagretSammensattKontrollsak.id)
        assertThat(funnetSammensattKontrollsak?.behandlingId).isEqualTo(lagretSammensattKontrollsak.behandlingId)
        assertThat(funnetSammensattKontrollsak?.fritekst).isEqualTo(lagretSammensattKontrollsak.fritekst)
    }
}
