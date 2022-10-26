package no.nav.familie.ks.sak.kjerne.totrinnskontroll

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

class TotrinnskontrollRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling()
    }

    @Test
    fun `findByBehandlingAndAktiv - skal returnere null dersom det ikke finnes noe aktiv totrinnskontroll for behandling`() {
        val totrinnskontroll =
            Totrinnskontroll(behandling = behandling, aktiv = false, saksbehandler = "test", saksbehandlerId = "testId")

        totrinnskontrollRepository.saveAndFlush(totrinnskontroll)

        val hentetTotrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id)

        assertThat(hentetTotrinnskontroll, Is(nullValue()))
    }

    @Test
    fun `findByBehandlingAndAktiv - skal returnere totrinnskontroll dersom det finnes aktiv en for behandling`() {
        val totrinnskontroll =
            Totrinnskontroll(behandling = behandling, aktiv = true, saksbehandler = "test", saksbehandlerId = "testId")

        totrinnskontrollRepository.saveAndFlush(totrinnskontroll)

        val hentetTotrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id).shouldNotBeNull()

        assertThat(hentetTotrinnskontroll.behandling, Is(behandling))
    }
}