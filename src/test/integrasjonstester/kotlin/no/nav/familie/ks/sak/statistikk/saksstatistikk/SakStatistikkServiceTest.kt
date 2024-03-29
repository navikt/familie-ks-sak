package no.nav.familie.ks.sak.no.nav.familie.ks.sak.statistikk.saksstatistikk

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.RegistrerPersonGrunnlagSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SendBehandlinghendelseTilDvhTask
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
    private lateinit var service: SakStatistikkService

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
            taskService.findAll().count { it.type == SendBehandlinghendelseTilDvhTask.TASK_TYPE },
        )
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand på behandling som utredes`() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        stegService.utførSteg(behandling.id, BehandlingSteg.REGISTRERE_PERSONGRUNNLAG)

        val tilstand = service.hentBehandlingensTilstand(behandling.id)
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

        // sjekk at datoer er konvertert riktig til offest
        assertEquals(
            behandling.endretTidspunkt.truncatedTo(ChronoUnit.SECONDS),
            tilstand.funksjoneltTidspunkt.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS),
        )
    }
}
