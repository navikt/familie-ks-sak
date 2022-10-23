package no.nav.familie.ks.sak.kjerne.behandling.steg

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagRegistrerSøknadDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.BEHANDLINGSRESULTAT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.IVERKSETT_MOT_OPPDRAG
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.REGISTRERE_PERSONGRUNNLAG
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.REGISTRERE_SØKNAD
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.VILKÅRSVURDERING
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.KLAR
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.TILBAKEFØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.UTFØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.VENTER
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.RegistrereSøknadSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class StegServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var stegService: StegService

    @MockkBean(relaxed = true)
    private lateinit var registerPersonGrunnlagSteg: RegistrerPersonGrunnlagSteg

    @MockkBean(relaxed = true)
    private lateinit var registrerSøknadSteg: RegistrereSøknadSteg

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setup() {
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandling.id)
        opprettVilkårsvurdering(søker, behandling, Resultat.IKKE_VURDERT)
        every { registerPersonGrunnlagSteg.utførSteg(any()) } just runs
        every { registerPersonGrunnlagSteg.getBehandlingssteg() } answers { callOriginal() }

        every { registrerSøknadSteg.utførSteg(any()) } just runs
        every { registrerSøknadSteg.getBehandlingssteg() } answers { callOriginal() }
    }

    @Test
    fun `utførSteg skal utføre REGISTRER_PERSONGRUNNLAG og sette neste steg til REGISTRER_SØKNAD for FGB`() {
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }

        behandling = behandlingRepository.hentBehandling(behandling.id)
        assertEquals(2, behandling.behandlingStegTilstand.size)
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(behandling, REGISTRERE_SØKNAD, KLAR)
    }

    @Test
    fun `utførSteg skal utføre REGISTRER_PERSONGRUNNLAG og sette neste steg til VILKÅRSVURDERING for revurdering`() {
        lagreBehandling(behandling.also { it.aktiv = false })
        var revurderingBehandling = lagreBehandling(
            lagBehandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER
            )
        )
        assertBehandlingHarSteg(revurderingBehandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(revurderingBehandling.id, REGISTRERE_PERSONGRUNNLAG) }

        revurderingBehandling = behandlingRepository.hentBehandling(revurderingBehandling.id)
        assertEquals(2, revurderingBehandling.behandlingStegTilstand.size)
        assertBehandlingHarSteg(revurderingBehandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(revurderingBehandling, VILKÅRSVURDERING, KLAR)
    }

    @Test
    fun `utførSteg skal tilbakeføre behandlingsresultat når REGISTRERE_SØKNAD utføres på nytt for FGB`() {
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD, lagRegistrerSøknadDto()) }
        assertDoesNotThrow { stegService.utførSteg(behandling.id, VILKÅRSVURDERING) }

        behandling = behandlingRepository.hentBehandling(behandling.id)
        assertEquals(4, behandling.behandlingStegTilstand.size)
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(behandling, REGISTRERE_SØKNAD, UTFØRT)
        assertBehandlingHarSteg(behandling, VILKÅRSVURDERING, UTFØRT)
        assertBehandlingHarSteg(behandling, BEHANDLINGSRESULTAT, KLAR)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD, lagRegistrerSøknadDto()) }
        behandling = behandlingRepository.hentBehandling(behandling.id)
        assertEquals(4, behandling.behandlingStegTilstand.size)
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(behandling, REGISTRERE_SØKNAD, UTFØRT)
        assertBehandlingHarSteg(behandling, VILKÅRSVURDERING, KLAR)
        assertBehandlingHarSteg(behandling, BEHANDLINGSRESULTAT, TILBAKEFØRT)
    }

    @Test
    fun `utførSteg skal gjenoppta REGISTRERE_SØKNAD når steget er på vent for FGB`() {
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }
        behandling = behandlingRepository.hentBehandling(behandling.id)
            .also {
                it.behandlingStegTilstand
                    .maxByOrNull { tilstand -> tilstand.behandlingSteg.sekvens }?.behandlingStegStatus = VENTER
            }
        lagreBehandling(behandling)

        behandling = behandlingRepository.hentBehandling(behandling.id)
        assertEquals(2, behandling.behandlingStegTilstand.size)
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(behandling, REGISTRERE_SØKNAD, VENTER)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD, lagRegistrerSøknadDto()) }
        behandling = behandlingRepository.hentBehandling(behandling.id)
        assertEquals(2, behandling.behandlingStegTilstand.size)
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(behandling, REGISTRERE_SØKNAD, KLAR)
    }

    @Test
    fun `utførSteg skal ikke utføre IVERKSETT_MOT_OPPDRAG steg`() {
        behandling.leggTilNesteSteg(IVERKSETT_MOT_OPPDRAG)
        lagreBehandling(behandling)

        val exception = assertThrows<RuntimeException> { stegService.utførSteg(behandling.id, IVERKSETT_MOT_OPPDRAG) }
        assertEquals(
            "Steget ${IVERKSETT_MOT_OPPDRAG.name} kan ikke behandles for behandling ${behandling.id}",
            exception.message
        )
    }

    @Test
    fun `utførSteg skal ikke utføre REGISTRERE_SØKNAD for behandling med årsak SATSENDRING`() {
        lagreBehandling(behandling.also { it.aktiv = false })
        val revurderingBehandling = lagreBehandling(
            lagBehandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.SATSENDRING
            )
        )
        assertBehandlingHarSteg(revurderingBehandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        revurderingBehandling.leggTilNesteSteg(REGISTRERE_SØKNAD)
        lagreBehandling(revurderingBehandling)

        val exception = assertThrows<RuntimeException> {
            stegService.utførSteg(
                revurderingBehandling.id,
                REGISTRERE_SØKNAD,
                lagRegistrerSøknadDto()
            )
        }
        assertEquals(
            "Steget ${REGISTRERE_SØKNAD.name} er ikke gyldig for behandling ${revurderingBehandling.id} " +
                "med opprettetÅrsak ${revurderingBehandling.opprettetÅrsak}",
            exception.message
        )
    }

    @Test
    fun `utførSteg skal ikke utføre SATSENDRING steg før REGISTRERE_PERSONGRUNNLAG er utført`() {
        behandling.leggTilNesteSteg(REGISTRERE_SØKNAD)
        lagreBehandling(behandling)

        val exception = assertThrows<RuntimeException> {
            stegService.utførSteg(
                behandling.id,
                REGISTRERE_SØKNAD,
                lagRegistrerSøknadDto()
            )
        }
        assertEquals(
            "Behandling ${behandling.id} har allerede et steg " +
                "${REGISTRERE_PERSONGRUNNLAG.name}} som er klar for behandling. " +
                "Kan ikke behandle ${REGISTRERE_SØKNAD.name}",
            exception.message
        )
    }

    private fun assertBehandlingHarSteg(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg,
        behandlingStegStatus: BehandlingStegStatus
    ) =
        assertTrue(
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == behandlingSteg &&
                    it.behandlingStegStatus == behandlingStegStatus
            }
        )
}
