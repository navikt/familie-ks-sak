package no.nav.familie.ks.sak.kjerne.logg.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggType
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

internal class LoggRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var loggRepository: LoggRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun beforeEach() {
        fagsak = lagreFagsak()
        behandling = lagreBehandling(fagsak)
    }

    @Test
    fun `hentLoggForBehandling - skal returnere logg som er lagret for behandling`() {
        val logg = Logg(
            behandlingId = behandling.id,
            type = LoggType.BEHANDLING_OPPRETTET,
            tittel = "${behandling.type.visningsnavn} opprettet",
            rolle = BehandlerRolle.SYSTEM
        )

        loggRepository.saveAndFlush(logg)

        val hentetLogg = loggRepository.hentLoggForBehandling(behandling.id)

        assertThat(hentetLogg.size, Is(1))
        assertThat(hentetLogg.single().behandlingId, Is(behandling.id))
        assertThat(hentetLogg.single().type, Is(LoggType.BEHANDLING_OPPRETTET))
    }

    private fun lagreFagsak(): Fagsak {
        val aktør = aktørRepository.saveAndFlush(randomAktør())
        return fagsakRepository.saveAndFlush(lagFagsak(aktør))
    }

    private fun lagreBehandling(fagsak: Fagsak): Behandling = behandlingRepository.saveAndFlush(lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD))
}
