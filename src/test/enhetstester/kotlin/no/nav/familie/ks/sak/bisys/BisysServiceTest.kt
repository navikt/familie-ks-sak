package no.nav.familie.ks.sak.bisys

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.api.dto.InfotrygdPeriode
import no.nav.familie.ks.sak.api.dto.KsSakPeriode
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.infotrygd.BarnDto
import no.nav.familie.ks.sak.integrasjon.infotrygd.Foedselsnummer
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaKlient
import no.nav.familie.ks.sak.integrasjon.infotrygd.InnsynResponse
import no.nav.familie.ks.sak.integrasjon.infotrygd.StonadDto
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.person.pdl.aktor.v2.Type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class BisysServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val personidentService = mockk<PersonidentService>()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    private val infotrygdReplikaKlient = mockk<InfotrygdReplikaKlient>()

    private val bisysService =
        BisysService(
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            personidentService = personidentService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            infotrygdReplikaKlient = infotrygdReplikaKlient,
        )

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
        every { personidentService.hentIdenter(barn1IKsSak, any()) } answers { listOf(PdlIdent(barn1IKsSak, false, Type.FOLKEREGISTERIDENT.name)) }
        every { personidentService.hentIdenter(barn2IInfotrygd, any()) } answers { listOf(PdlIdent(barn2IInfotrygd, false, Type.FOLKEREGISTERIDENT.name)) }
        every { fagsakService.hentFagsakerPåPerson(barn1Aktør) } returns listOf(fagsak)
        every { fagsakService.hentFagsakerPåPerson(barn2Aktør) } returns emptyList()
        every { behandlingService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling

        val andelerTilkjentYtelse1 =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn1Aktør,
                stønadFom = YearMonth.now().minusMonths(5),
                stønadTom = YearMonth.now().minusMonths(3),
                sats = 7500,
            )
        val andelerTilkjentYtelse2 =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn1Aktør,
                stønadFom = YearMonth.now().minusMonths(2),
                stønadTom = YearMonth.now().plusMonths(3),
                sats = 4500,
            )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns
            listOf(
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelerTilkjentYtelse1, emptyList()),
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelerTilkjentYtelse2, emptyList()),
            )
    }

    @Test
    fun `hentUtbetalingsinfo skal hente utbetalingsinfo fra både ks-sak og infotrygd`() {
        every {
            infotrygdReplikaKlient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        } returns
            InnsynResponse(
                data =
                    listOf(
                        StonadDto(
                            fnr = Foedselsnummer(randomFnr()),
                            fom = YearMonth.now().minusMonths(5),
                            tom = YearMonth.now().plusMonths(3),
                            belop = 7500,
                            barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd))),
                        ),
                        StonadDto(
                            fnr = Foedselsnummer(randomFnr()),
                            fom = YearMonth.now().minusMonths(7),
                            tom = YearMonth.now().plusMonths(5),
                            belop = 4500,
                            barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd))),
                        ),
                    ),
            )

        val utbetalinger = bisysService.hentUtbetalingsinfo(LocalDate.now().minusMonths(32), barnIdenter)
        assertTrue { utbetalinger.infotrygdPerioder.isNotEmpty() }
        assertTrue { utbetalinger.infotrygdPerioder.size == 2 }

        val utbetalingerFraKsSak = utbetalinger.ksSakPerioder
        assertEquals(2, utbetalingerFraKsSak.size)
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().minusMonths(3),
        )
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(2),
            tomMåned = YearMonth.now().plusMonths(3),
        )

        val utbetalingerFraInfotrygd = utbetalinger.infotrygdPerioder
        assertEquals(2, utbetalingerFraInfotrygd.size)
        utbetalingerFraInfotrygd.assertInfotrygdUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().plusMonths(3),
        )
        utbetalingerFraInfotrygd.assertInfotrygdUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(7),
            tomMåned = YearMonth.now().plusMonths(5),
        )
    }

    @Test
    fun `hentUtbetalingsinfo skal hente utbetalingsinfo kun fra ks-sak`() {
        every {
            infotrygdReplikaKlient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        } returns InnsynResponse(data = emptyList())

        val utbetalinger = bisysService.hentUtbetalingsinfo(LocalDate.now().minusMonths(32), barnIdenter)
        assertTrue { utbetalinger.infotrygdPerioder.isNullOrEmpty() }
        assertTrue { utbetalinger.ksSakPerioder.size == 2 }

        val utbetalingerFraKsSak = utbetalinger.ksSakPerioder
        assertEquals(2, utbetalingerFraKsSak.size)
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().minusMonths(3),
        )
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(2),
            tomMåned = YearMonth.now().plusMonths(3),
        )
    }

    @Test
    fun `hentUtbetalingsinfo skal hente utbetalingsinfo fra både ks-sak og infotrygd og filtrere på fomdato`() {
        every {
            infotrygdReplikaKlient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        } returns
            InnsynResponse(
                data =
                    listOf(
                        StonadDto(
                            fnr = Foedselsnummer(randomFnr()),
                            fom = YearMonth.now().minusMonths(5),
                            tom = YearMonth.now().plusMonths(3),
                            belop = 7500,
                            barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd))),
                        ),
                        StonadDto(
                            fnr = Foedselsnummer(randomFnr()),
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().minusMonths(9),
                            belop = 4500,
                            barn = listOf(BarnDto(fnr = Foedselsnummer(barn2IInfotrygd))),
                        ),
                    ),
            )

        val utbetalinger = bisysService.hentUtbetalingsinfo(LocalDate.now().minusMonths(4), barnIdenter)
        assertTrue { utbetalinger.infotrygdPerioder.isNotEmpty() }
        assertTrue { utbetalinger.infotrygdPerioder.size == 1 }

        val utbetalingerFraKsSak = utbetalinger.ksSakPerioder
        assertEquals(2, utbetalingerFraKsSak.size)
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().minusMonths(3),
        )
        utbetalingerFraKsSak.assertUtbetaling(
            beløp = 4500,
            fomMåned = YearMonth.now().minusMonths(2),
            tomMåned = YearMonth.now().plusMonths(3),
        )

        val utbetalingerFraInfotrygd = utbetalinger.infotrygdPerioder
        assertEquals(1, utbetalingerFraInfotrygd.size)
        utbetalingerFraInfotrygd.assertInfotrygdUtbetaling(
            beløp = 7500,
            fomMåned = YearMonth.now().minusMonths(5),
            tomMåned = YearMonth.now().plusMonths(3),
        )
    }

    @Test
    fun `hentUtbetalingsinfo filtrerer vekk identer med bare NPID`() {
        every {
            infotrygdReplikaKlient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        } returns InnsynResponse(data = emptyList())
        every { personidentService.hentIdenter(any(), false) } returns listOf(PdlIdent("NPID", false, Type.NPID.name))

        val utbetalinger = bisysService.hentUtbetalingsinfo(LocalDate.now().minusMonths(32), listOf("NPID"))
        assertTrue { utbetalinger.infotrygdPerioder.isEmpty() }
        assertTrue { utbetalinger.ksSakPerioder.isEmpty() }
    }

    private fun List<KsSakPeriode>.assertUtbetaling(
        beløp: Int,
        fomMåned: YearMonth,
        tomMåned: YearMonth,
    ) = assertTrue {
        this.any {
            it.barn.beløp == beløp &&
                it.fomMåned == fomMåned &&
                it.tomMåned == tomMåned
        }
    }

    private fun List<InfotrygdPeriode>.assertInfotrygdUtbetaling(
        beløp: Int,
        fomMåned: YearMonth,
        tomMåned: YearMonth,
    ) = assertTrue {
        this.any {
            it.beløp == beløp &&
                it.fomMåned == fomMåned &&
                it.tomMåned == tomMåned
        }
    }
}
