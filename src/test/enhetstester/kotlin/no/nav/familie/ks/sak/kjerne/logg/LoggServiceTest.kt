package no.nav.familie.ks.sak.kjerne.logg

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.ArbeidsfordelingsEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.familie.ks.sak.kjerne.logg.domene.LoggRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class LoggServiceTest {

    @MockK
    private lateinit var rolleConfig: RolleConfig

    @MockK
    private lateinit var loggRepository: LoggRepository

    @InjectMockKs
    private lateinit var loggService: LoggService

    @Test
    fun `opprettBehandlingLogg - skal lagre logg på at behandling er opprettet`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettBehandlingLogg(behandling)

        verify(exactly = 1) { loggRepository.save(slot.captured) }

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.BEHANDLING_OPPRETTET))
    }

    @Test
    fun `hentLoggForBehandling - skal returnere all logg for behandling`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val loggForBehandling = listOf<Logg>(mockk(), mockk())

        every { loggRepository.hentLoggForBehandling(behandling.id) } returns loggForBehandling

        val hentetLoggForBehandling = loggService.hentLoggForBehandling(behandling.id)

        assertThat(hentetLoggForBehandling.size, Is(2))
        assertThat(hentetLoggForBehandling, Is(loggForBehandling))
    }

    @Test
    fun `opprettAutovedtakTilManuellBehandling - skal lagre logg med type autovedtak til manuell beheandling`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettAutovedtakTilManuellBehandling(behandling, "tekststreng")

        verify(exactly = 1) { loggRepository.save(slot.captured) }

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.AUTOVEDTAK_TIL_MANUELL_BEHANDLING))
        assertThat(lagretLogg.tekst, Is("tekststreng"))
    }

    @Test
    fun `opprettAutovedtakTilManuellBehandling - skal lagre logg med type behandle enhet endret med tekstbeskrivelse`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val lagredeLogg = mutableListOf<Logg>()

        val arbeidsFordelingEnhet = ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetId = "fraEnhetId",
            behandlendeEnhetNavn = "fraEnhetNavn"
        )

        val aktivArbeidsfordelingenhet = ArbeidsfordelingsEnhet(
            enhetId = arbeidsFordelingEnhet.behandlendeEnhetId,
            enhetNavn = arbeidsFordelingEnhet.behandlendeEnhetNavn
        )

        every { loggRepository.save(capture(lagredeLogg)) } returns mockk()

        loggService.opprettBehandlendeEnhetEndret(
            behandling,
            aktivArbeidsfordelingenhet,
            arbeidsFordelingEnhet,
            true,
            "testbegrunnelse"
        )

        verify(exactly = 1) { loggRepository.save(lagredeLogg[0]) }

        val lagretLoggManuellOppdatering = lagredeLogg[0]

        assertThat(lagretLoggManuellOppdatering.behandlingId, Is(behandling.id))
        assertThat(lagretLoggManuellOppdatering.type, Is(LoggType.BEHANDLENDE_ENHET_ENDRET))
        assertThat(
            lagretLoggManuellOppdatering.tekst,
            Is("Behandlende enhet manuelt endret fra fraEnhetId fraEnhetNavn til fraEnhetId fraEnhetNavn.\n\ntestbegrunnelse")
        )

        loggService.opprettBehandlendeEnhetEndret(
            behandling,
            aktivArbeidsfordelingenhet,
            arbeidsFordelingEnhet,
            false,
            "testbegrunnelse"
        )

        verify(exactly = 1) { loggRepository.save(lagredeLogg[1]) }

        val lagretLoggIkkeManuellOppdatering = lagredeLogg[1]

        assertThat(lagretLoggIkkeManuellOppdatering.behandlingId, Is(behandling.id))
        assertThat(lagretLoggIkkeManuellOppdatering.type, Is(LoggType.BEHANDLENDE_ENHET_ENDRET))
        assertThat(
            lagretLoggIkkeManuellOppdatering.tekst,
            Is("Behandlende enhet automatisk endret fra fraEnhetId fraEnhetNavn til fraEnhetId fraEnhetNavn.\n\ntestbegrunnelse")
        )
    }

    @Test
    fun `opprettRegistrertSøknadLogg - skal lagre logg med informasjon om at søknad er opprettet dersom det ikke finnes en søknad for behandling fra før`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val lagretLoggSlot = slot<Logg>()
        every { loggRepository.save(capture(lagretLoggSlot)) } returns mockk()

        loggService.opprettRegistrertSøknadLogg(behandling.id, false)

        val lagretLogg = lagretLoggSlot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.SØKNAD_REGISTRERT))
        assertThat(lagretLogg.tittel, Is("Søknaden ble registrert"))
    }

    @Test
    fun `opprettRegistrertSøknadLogg - skal lagre logg med informasjon om at søknad er endret dersom det finnes en søknad for behandling fra før`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val lagretLoggSlot = slot<Logg>()
        every { loggRepository.save(capture(lagretLoggSlot)) } returns mockk()

        loggService.opprettRegistrertSøknadLogg(behandling.id, true)

        val lagretLogg = lagretLoggSlot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.SØKNAD_REGISTRERT))
        assertThat(lagretLogg.tittel, Is("Søknaden ble endret"))
    }

    @Test
    fun `opprettMottattDokumentLogg - skal lagre logg på at dokument er mottatt `() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val mottattDato = LocalDateTime.of(2022, 4, 1, 0, 0)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettMottattDokumentLogg(behandling, "testTekst", mottattDato)

        verify(exactly = 1) { loggRepository.save(slot.captured) }

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.DOKUMENT_MOTTATT))
        assertThat(lagretLogg.tekst, Is("testTekst"))
        assertThat(lagretLogg.tittel, Is("Dokument mottatt 01.04.22"))
    }

    @Test
    fun `opprettSettPåVentLogg - skal lagre logg på at behandling er satt på vent`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettSettPåVentLogg(behandling, "Testårsak")

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.BEHANDLIG_SATT_PÅ_VENT))
        assertThat(lagretLogg.tekst, Is("Årsak: Testårsak"))
    }

    @Test
    fun `opprettVilkårsvurderingLogg skal lagre logg når behandlingsresultat har utledet`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()
        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettVilkårsvurderingLogg(
            behandling,
            Behandlingsresultat.IKKE_VURDERT,
            Behandlingsresultat.INNVILGET
        )

        verify(exactly = 1) { loggRepository.save(slot.captured) }

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.VILKÅRSVURDERING))
        assertThat(lagretLogg.tekst, Is("Resultat ble ${Behandlingsresultat.INNVILGET.displayName.lowercase()}"))
        assertThat(lagretLogg.tittel, Is("Vilkårsvurdering gjennomført"))
    }

    @Test
    fun `opprettVilkårsvurderingLogg skal lagre logg når behandlingsresultat har endret`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val behandlingsforrigeResultat = Behandlingsresultat.INNVILGET
        val behandlingsNyResultat = Behandlingsresultat.ENDRET_OG_OPPHØRT
        val slot = slot<Logg>()
        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettVilkårsvurderingLogg(behandling, behandlingsforrigeResultat, behandlingsNyResultat)

        verify(exactly = 1) { loggRepository.save(slot.captured) }

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.VILKÅRSVURDERING))
        assertThat(
            lagretLogg.tekst,
            Is(
                "Resultat gikk fra ${behandlingsforrigeResultat.displayName.lowercase()} " +
                    "til ${behandlingsNyResultat.displayName.lowercase()}"
            )
        )
        assertThat(lagretLogg.tittel, Is("Vilkårsvurdering endret"))
    }

    @Test
    fun `opprettVilkårsvurderingLogg skal ikke lagre logg når behandlingsresultat ikke har endret`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val behandlingsforrigeResultat = Behandlingsresultat.INNVILGET
        val behandlingsNyResultat = Behandlingsresultat.INNVILGET

        loggService.opprettVilkårsvurderingLogg(behandling, behandlingsforrigeResultat, behandlingsNyResultat)

        verify(exactly = 0) { loggRepository.save(any()) }
    }

    @Test
    fun `opprettOppdaterVentingLogg - skal lagre logg på at frist på en behandlings SettPåVent er endret`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val nyFrist = LocalDate.now()
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettOppdaterVentingLogg(behandling = behandling, endretFrist = nyFrist)

        verify(exactly = 1) { loggRepository.save(slot.captured) }

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.VENTENDE_BEHANDLING_ENDRET))
        assertThat(lagretLogg.tekst, Is("Frist er endret til ${nyFrist.tilKortString()}"))
    }

    @Test
    fun `gjenopptaBehandlingLogg - skal lagre logg på en behandling som var satt på vent er gjenopptatt`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettBehandlingGjenopptattLogg(behandling)

        verify(exactly = 1) { loggRepository.save(slot.captured) }

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.BEHANDLIG_GJENOPPTATT))
    }

    @Test
    fun `opprettBrevIkkeDistribuertUkjentAdresseLogg - skal lagre logg på at brev ikke ble distribuert grunnet ukjent addresse`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(behandling.id, "ukjent addresse")

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.BREV_IKKE_DISTRIBUERT))
        assertThat(lagretLogg.tekst, Is("ukjent addresse"))
    }

    @Test
    fun `opprettDistribuertBrevLogg - skal lagre logg på at brev har blitt distribuert`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettDistribuertBrevLogg(behandling.id, "distribuert", BehandlerRolle.SYSTEM)

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.DISTRIBUERE_BREV))
        assertThat(lagretLogg.tekst, Is("distribuert"))
    }

    @Test
    fun `opprettBrevIkkeDistribuertUkjentDødsboadresseLogg - skal lagre logg på at brev ikke ble distribuert grunnet ukjent dødsboaddresse`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(behandling.id, "ukjent dødsboaddresse")

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.BREV_IKKE_DISTRIBUERT_UKJENT_DØDSBO))
        assertThat(lagretLogg.tekst, Is("ukjent dødsboaddresse"))
    }

    @Test
    fun `opprettSendTilBeslutterLogg - skal lagre logg på at behandling ble sendt til beslutter`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()

        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettSendTilBeslutterLogg(behandling.id)

        val lagretLogg = slot.captured

        assertThat(lagretLogg.behandlingId, Is(behandling.id))
        assertThat(lagretLogg.type, Is(LoggType.SEND_TIL_BESLUTTER))
    }
}
