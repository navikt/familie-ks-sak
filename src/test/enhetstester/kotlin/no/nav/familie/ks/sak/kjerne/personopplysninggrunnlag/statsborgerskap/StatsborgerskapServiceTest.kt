package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.statsborgerskap

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagStatsborgerskap
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.Norden
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class StatsborgerskapServiceTest {
    private val integrasjonKlient = mockk<IntegrasjonKlient>()

    private val statsborgerskapService = StatsborgerskapService(integrasjonKlient)

    @BeforeEach
    fun setup() {
        every { integrasjonKlient.hentAlleEØSLand() } returns hentKodeverkLand()
    }

    @Test
    fun `hentLand skal hente returnere landNavn gitt landKode`() {
        every { integrasjonKlient.hentLand("NOR") } returns "Norge"

        val landNavn = statsborgerskapService.hentLand("NOR")

        assertEquals(landNavn, "Norge")
    }

    @Test
    fun `hentStatsborgerskapMedMedlemskap skal hente statsborgerskap for en nordisk statsborger`() {
        val statsborgerskap = lagStatsborgerskap("SWE")
        val statsborgerskapereMedMedlemskap =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap,
                lagPersonopplysningGrunnlag(111L, randomFnr()).søker,
            )
        assertTrue { statsborgerskapereMedMedlemskap.isNotEmpty() }
        assertEquals(1, statsborgerskapereMedMedlemskap.size)

        val statsborgerskapMedMedlemskap = statsborgerskapereMedMedlemskap.single()
        assertEquals(Medlemskap.NORDEN, statsborgerskapMedMedlemskap.medlemskap)
        assertEquals(Norden.SWE.name, statsborgerskapMedMedlemskap.landkode)
    }

    @Test
    fun `hentStatsborgerskapMedMedlemskap skal hente statsborgerskap for en eøs statsborger`() {
        val statsborgerskap = lagStatsborgerskap("POL")
        val statsborgerskapMedMedlemskap =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap,
                lagPersonopplysningGrunnlag(111L, randomFnr()).søker,
            )

        assertEquals(Medlemskap.EØS, statsborgerskapMedMedlemskap.last().medlemskap)
        assertEquals("POL", statsborgerskapMedMedlemskap.last().landkode)
    }

    @Test
    fun `hentStatsborgerskapMedMedlemskap skal evaluere britiske statsborgere med ukjent periode som tredjelandsborgere`() {
        val statsborgerStorbritanniaUtenPeriode =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        val grStatsborgerskapUtenPeriode =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerStorbritanniaUtenPeriode,
                person = lagPerson(aktør = randomAktør()),
            )
        assertEquals(1, grStatsborgerskapUtenPeriode.size)
        assertEquals(Medlemskap.TREDJELANDSBORGER, grStatsborgerskapUtenPeriode.single().medlemskap)
        assertTrue(grStatsborgerskapUtenPeriode.single().gjeldendeNå())
    }

    @Test
    fun `hentStatsborgerskapMedMedlemskap skal evaluere britiske statsborgere etter brexit som tredjelandsborgere`() {
        val statsborgerStorbritanniaMedPeriodeEtterBrexit =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = LocalDate.of(2022, 3, 1),
                gyldigTilOgMed = LocalDate.now(),
                bekreftelsesdato = null,
            )
        val grStatsborgerskapEtterBrexit =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerStorbritanniaMedPeriodeEtterBrexit,
                person = lagPerson(aktør = randomAktør()),
            )
        assertEquals(1, grStatsborgerskapEtterBrexit.size)
        assertEquals(Medlemskap.TREDJELANDSBORGER, grStatsborgerskapEtterBrexit.single().medlemskap)
        assertTrue(grStatsborgerskapEtterBrexit.single().gjeldendeNå())
    }

    @Test
    fun `hentStatsborgerskapMedMedlemskap skal evaluere britiske statsborgere under Brexit som først EØS, nå tredjelandsborgere`() {
        val datoFørBrexit = LocalDate.of(1989, 3, 1)
        val datoEtterBrexit = LocalDate.of(2020, 5, 1)

        val statsborgerStorbritanniaMedPeriodeUnderBrexit =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = datoFørBrexit,
                gyldigTilOgMed = datoEtterBrexit,
                bekreftelsesdato = null,
            )
        val grStatsborgerskapUnderBrexit =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerStorbritanniaMedPeriodeUnderBrexit,
                person = lagPerson(aktør = randomAktør()),
            )
        assertEquals(2, grStatsborgerskapUnderBrexit.size)
        assertEquals(datoFørBrexit, grStatsborgerskapUnderBrexit.first().gyldigPeriode?.fom)
        assertEquals(LocalDate.of(2009, Month.DECEMBER, 31), grStatsborgerskapUnderBrexit.first().gyldigPeriode?.tom)
        assertEquals(Medlemskap.EØS, grStatsborgerskapUnderBrexit.sortedBy { it.gyldigPeriode?.fom }.first().medlemskap)
        assertEquals(
            Medlemskap.TREDJELANDSBORGER,
            grStatsborgerskapUnderBrexit.sortedBy { it.gyldigPeriode?.fom }.last().medlemskap,
        )
    }

    internal fun hentKodeverkLand(): KodeverkDto {
        val fom1900 = LocalDate.of(1900, Month.JANUARY, 1)
        val fom1990 = LocalDate.of(1990, Month.JANUARY, 1)
        val fom2004 = LocalDate.of(2004, Month.JANUARY, 1)
        val tom2010 = LocalDate.of(2009, Month.DECEMBER, 31)
        val tom9999 = LocalDate.of(9999, Month.DECEMBER, 31)

        val beskrivelsePolen = BeskrivelseDto("POL", "")
        val betydningPolen = BetydningDto(fom2004, tom9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
        val beskrivelseTyskland = BeskrivelseDto("DEU", "")
        val betydningTyskland =
            BetydningDto(fom1900, tom9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
        val beskrivelseDanmark = BeskrivelseDto("DNK", "")
        val betydningDanmark =
            BetydningDto(fom1990, tom9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
        val beskrivelseUK = BeskrivelseDto("GBR", "")
        val betydningUK = BetydningDto(fom1900, tom2010, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK))

        return KodeverkDto(
            betydninger =
                mapOf(
                    "POL" to listOf(betydningPolen),
                    "DEU" to listOf(betydningTyskland),
                    "DNK" to listOf(betydningDanmark),
                    "GBR" to listOf(betydningUK),
                ),
        )
    }
}
