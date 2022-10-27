package no.nav.familie.ks.sak.no.nav.familie.ks.sak.statistikk.saksstatistikk

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.RegistrerPersonGrunnlagSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.statistikk.saksstatistikk.BehandlingTilstandService
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SendBehandlinghendelseTilDvhTask
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingTilstandServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @MockkBean(relaxed = true)
    private lateinit var registerPersonGrunnlagSteg: RegistrerPersonGrunnlagSteg

    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var service: BehandlingTilstandService

    @Test
    fun `opprettSendingAvBehandlingensTilstand skal generere melding om ny tilstand til DVH`() {
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }
        opprettSøkerFagsakOgBehandling()
        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                id = 123,
                behandlingId = behandling.id,
                behandlendeEnhetId = "4321",
                behandlendeEnhetNavn = "Test enhet",
                manueltOverstyrt = false
            )
        )
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        opprettVilkårsvurdering(søker, behandling, Resultat.IKKE_VURDERT)
        stegService.utførSteg(behandling.id, BehandlingSteg.REGISTRERE_PERSONGRUNNLAG)
        assertEquals(
            1,
            taskService.taskRepository.findAll().filter { it.type == SendBehandlinghendelseTilDvhTask.TASK_TYPE }.count()
        )
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand på behandling som utredes`() {
        opprettSøkerFagsakOgBehandling()
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }
        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                id = 123,
                behandlingId = behandling.id,
                behandlendeEnhetId = "4321",
                behandlendeEnhetNavn = "Test enhet",
                manueltOverstyrt = false
            )
        )
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        opprettVilkårsvurdering(søker, behandling, Resultat.IKKE_VURDERT)
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
        assertEquals(behandling.resultat, tilstand.behandlingResultat)
        assertEquals(behandling.type, tilstand.behandlingType)
    }
}
