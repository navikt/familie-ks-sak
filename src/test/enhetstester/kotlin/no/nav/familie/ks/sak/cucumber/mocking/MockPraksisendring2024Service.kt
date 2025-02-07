package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.kjerne.praksisendring.Praksisendring2024Service

fun mockPraksisendring2024Service(): Praksisendring2024Service =
    mockk<Praksisendring2024Service>().apply {
        every { genererAndelerForPraksisendring2024(any(), any(), any()) } returns emptyList()
    }
