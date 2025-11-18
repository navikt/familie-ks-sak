import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient

fun mockIntegrasjonKlient(): IntegrasjonKlient =
    mockk<IntegrasjonKlient>().apply {
        every { sjekkErEgenAnsatt(any()) } answers {
            val personIdenter = firstArg<Set<String>>()
            personIdenter.associateWith { false }
        }
    }
