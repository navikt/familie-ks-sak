package no.nav.familie.ks.sak.bisys

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.api.dto.InfotrygdPeriode
import no.nav.familie.ks.sak.api.dto.KsSakPeriode
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.infotrygd.BarnDto
import no.nav.familie.ks.sak.integrasjon.infotrygd.Foedselsnummer
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.integrasjon.infotrygd.InnsynResponse
import no.nav.familie.ks.sak.integrasjon.infotrygd.StonadDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class BisysServiceTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @InjectMockKs
    private lateinit var bisysService: BisysService

    private val barn1IKsSak = randomFnr()
    private val barn2IInfotrygd = randomFnr()
    private val barn1Aktør = randomAktør(fnr = barn1IKsSak)
    private val barn2Aktør = randomAktør(fnr = barn2IInfotrygd)
    private val barnIdenter = listOf(barn1IKsSak, barn2IInfotrygd)

    private val fagsak = lagFagsak()
    private val behandling = lagBehandling(fagsak = fagsak)

    @BeforeEach
    fun setup() {
        every { personidentService.hentAktør(barn1IKsSak) } returns barn1Aktør
        every { personidentService.hentAktør(barn2IInfotrygd) } returns barn2Aktør
        every { fagsakService.hentFagsakerPåPerson(barn1Aktør) } returns listOf(fagsak)
        every { fagsakService.hentFagsakerPåPerson(barn2Aktør) } returns emptyList()
        every { behandlingService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling

        val andelerTilkjentYtelse1 = lagAndelTilkjentYtelse(
            behandling = behandling,
            aktør = barn1Aktør,
            stønadFom = YearMonth.now().minusMonths(5),
            stønadTom = YearMonth.now().minusMonths(3),
            sats = 7500
        )
        val andelerTilkjentYtelse2 = lagAndelTilkjentYtelse(
            behandling = behandling,
            aktør = barn1Aktør,
            stønadFom = YearMonth.now().minusMonths(2),
            stønadTom = YearMonth.now().plusMonths(3),
            sats = 4500
        )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns listOf(
            AndelTilkjentYtelseMedEndreteUtbetalinger(andelerTilkjentYtelse1, emptyList()),
            AndelTilkjentYtelseMedEndreteUtbetalinger(andelerTilkjentYtelse2, emptyList())
        )
    }

    @Test
    fun `hentUtbetalingsinfo skal hente utbetalingsinfo fra både ks-sak og infotrygd`() {
        every {
            infotrygdReplikaClient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        } returns InnsynResponse(
            data = listOf(
                StonadDto(
                    fnr = Foedselsnummer(randomFnr()),
                    fom = YearMonth.now().minusMonths(5),
                    tom = YearMonth.now().plusMonths(3),
                    belop = 7500,
                    barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd)))
                ),
                StonadDto(
                    fnr = Foedselsnummer(randomFnr()),
                    fom = YearMonth.now().minusMonths(7),
                    tom = YearMonth.now().plusMonths(5),
                    belop = 4500,
                    barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd)))
                )
            )
        )

        val utbetalinger = bisysService.hentUtbetalingsinfo(LocalDate.now().minusMonths(32), barnIdenter)
        assertTrue { utbetalinger.infotrygdPerioder.isNotEmpty() }
        assertTrue { utbetalinger.infotrygdPerioder.size == 2 }

        val utbetalingerFraKsSak = utbetalinger.ksSakPerioder
        assertEquals(2, utbetalingerFraKsSak.size)
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().minusMonths(3)
        )
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(2),
            tomMåned = YearMonth.now().plusMonths(3)
        )

        val utbetalingerFraInfotrygd = utbetalinger.infotrygdPerioder
        assertEquals(2, utbetalingerFraInfotrygd.size)
        utbetalingerFraInfotrygd.assertInfotrygdUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().plusMonths(3)
        )
        utbetalingerFraInfotrygd.assertInfotrygdUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(7),
            tomMåned = YearMonth.now().plusMonths(5)
        )
    }

    @Test
    fun `hentUtbetalingsinfo skal hente utbetalingsinfo kun fra ks-sak`() {
        every {
            infotrygdReplikaClient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        } returns InnsynResponse(data = emptyList())

        val utbetalinger = bisysService.hentUtbetalingsinfo(LocalDate.now().minusMonths(32), barnIdenter)
        assertTrue { utbetalinger.infotrygdPerioder.isNullOrEmpty() }
        assertTrue { utbetalinger.ksSakPerioder.size == 2 }

        val utbetalingerFraKsSak = utbetalinger.ksSakPerioder
        assertEquals(2, utbetalingerFraKsSak.size)
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().minusMonths(3)
        )
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(2),
            tomMåned = YearMonth.now().plusMonths(3)
        )
    }

    @Test
    fun `hentUtbetalingsinfo skal hente utbetalingsinfo fra både ks-sak og infotrygd og filtrere på fomdato`() {
        every {
            infotrygdReplikaClient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        } returns InnsynResponse(
            data = listOf(
                StonadDto(
                    fnr = Foedselsnummer(randomFnr()),
                    fom = YearMonth.now().minusMonths(5),
                    tom = YearMonth.now().plusMonths(3),
                    belop = 7500,
                    barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd)))
                ),
                StonadDto(
                    fnr = Foedselsnummer(randomFnr()),
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(9),
                    belop = 4500,
                    barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd)))
                )
            )
        )

        val utbetalinger = bisysService.hentUtbetalingsinfo(LocalDate.now().minusMonths(4), barnIdenter)
        assertTrue { utbetalinger.infotrygdPerioder.isNotEmpty() }
        assertTrue { utbetalinger.infotrygdPerioder.size == 1 }

        val utbetalingerFraKsSak = utbetalinger.ksSakPerioder
        assertEquals(2, utbetalingerFraKsSak.size)
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().minusMonths(3)
        )
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(2),
            tomMåned = YearMonth.now().plusMonths(3)
        )

        val utbetalingerFraInfotrygd = utbetalinger.infotrygdPerioder
        assertEquals(1, utbetalingerFraInfotrygd.size)
        utbetalingerFraInfotrygd.assertInfotrygdUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().plusMonths(3)
        )
    }

    private fun List<KsSakPeriode>.assertUtbetaling(beløp: Int, fomMåned: YearMonth, tomMåned: YearMonth) =
        assertTrue {
            this.any {
                it.barn.beløp == beløp &&
                    it.fomMåned == fomMåned &&
                    it.tomMåned == tomMåned
            }
        }

    private fun List<InfotrygdPeriode>.assertInfotrygdUtbetaling(beløp: Int, fomMåned: YearMonth, tomMåned: YearMonth) =
        assertTrue {
            this.any {
                it.beløp == beløp &&
                    it.fomMåned == fomMåned &&
                    it.tomMåned == tomMåned
            }
        }
}
