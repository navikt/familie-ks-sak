package no.nav.familie.ks.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@TestConfiguration
class ØkonomiTestConfig {
    @Bean
    @Profile("mock-økonomi")
    @Primary
    fun mockØkonomiKlient(): OppdragKlient {
        val økonomiKlient: OppdragKlient = mockk(relaxed = true)

        clearØkonomiMocks(økonomiKlient)

        return økonomiKlient
    }

    companion object {
        fun clearØkonomiMocks(økonomiKlient: OppdragKlient) {
            clearMocks(økonomiKlient)

            val iverksettRespons = "Mocksvar fra Økonomi-klient"
            every { økonomiKlient.iverksettOppdrag(any()) } returns iverksettRespons

            val hentStatusRespons = OppdragStatus.KVITTERT_OK

            every { økonomiKlient.hentStatus(any()) } returns hentStatusRespons

            every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)
        }
    }
}

val simulertPosteringMock =
    listOf(
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
            fom = LocalDate.now().withDayOfMonth(1),
            tom = LocalDate.now().plusMonths(7).sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 7500.0.toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.parse("2024-05-10"),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
    )

val simuleringMottakerMock =
    listOf(
        SimuleringMottaker(
            simulertPostering = simulertPosteringMock,
            mottakerType = MottakerType.BRUKER,
            mottakerNummer = "12345678910",
        ),
    )
