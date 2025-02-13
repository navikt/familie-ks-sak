package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.adopsjon

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class AdopsjonRepositoryTest(
    @Autowired private val adopsjonRepository: AdopsjonRepository,
) : OppslagSpringRunnerTest() {
    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `skal kaste feil hvis det forsøkes å lagre to adopsjoner på samme aktør i samme behandling `() {
        val adopsjon =
            Adopsjon(
                aktør = barn,
                behandlingId = behandling.id,
                adopsjonsdato = LocalDate.now().minusYears(1),
            )

        val adopsjon2 =
            Adopsjon(
                aktør = barn,
                behandlingId = behandling.id,
                adopsjonsdato = LocalDate.now(),
            )

        adopsjonRepository.saveAndFlush(adopsjon)

        assertThrows<DataIntegrityViolationException> {
            adopsjonRepository.saveAndFlush(adopsjon2)
        }
    }
}
