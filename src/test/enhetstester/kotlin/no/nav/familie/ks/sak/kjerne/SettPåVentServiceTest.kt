package no.nav.familie.ks.sak.kjerne

// TODO:
//  * Bytte ut SettPåVent-entitet med BehandlingstegTilstand-entitet.
//  * Bytte ut SettPåVentRepository med BehandlingstegTilstandRepository
//  * Bytte ut SettPåVentService med StegService.
//  Lar disse testene ligge her utkommentert, i og med at jeg mistenker at logikken rundt det å sette behandling på vent vil ligne veldig
//  på slik det ble gjort i BA til tross for at vi slenger det inn i BehandlingstegTilstand.
//  Dersom det viser seg at det ikke stemmer, kan denne fila trygt slettes!
// import io.mockk.every
// import io.mockk.impl.annotations.InjectMockKs
// import io.mockk.impl.annotations.MockK
// import io.mockk.junit5.MockKExtension
// import io.mockk.just
// import io.mockk.mockk
// import io.mockk.runs
// import io.mockk.slot
// import no.nav.familie.ks.sak.common.exception.Feil
// import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
// import no.nav.familie.ks.sak.data.lagBehandling
// import no.nav.familie.ks.sak.data.shouldNotBeNull
// import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
// import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
// import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
// import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
// import no.nav.familie.ks.sak.kjerne.logg.LoggService
// import org.junit.jupiter.api.Assertions.assertEquals
// import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.assertThrows
// import org.junit.jupiter.api.extension.ExtendWith
// import java.time.LocalDate
//
// @ExtendWith(MockKExtension::class)
// class SettPåVentServiceTest {
//
//    @MockK
//    private lateinit var loggService: LoggService
//
//    @MockK
//    private lateinit var oppgaveService: OppgaveService
//
//    @MockK
//    private lateinit var behandlingRepository: BehandlingRepository
//
//    @MockK
//    private lateinit var settPåVentRepository: SettPåVentRepository
//
//    @InjectMockKs
//    private lateinit var settPåVentService: SettPåVentService
//
//    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
//
//    @Test
//    fun `finnAktivSettPåVentPåBehandling - skal returere aktiv SettPåVent dersom den eksisterer`() {
//        val frist = LocalDate.now()
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns SettPåVent(behandling = behandling, frist = frist, årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
//
//        val settPåVent = settPåVentService.finnAktivSettPåVentPåBehandling(behandling.id).shouldNotBeNull()
//
//        assertEquals(behandling.id, settPåVent.behandling.id)
//        assertEquals(frist, settPåVent.frist)
//        assertEquals(SettPåVentÅrsak.AVVENTER_DOKUMENTASJON, settPåVent.årsak)
//    }
//
//    @Test
//    fun `finnAktivSettPåVentPåBehandlingThrows - skal kaste feil dersom det ikke finnes noen SettPåVent for behandling`() {
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns null
//
//        val feil = assertThrows<Feil> { settPåVentService.finnAktivSettPåVentPåBehandlingThrows(behandling.id) }
//
//        assertEquals("Behandling ${behandling.id} er ikke satt på vent.", feil.message)
//    }
//
//    @Test
//    fun `settBehandlingPåVent - skal kaste feil dersom behandlingen allerede er satt på vent`() {
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns SettPåVent(
//            behandling = behandling,
//            frist = LocalDate.now(),
//            årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//        )
//
//        val funksjonellFeil = assertThrows<FunksjonellFeil> {
//            settPåVentService.settBehandlingPåVent(
//                behandling.id,
//                LocalDate.now(),
//                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//            )
//        }
//
//        assertEquals("Behandling ${behandling.id} er allerede satt på vent.", funksjonellFeil.message)
//    }
//
//    @Test
//    fun `settBehandlingPåVent - skal kaste feil dersom frist er satt før dagens dato`() {
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns null
//
//        val funksjonellFeil = assertThrows<FunksjonellFeil> {
//            settPåVentService.settBehandlingPåVent(
//                behandling.id,
//                LocalDate.now().minusDays(1),
//                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//            )
//        }
//
//        assertEquals(
//            "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
//            funksjonellFeil.message
//        )
//    }
//
//    @Test
//    fun `settBehandlingPåVent - skal kaste feil dersom behandlingen er avsluttet`() {
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling.copy(status = BehandlingStatus.AVSLUTTET)
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns null
//
//        val funksjonellFeil = assertThrows<FunksjonellFeil> {
//            settPåVentService.settBehandlingPåVent(
//                behandling.id,
//                LocalDate.now(),
//                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//            )
//        }
//
//        assertEquals(
//            "Behandling ${behandling.id} er avsluttet og kan ikke settes på vent.",
//            funksjonellFeil.message
//        )
//    }
//
//    @Test
//    fun `settBehandlingPåVent - skal kaste feil dersom behandlingen ikke er aktiv`() {
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling.copy(aktiv = false)
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns null
//
//        val funksjonellFeil = assertThrows<Feil> {
//            settPåVentService.settBehandlingPåVent(
//                behandling.id,
//                LocalDate.now(),
//                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//            )
//        }
//
//        assertEquals(
//            "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent.",
//            funksjonellFeil.message
//        )
//    }
//
//    @Test
//    fun `settBehandlingPåVent - skal opprette SettPåVent tilknyttet behandling`() {
//        val settPåVentSlot = slot<SettPåVent>()
//
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns null
//
//        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs
//
//        every { settPåVentRepository.save(capture(settPåVentSlot)) } returns mockk()
//
//        every { oppgaveService.forlengFristÅpneOppgaverPåBehandling(any(), any()) } just runs
//
//        val frist = LocalDate.of(2022, 10, 19)
//
//        settPåVentService.settBehandlingPåVent(
//            behandlingId = behandling.id,
//            frist,
//            SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//        )
//
//        val settPåVent = settPåVentSlot.captured
//
//        assertEquals(
//            behandling.id,
//            settPåVent.behandling.id
//        )
//        assertEquals(frist, settPåVent.frist)
//        assertEquals(SettPåVentÅrsak.AVVENTER_DOKUMENTASJON, settPåVent.årsak)
//    }
//
//    @Test
//    fun `oppdaterSettBehandlingPåVent - skal kaste feil dersom ny frist og årsak er lik eksisterende`() {
//        val frist = LocalDate.of(2022, 10, 19)
//
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns SettPåVent(behandling = behandling, frist = frist, årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
//
//        val funksjonellFeil = assertThrows<FunksjonellFeil> {
//            settPåVentService.oppdaterSettBehandlingPåVent(
//                behandlingId = behandling.id,
//                frist,
//                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//            )
//        }
//        assertEquals(
//            "Behandlingen er allerede satt på vent med frist $frist og årsak ${SettPåVentÅrsak.AVVENTER_DOKUMENTASJON}.",
//            funksjonellFeil.message
//        )
//    }
//
//    @Test
//    fun `oppdaterSettBehandlingPåVent - skal oppdatere eksisterende SettPåVent på behandling`() {
//        val settPåVentSlot = slot<SettPåVent>()
//        val frist = LocalDate.of(2022, 10, 19)
//
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns SettPåVent(behandling = behandling, frist = frist, årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
//
//        every { loggService.opprettOppdaterVentingLogg(any(), any(), any()) } just runs
//
//        every { settPåVentRepository.save(capture(settPåVentSlot)) } returns mockk()
//
//        every { oppgaveService.forlengFristÅpneOppgaverPåBehandling(any(), any()) } just runs
//
//        val nyFrist = LocalDate.of(2022, 10, 20)
//
//        settPåVentService.oppdaterSettBehandlingPåVent(
//            behandlingId = behandling.id,
//            nyFrist,
//            SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
//        )
//
//        val oppdatertSettPåVent = settPåVentSlot.captured
//
//        assertEquals(
//            nyFrist,
//            oppdatertSettPåVent.frist
//        )
//    }
//
//    @Test
//    fun `gjenopptaBehandling - skal kaste feil dersom behandlingen ikke er satt på vent`() {
//        val frist = LocalDate.of(2022, 10, 19)
//
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns null
//
//        val funksjonellFeil = assertThrows<FunksjonellFeil> { settPåVentService.gjenopptaBehandling(behandling.id) }
//
//        assertEquals("Behandling ${behandling.id} er ikke satt på vent.", funksjonellFeil.message)
//        assertEquals(
//            "Behandlingen er ikke på vent og det er ikke mulig å gjenoppta behandling.",
//            funksjonellFeil.frontendFeilmelding
//        )
//    }
//
//    @Test
//    fun `gjenopptaBehandling - skal gjenoppta behandling som er satt på vent`() {
//        val settPåVentSlot = slot<SettPåVent>()
//        val frist = LocalDate.of(2022, 10, 19)
//
//        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling
//        every {
//            settPåVentRepository.findByBehandlingIdAndAktiv(
//                behandlingId = behandling.id,
//                true
//            )
//        } returns SettPåVent(behandling = behandling, frist = frist, årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
//
//        every { loggService.gjenopptaBehandlingLogg(any()) } just runs
//
//        every { settPåVentRepository.save(capture(settPåVentSlot)) } returns mockk()
//
//        every { oppgaveService.settFristÅpneOppgaverPåBehandlingTil(any(), any()) } just runs
//
//        settPåVentService.gjenopptaBehandling(behandling.id)
//
//        val gjenopptattSettPåVent = settPåVentSlot.captured
//
//        assertEquals(false, gjenopptattSettPåVent.aktiv)
//    }
// }
