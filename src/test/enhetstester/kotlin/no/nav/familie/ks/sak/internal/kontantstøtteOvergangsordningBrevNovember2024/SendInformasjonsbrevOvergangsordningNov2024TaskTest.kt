package no.nav.familie.ks.sak.internal.kontantstøtteOvergangsordningBrevNovember2024

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SendInformasjonsbrevOvergangsordningNov2024TaskTest {
    private val mockBrevService = mockk<BrevService>()
    private val mockFagsakService = mockk<FagsakService>()
    private val mockPersonopplysningerService = mockk<PersonopplysningerService>()
    private val mockTaskService = mockk<TaskService>()

    private val sendInformasjonsbrevOvergangsordningNov2024Task =
        SendInformasjonsbrevOvergangsordningNov2024Task(
            brevService = mockBrevService,
            fagsakService = mockFagsakService,
            personopplysningerService = mockPersonopplysningerService,
            taskService = mockTaskService,
        )

    @Test
    fun `skal kaste feil dersom det allerede har blitt sendt brev på fagsak`() {
        // Arrange
        val task =
            Task(
                payload = "1",
                type = SendInformasjonsbrevOvergangsordningNov2024Task.TASK_STEP_TYPE,
            )

        every {
            mockTaskService.finnAlleTaskerMedPayloadOgType(
                payload = "1",
                type = SendInformasjonsbrevOvergangsordningNov2024Task.TASK_STEP_TYPE,
            )
        } returns listOf(mockk(), mockk())

        // Act & Assert
        val feilmelding =
            assertThrows<Feil> {
                sendInformasjonsbrevOvergangsordningNov2024Task.doTask(task)
            }

        assertThat(feilmelding.message).isEqualTo("Det finnes flere tasker med samme payload og type på fagsak 1")
    }

    @Test
    fun `skal kaste feil dersom det ikke er navn på person`() {
        // Arrange
        val task =
            Task(
                payload = "1",
                type = SendInformasjonsbrevOvergangsordningNov2024Task.TASK_STEP_TYPE,
            )

        val mockFagsak = lagFagsak(id = 1)
        val mockPdlPersonInfo =
            mockk<PdlPersonInfo>().apply {
                every { navn } returns null
            }

        every { mockFagsakService.hentFagsak(1L) } returns mockFagsak
        every { mockPersonopplysningerService.hentPersoninfoEnkel(mockFagsak.aktør) } returns mockPdlPersonInfo

        every {
            mockTaskService.finnAlleTaskerMedPayloadOgType(
                payload = "1",
                type = SendInformasjonsbrevOvergangsordningNov2024Task.TASK_STEP_TYPE,
            )
        } returns listOf(task)

        // Act & Assert
        val feilmelding =
            assertThrows<Feil> {
                sendInformasjonsbrevOvergangsordningNov2024Task.doTask(task)
            }

        assertThat(feilmelding.message).isEqualTo("Fant ikke navn på person i fagsak 1")
    }

    @Test
    fun `skal sende brev til person med riktig brevmal`() {
        // Arrange
        val task =
            Task(
                payload = "1",
                type = SendInformasjonsbrevOvergangsordningNov2024Task.TASK_STEP_TYPE,
            )

        val fagsak = lagFagsak(id = 1)
        val mockPdlPersonInfo =
            mockk<PdlPersonInfo>().apply {
                every { navn } returns "Test"
            }

        val slotManueltBrevDto = slot<ManueltBrevDto>()

        every { mockFagsakService.hentFagsak(1L) } returns fagsak
        every { mockPersonopplysningerService.hentPersoninfoEnkel(fagsak.aktør) } returns mockPdlPersonInfo
        every {
            mockBrevService.sendBrev(
                fagsak = any(),
                manueltBrevDto = capture(slotManueltBrevDto),
            )
        } just runs

        every {
            mockTaskService.finnAlleTaskerMedPayloadOgType(
                payload = "1",
                type = SendInformasjonsbrevOvergangsordningNov2024Task.TASK_STEP_TYPE,
            )
        } returns listOf(task)

        // Act
        sendInformasjonsbrevOvergangsordningNov2024Task.doTask(task)

        // Assert
        val capturedManueltBrevDto = slotManueltBrevDto.captured

        assertThat(capturedManueltBrevDto.brevmal).isEqualTo(Brevmal.INFORMASJONSBREV_OVERGANGSORDNING_NOVEMBER_2024)
        assertThat(capturedManueltBrevDto.mottakerNavn).isEqualTo("Test")
        assertThat(capturedManueltBrevDto.mottakerIdent).isEqualTo(fagsak.aktør.aktivFødselsnummer())
    }
}
