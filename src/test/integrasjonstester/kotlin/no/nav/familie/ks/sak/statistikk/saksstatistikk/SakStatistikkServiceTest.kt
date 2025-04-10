package no.nav.familie.ks.sak.no.nav.familie.ks.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.RegistrerPersonGrunnlagSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.statistikk.saksstatistikk.BehandlingStatistikkV2Dto
import no.nav.familie.ks.sak.statistikk.saksstatistikk.RelatertBehandlingUtleder
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SendBehandlinghendelseTilDvhV2Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class SakStatistikkServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var taskService: TaskService

    @MockkBean(relaxed = true)
    private lateinit var registerPersonGrunnlagSteg: RegistrerPersonGrunnlagSteg

    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var sakStatistikkService: SakStatistikkService

    @MockkBean
    private lateinit var relatertBehandlingUtleder: RelatertBehandlingUtleder

    @BeforeEach
    fun setup() {
        every { relatertBehandlingUtleder.utledRelatertBehandling(any()) } returns null
    }

    @Test
    fun `opprettSendingAvBehandlingensTilstand skal generere melding om ny tilstand til DVH`() {
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        stegService.utførSteg(behandling.id, BehandlingSteg.REGISTRERE_PERSONGRUNNLAG)
        assertEquals(
            1,
            taskService.findAll().count { it.type == SendBehandlinghendelseTilDvhV2Task.TASK_TYPE },
        )
    }

    @Test
    fun `ved sending av task skal teknisk tid være nyere enn funksjonell tid`() {
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        stegService.utførSteg(behandling.id, BehandlingSteg.REGISTRERE_PERSONGRUNNLAG)
        taskService.findAll().filter { it.type == SendBehandlinghendelseTilDvhV2Task.TASK_TYPE }.first().let {
            val behandlingStatistikkV1Dto: BehandlingStatistikkV2Dto =
                no.nav.familie.kontrakter.felles.objectMapper
                    .readValue(it.payload)
            val tekniskTid =
                ZonedDateTime.of(
                    LocalDateTime.now(),
                    SakStatistikkService.TIMEZONE,
                )
            val funksjonellTid = behandlingStatistikkV1Dto.funksjoneltTidspunkt
            assertEquals(true, tekniskTid.isAfter(funksjonellTid))
        }
    }

    @Test
    fun `sendMeldingOmEndringAvBehandlingkategori skal generere melding om ny tilstand til DVH`() {
        // Arrange
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)

        // Act
        sakStatistikkService.sendMeldingOmEndringAvBehandlingkategori(behandling.id, BehandlingKategori.EØS)

        // Assert
        val tasks = taskService.findAll()
        assertEquals(1, tasks.count { it.type == SendBehandlinghendelseTilDvhV2Task.TASK_TYPE })

        val taskSomErOpprettet = tasks[0]
        assertThat(taskSomErOpprettet.metadata["beskrivelse"]).isEqualTo("Endrer behandlingskategori til EØS for behandling ${behandling.id}")
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand på behandling som utredes`() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        stegService.utførSteg(behandling.id, BehandlingSteg.REGISTRERE_PERSONGRUNNLAG)

        val tilstand = sakStatistikkService.hentBehandlingensTilstandV2(behandling.id, false)
        assertEquals(behandling.fagsak.id, tilstand.saksnummer)
        assertEquals(behandling.id, tilstand.behandlingID)
        assertEquals(behandling.status, tilstand.behandlingStatus)
        assertEquals("4321", tilstand.ansvarligEnhet)
        assertEquals(null, tilstand.ansvarligBeslutter)
        assertEquals(behandling.endretAv, tilstand.ansvarligSaksbehandler)
        assertEquals(behandling.opprettetÅrsak, tilstand.behandlingOpprettetÅrsak)
        assertEquals(behandling.status, tilstand.behandlingStatus)
        assertEquals(true, tilstand.behandlingErManueltOpprettet)
        assertEquals(behandling.resultat, tilstand.behandlingsResultat)
        assertEquals(behandling.type, tilstand.behandlingType)
        assertEquals(behandling.skalBehandlesAutomatisk(), tilstand.automatiskBehandlet)

        // sjekk at datoer er konvertert riktig til offest
        assertThat(behandling.endretTidspunkt).isCloseTo(tilstand.funksjoneltTidspunkt.toLocalDateTime(), Assertions.within(1, ChronoUnit.SECONDS))
    }
}
