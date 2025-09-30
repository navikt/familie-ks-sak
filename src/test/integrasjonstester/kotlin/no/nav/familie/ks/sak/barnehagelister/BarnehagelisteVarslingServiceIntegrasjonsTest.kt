package no.nav.familie.ks.sak.barnehagelister

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.data.lagBarnehagebarn
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BarnehagelisteVarslingServiceIntegrasjonsTest(
    @Autowired private val barnehageBarnRepository: BarnehagebarnRepository,
) : OppslagSpringRunnerTest() {
    private val epostService = mockk<EpostService>()
    private val geografiHierarkiService = mockk<GeografiHierarkiService>()
    private val barnehagelisteVarslingService =
        BarnehagelisteVarslingService(barnehageBarnRepository, epostService, geografiHierarkiService)

    @BeforeEach
    fun setup() {
        clearMocks(epostService, geografiHierarkiService)
        every { epostService.sendEpostVarslingBarnehagelister(any(), any()) } just runs

        // Brukes når service bygger enhet -> (bydeler/kommuner) fra fylkene i JSON
        every { geografiHierarkiService.hentBydelerEllerKommunerPerKommuneMedNavn("03") } returns
            mapOf(
                "0302" to "Grünerløkka",
                "0303" to "Sagene",
            )
        every { geografiHierarkiService.hentBydelerEllerKommunerPerKommuneMedNavn("31") } returns
            mapOf(
                "3103" to "Moss",
            )
    }

    @Test
    fun `sendVarslingOmNyBarnehagelisteTilEnhet sender epost når en kommune sender inn for første gang ila én måned`() {
        // Arrange
        val barnehageBarnSendtIGår =
            lagBarnehagebarn(
                endretTidspunkt = LocalDateTime.now().minusDays(1),
                kommuneNr = "0301",
                kommuneNavn = "Gamle Oslo",
            )
        val barnehageBarnSendtIGårMedSammeKommuneSomIDag =
            lagBarnehagebarn(
                endretTidspunkt = LocalDateTime.now().minusDays(1),
                kommuneNr = "3103",
                kommuneNavn = "Moss",
            )
        val barnehagebarnSendtIlaSisteDøgnFraKommuneSomHarBlittSendtTidligere =
            lagBarnehagebarn(
                endretTidspunkt = LocalDateTime.now().minusHours(2),
                kommuneNr = "3103",
                kommuneNavn = "Moss",
            )
        val barnehageBarnSendtIlaSisteDøgnSagene =
            lagBarnehagebarn(
                endretTidspunkt = LocalDateTime.now().minusHours(2),
                kommuneNr = "0303",
                kommuneNavn = "Sagene",
            )
        val barnehageBarnSendtIlaSisteDøgnLøkka =
            lagBarnehagebarn(
                endretTidspunkt = LocalDateTime.now().minusHours(2),
                kommuneNr = "0302",
                kommuneNavn = "Grünerløkka",
            )
        barnehageBarnRepository.saveAll(
            listOf(
                barnehageBarnSendtIGår,
                barnehageBarnSendtIGårMedSammeKommuneSomIDag,
                barnehagebarnSendtIlaSisteDøgnFraKommuneSomHarBlittSendtTidligere,
                barnehageBarnSendtIlaSisteDøgnSagene,
                barnehageBarnSendtIlaSisteDøgnLøkka,
            ),
        )

        // Act
        barnehagelisteVarslingService.sendVarslingOmNyBarnehagelisteTilEnhet()

        // Assert
        verify(exactly = 1) {
            epostService.sendEpostVarslingBarnehagelister(
                BarnehagelisteVarslingService.`KONTAKT_E-POST_BERGEN`,
                setOf("Grünerløkka", "Sagene"),
            )
        }
        verify(exactly = 1) {
            epostService.sendEpostVarslingBarnehagelister(
                BarnehagelisteVarslingService.`KONTAKT_E-POST_VADSØ`,
                setOf("Moss"),
            )
        }
    }

    @Test
    fun `sendVarslingOmNyBarnehagelisteTilEnhet sender ikke epost hvis det ikke er blitt sendt inn noe ila det siste døgnet`() {
        // Arrange
        val barnehagebarnSendtForMerEnnEttDøgnSiden =
            lagBarnehagebarn(
                endretTidspunkt = LocalDateTime.now().minusDays(2),
                kommuneNr = "0301",
                kommuneNavn = "Gamle Oslo",
            )

        barnehageBarnRepository.saveAll(
            listOf(
                barnehagebarnSendtForMerEnnEttDøgnSiden,
            ),
        )

        // Act
        barnehagelisteVarslingService.sendVarslingOmNyBarnehagelisteTilEnhet()

        // Assert
        verify(exactly = 0) { epostService.sendEpostVarslingBarnehagelister(any(), any()) }
    }
}
