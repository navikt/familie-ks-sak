package no.nav.familie.ks.sak.barnehagelister

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.kodeverk.Bydel
import no.nav.familie.kontrakter.felles.kodeverk.Fylke
import no.nav.familie.kontrakter.felles.kodeverk.HierarkiGeografiInnlandDto
import no.nav.familie.kontrakter.felles.kodeverk.Innland
import no.nav.familie.kontrakter.felles.kodeverk.Kommune
import no.nav.familie.kontrakter.felles.kodeverk.LandDto
import no.nav.familie.kontrakter.felles.kodeverk.NoderInnland
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.data.lagBarnehagebarn
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BarnehagelisteVarslingServiceIntegrasjonsTest(
    @Autowired private val barnehageBarnRepository: BarnehagebarnRepository,
) : OppslagSpringRunnerTest() {
    private val epostService = mockk<EpostService>()
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val barnehagelisteVarslingService = BarnehagelisteVarslingService(barnehageBarnRepository, epostService, integrasjonClient)

    @BeforeEach
    fun setup() {
        clearMocks(epostService)
        every { epostService.sendEpostVarslingBarnehagelister(any(), any()) } just runs
        every { integrasjonClient.hentFylkerOgKommuner() } returns geografiFixture()
    }

    private fun nb(label: String) = mapOf("nb" to label)

    private fun geografiFixture(): HierarkiGeografiInnlandDto =
        HierarkiGeografiInnlandDto(
            noder =
                NoderInnland(
                    innland =
                        Innland(
                            undernoder =
                                mapOf(
                                    "NOR" to
                                        LandDto(
                                            undernoder =
                                                mapOf(
                                                    // Oslo fylke (03)
                                                    "03" to
                                                        Fylke(
                                                            kode = "03",
                                                            tekster = nb("Oslo"),
                                                            termer = emptyMap(),
                                                            undernoder =
                                                                mapOf(
                                                                    // Oslo kommune (0301) med bydeler
                                                                    "0301" to
                                                                        Kommune(
                                                                            kode = "0301",
                                                                            tekster = nb("Oslo"),
                                                                            termer = emptyMap(),
                                                                            undernoder =
                                                                                mapOf(
                                                                                    "0302" to
                                                                                        Bydel(
                                                                                            kode = "0302",
                                                                                            tekster = nb("Grünerløkka"),
                                                                                            termer = emptyMap(),
                                                                                        ),
                                                                                    "0303" to
                                                                                        Bydel(
                                                                                            kode = "0303",
                                                                                            tekster = nb("Sagene"),
                                                                                            termer = emptyMap(),
                                                                                        ),
                                                                                ),
                                                                        ),
                                                                ),
                                                        ),
                                                    // Viken (31) – inneholder Moss (3103)
                                                    "31" to
                                                        Fylke(
                                                            kode = "31",
                                                            tekster = nb("Viken"),
                                                            termer = emptyMap(),
                                                            undernoder =
                                                                mapOf(
                                                                    "3103" to
                                                                        Kommune(
                                                                            kode = "3103",
                                                                            tekster = nb("Moss"),
                                                                            termer = emptyMap(),
                                                                            undernoder = null, // ingen bydeler
                                                                        ),
                                                                ),
                                                        ),
                                                ),
                                        ),
                                ),
                        ),
                ),
        )

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
