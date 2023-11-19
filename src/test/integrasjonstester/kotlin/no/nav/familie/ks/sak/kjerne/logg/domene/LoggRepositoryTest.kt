package no.nav.familie.ks.sak.kjerne.logg.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggType
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

internal class LoggRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var loggRepository: LoggRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `hentLoggForBehandling - skal returnere logg som er lagret for behandling`() {
        val logg =
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "${behandling.type.visningsnavn} opprettet",
                rolle = BehandlerRolle.SYSTEM,
            )

        loggRepository.saveAndFlush(logg)

        val hentetLogg = loggRepository.hentLoggForBehandling(behandling.id)

        assertThat(hentetLogg.size, Is(1))
        assertThat(hentetLogg.single().behandlingId, Is(behandling.id))
        assertThat(hentetLogg.single().type, Is(LoggType.BEHANDLING_OPPRETTET))
    }
}
