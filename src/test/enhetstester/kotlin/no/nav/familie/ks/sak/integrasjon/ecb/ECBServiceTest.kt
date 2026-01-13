package no.nav.familie.ks.sak.integrasjon.ecb

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.ecb.domene.ECBValutakursCache
import no.nav.familie.ks.sak.integrasjon.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.ECBValutakursRestKlient
import no.nav.familie.valutakurs.domene.ecb.ECBValutakursData
import no.nav.familie.valutakurs.domene.ecb.Frequency
import no.nav.familie.valutakurs.domene.ecb.toExchangeRates
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateDate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateKey
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateValue
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesForCurrency
import no.nav.familie.valutakurs.exception.IngenValutakursException
import no.nav.familie.valutakurs.exception.ValutakursException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBServiceTest {
    private val ecbClient = mockk<ECBValutakursRestKlient>()
    private val evbValutakursCacheRepository = mockk<ECBValutakursCacheRepository>()

    private val ecbService = ECBService(ecbClient, evbValutakursCacheRepository)

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Hent valutakurs for utenlandsk valuta til NOK og sjekk at beregning av kurs er riktig`() {
        val valutakursDato = LocalDate.of(2022, 6, 28)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(10.337)), Pair("SEK", BigDecimal.valueOf(10.6543))),
                valutakursDato.toString(),
            )

        every { evbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns null
        every { evbValutakursCacheRepository.save(any()) } returns ECBValutakursCache(kurs = BigDecimal.valueOf(10.6543), valutakode = "SEK", valutakursdato = valutakursDato)
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        val sekTilNOKValutakurs = ecbService.hentValutakurs("SEK", valutakursDato)
        assertEquals(BigDecimal.valueOf(0.9702185972), sekTilNOKValutakurs)
    }

    @Test
    fun `Test at ECBService kaster ECBServiceException dersom de returnerte kursene ikke inneholder kurs for forespurt valuta`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(10.337))),
                valutakursDato.toString(),
            )
        every { evbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns null
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService kaster FunksjonellFeil dersom ValutakursRestClient kaster IngenValutakursException`() {
        val valutakursDato = LocalDate.of(2024, 3, 29)
        every { evbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns null
        every { ecbClient.hentValutakurs(any(), any(), any()) } throws IngenValutakursException(message = "Fant ikke valutakurser", cause = null)

        val feil = assertThrows<FunksjonellFeil> { ecbService.hentValutakurs("SEK", valutakursDato) }
        assertThat(feil.frontendFeilmelding).startsWith("Fant ikke valutakurser")
    }

    @Test
    fun `Test at ECBService kaster ECBServiceException dersom ValutakursRestClient kaster ValutakursException`() {
        val valutakursDato = LocalDate.of(2024, 3, 29)
        every { evbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns null
        every { ecbClient.hentValutakurs(any(), any(), any()) } throws ValutakursException(message = "En feil har skjedd", cause = null)

        val feil = assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
        assertThat(feil.message).startsWith("En feil har skjedd")
    }

    @Test
    fun `Test at ECBService kaster ECBServiceException dersom de returnerte kursene ikke inneholder kurser med forespurt dato`() {
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(10.337)), Pair("SEK", BigDecimal.valueOf(10.6543))),
                valutakursDato.minusDays(1).toString(),
            )
        every { evbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns null
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService returnerer NOK til EUR dersom den forespurte valutaen er EUR`() {
        val nokTilEur = BigDecimal.valueOf(9.4567)
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(9.4567))),
                valutakursDato.toString(),
            )
        every { evbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns null
        every { evbValutakursCacheRepository.save(any()) } returns ECBValutakursCache(kurs = BigDecimal.valueOf(9.4567), valutakode = "EUR", valutakursdato = valutakursDato)
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "EUR"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertEquals(nokTilEur, ecbService.hentValutakurs("EUR", valutakursDato))
    }

    private fun createECBResponse(
        frequency: Frequency,
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): ECBValutakursData =
        ECBValutakursData(
            SDMXExchangeRatesDataSet(
                exchangeRates.map {
                    SDMXExchangeRatesForCurrency(
                        listOf(
                            SDMXExchangeRateKey("CURRENCY", it.first),
                            SDMXExchangeRateKey("FREQ", frequency.toFrequencyParam()),
                        ),
                        listOf(),
                        listOf(
                            SDMXExchangeRate(
                                SDMXExchangeRateDate(exchangeRateDate),
                                SDMXExchangeRateValue((it.second)),
                            ),
                        ),
                    )
                },
            ),
        )
}
