import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService

fun mockAdopsjonService(): AdopsjonService =
    mockk<AdopsjonService>().apply {
        every { hentAlleAdopsjonerForBehandling(any()) } returns emptyList()
        every { finnAdopsjonForAktørIBehandling(any(), any()) } returns null
    }
