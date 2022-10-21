package no.nav.familie.ks.sak.kjerne.logg

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.familie.ks.sak.kjerne.logg.domene.LoggRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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

        every { loggRepository.save(capture(lagredeLogg)) } returns mockk()

        loggService.opprettBehandlendeEnhetEndret(
            behandling,
            arbeidsFordelingEnhet,
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
            arbeidsFordelingEnhet,
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
    fun `opprettVilkårsvurderingLogg skal lagre logg når behandlingsresultat har utledet`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val slot = slot<Logg>()
        every { loggRepository.save(capture(slot)) } returns mockk()

        loggService.opprettVilkårsvurderingLogg(behandling, Behandlingsresultat.IKKE_VURDERT, Behandlingsresultat.INNVILGET)

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
}
