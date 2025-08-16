import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient

fun mockIntegrasjonClient(): IntegrasjonClient =
    mockk<IntegrasjonClient>().apply {
        every { sjekkErEgenAnsattBulk(any()) } answers {
            val personIdenter = firstArg<List<String>>()
            personIdenter.associateWith { false }
        }
    }
