import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient

fun mockIntegrasjonClient(): IntegrasjonClient =
    mockk<IntegrasjonClient>().apply {
        every { sjekkErEgenAnsatt(any()) } answers {
            val personIdenter = firstArg<Set<String>>()
            personIdenter.associateWith { false }
        }
    }
