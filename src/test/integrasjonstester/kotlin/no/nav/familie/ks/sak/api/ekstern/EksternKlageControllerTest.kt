package no.nav.familie.ks.sak.no.nav.familie.ks.sak.api.ekstern

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilgangskontroll.FagsakTilgang
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.ekstern.EksternKlageController
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EksternKlageControllerTest(
    @Autowired private val aktørRepository: AktørRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val klageService: KlageService,
    @Autowired private val opprettBehandlingService: OpprettBehandlingService,
) : OppslagSpringRunnerTest() {
    private val tilgangService = mockk<TilgangService>()
    private val eksternKlageController =
        EksternKlageController(
            tilgangService = tilgangService,
            opprettBehandlingService = opprettBehandlingService,
            klageService = klageService,
        )

    @Nested
    inner class HarTilgangTilFagsak {
        @Test
        fun `skal returnere true dersom tilgang til fagsak`() {
            // Arrange
            val aktør = aktørRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))

            every { tilgangService.validerTilgangTilFagsak(fagsak.id, any()) } just runs

            // Act
            val response: Ressurs<FagsakTilgang> = eksternKlageController.hentTilgangTilFagsak(fagsak.id)

            // Assert
            assertThat(response.data?.harTilgang).isTrue()
        }

        @Test
        fun `skal returnere false dersom ikke tilgang til fagsak`() {
            // Arrange
            val aktør = aktørRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))

            every { tilgangService.validerTilgangTilFagsak(fagsak.id, any()) } throws RolleTilgangskontrollFeil("Ingen tilgang")

            // Act
            val response: Ressurs<FagsakTilgang> = eksternKlageController.hentTilgangTilFagsak(fagsak.id)

            // Assert
            assertThat(response.data?.harTilgang).isFalse()
        }
    }
}
