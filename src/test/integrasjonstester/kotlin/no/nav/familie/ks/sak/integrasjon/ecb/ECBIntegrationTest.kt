package no.nav.familie.ks.sak.integrasjon.ecb

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.DatabaseCleanupService
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBIntegrationTest : OppslagSpringRunnerTest() {
    private val ecbClient = mockk<ECBValutakursRestKlient>()

    @Autowired
    private lateinit var ecbService: ECBService

    @Autowired
    private lateinit var ecbValutakursCacheRepository: ECBValutakursCacheRepository

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @BeforeEach
    fun setUp() {
        ecbService =
            ECBService(
                ecbClient = ecbClient,
                ecbValutakursCacheRepository = ecbValutakursCacheRepository,
            )
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal teste at valutakurs hentes fra cache dersom valutakursen allerede er hentet fra ECB`() {
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(9.4567))),
                valutakursDato.toString(),
            )
        every {
            ecbClient.hentValutakurs(
                any(),
                any(),
                any(),
            )
        } returns ecbExchangeRatesData.toExchangeRates()

        ecbService.hentValutakurs("EUR", valutakursDato)
        val valutakurs = ecbValutakursCacheRepository.findByValutakodeAndValutakursdato("EUR", valutakursDato)
        assertEquals(valutakurs!!.kurs, BigDecimal.valueOf(9.4567))
        ecbService.hentValutakurs("EUR", valutakursDato)
        verify(exactly = 1) {
            ecbClient.hentValutakurs(
                any(),
                any(),
                any(),
            )
        }
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
