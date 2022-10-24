package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.statsborgerskap

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagStatsborgerskap
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class StatsborgerskapServiceTest {

    @MockK
    private lateinit var integrasjonClient: IntegrasjonClient

    @InjectMockKs
    private lateinit var statsborgerskapService: StatsborgerskapService

    @Test
    fun `hentLand skal hente returnere landNavn gitt landKode`() {
        every { integrasjonClient.hentLand("NOR") } returns "Norge"

        val landNavn = statsborgerskapService.hentLand("NOR")

        assertEquals(landNavn, "Norge")
    }

    @Test
    fun `hentStatsborgerskapMedMedlemskap skal hente statsborgerskap for en nordisk statsborger`() {
        val statsborgerskap = lagStatsborgerskap("SWE")
        val statsborgerskapereMedMedlemskap = statsborgerskapService.hentStatsborgerskapMedMedlemskap(
            statsborgerskap,
            lagPersonopplysningGrunnlag(111L, randomFnr()).søker
        )
        assertTrue { statsborgerskapereMedMedlemskap.isNotEmpty() }
        assertEquals(1, statsborgerskapereMedMedlemskap.size)

        val statsborgerskapMedMedlemskap = statsborgerskapereMedMedlemskap.single()
        assertEquals(Medlemskap.NORDEN, statsborgerskapMedMedlemskap.medlemskap)
        assertEquals(StatsborgerskapService.Norden.SWE.name, statsborgerskapMedMedlemskap.landkode)
    }

    @Test
    fun `hentStatsborgerskapMedMedlemskap skal hente statsborgerskap for en eøs statsborger`() {
        val statsborgerskap = lagStatsborgerskap("POR")
        assertTrue {
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap,
                lagPersonopplysningGrunnlag(111L, randomFnr()).søker
            ).isEmpty() // EØS er ikke implementert enda, derfor returnerer det tom liste
        }
    }
}
