package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBrevmottakerDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.brev.GenererBrevService
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.internal.TaskService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import org.hamcrest.CoreMatchers.`is` as Is

class BeslutteVedtakStegTest {
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val vedtakService = mockk<VedtakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val taskService = mockk<TaskService>()
    private val loggService = mockk<LoggService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val genererBrevService = mockk<GenererBrevService>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val brevmottakerService = mockk<BrevmottakerService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val simuleringService = mockk<SimuleringService>()

    private val beslutteVedtakSteg =
        BeslutteVedtakSteg(
            totrinnskontrollService = totrinnskontrollService,
            vedtakService = vedtakService,
            behandlingService = behandlingService,
            taskService = taskService,
            loggService = loggService,
            vilkårsvurderingService = vilkårsvurderingService,
            featureToggleService = featureToggleService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            brevmottakerService = brevmottakerService,
            tilbakekrevingService = tilbakekrevingService,
            simuleringService = simuleringService,
        )

    private val underkjentVedtakDto = BesluttVedtakDto(Beslutning.UNDERKJENT, "UNDERKJENT")
    private val godkjentVedtakDto = BesluttVedtakDto(Beslutning.GODKJENT, "GODKJENT")

    @BeforeEach
    fun init() {
        every { behandlingService.hentBehandling(200) } returns lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { featureToggleService.isEnabled(FeatureToggle.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) } returns false
        every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) } returns mockk()
        every { taskService.save(any()) } returns mockk()
        every { genererBrevService.genererBrevForBehandling(any()) } returns ByteArray(200)
    }

    @Test
    fun `utførSteg skal kaste FunksjonellFeil dersom behandlingen er satt til IVERKSETTER_VEDTAK `() {
        every { behandlingService.hentBehandling(200) } returns
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).apply {
                status = BehandlingStatus.IVERKSETTER_VEDTAK
            }

        val funksjonellFeil = assertThrows<FunksjonellFeil> { beslutteVedtakSteg.utførSteg(200, godkjentVedtakDto) }

        assertThat(funksjonellFeil.message, Is("Behandlingen er allerede sendt til oppdrag og venter på kvittering"))
    }

    @Test
    fun `utførSteg skal kaste FunksjonellFeil dersom behandlingen er satt til AVSLUTTET `() {
        every { behandlingService.hentBehandling(200) } returns
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).apply {
                status = BehandlingStatus.AVSLUTTET
            }

        val funksjonellFeil = assertThrows<FunksjonellFeil> { beslutteVedtakSteg.utførSteg(200, godkjentVedtakDto) }

        assertThat(funksjonellFeil.message, Is("Behandlingen er allerede avsluttet"))
    }

    @Test
    fun `utførSteg skal kaste FunksjonellFeil dersom behandling årsaken er satt til KORREKSJON_VEDTAKSBREV og SB ikke har feature togglet på `() {
        every { behandlingService.hentBehandling(200) } returns lagBehandling(opprettetÅrsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV)
        every { featureToggleService.isEnabled(FeatureToggle.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) } returns false

        val funksjonellFeil = assertThrows<FunksjonellFeil> { beslutteVedtakSteg.utførSteg(200, godkjentVedtakDto) }

        assertThat(
            funksjonellFeil.message,
            Is("Årsak Korrigere vedtak med egen brevmal og toggle familie-ks-sak.behandling.korreksjon-vedtaksbrev false"),
        )
        assertThat(
            funksjonellFeil.frontendFeilmelding,
            Is("Du har ikke tilgang til å beslutte for denne behandlingen. Ta kontakt med teamet dersom dette ikke stemmer."),
        )
    }

    @Test
    fun `utførSteg skal opprette og initiere nytt vedtak dersom vedtaket er underkjent `() {
        every {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                underkjentVedtakDto.beslutning,
                emptyList(),
            )
        } returns mockk(relaxed = true)
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns mockk()
        every { vilkårsvurderingService.oppdater(any()) } returns mockk()
        every { vedtakService.opprettOgInitierNyttVedtakForBehandling(any(), any()) } just runs
        every { brevmottakerService.hentBrevmottakere(any()) } returns listOf(lagBrevmottakerDto(id = 123))

        beslutteVedtakSteg.utførSteg(200, underkjentVedtakDto)

        verify(exactly = 1) {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                underkjentVedtakDto.beslutning,
                emptyList(),
            )
        }
        verify(exactly = 1) { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) }
        verify(exactly = 2) { taskService.save(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) }
        verify(exactly = 1) { vilkårsvurderingService.oppdater(any()) }
        verify(exactly = 1) { vedtakService.opprettOgInitierNyttVedtakForBehandling(any(), any()) }
    }

    @Test
    fun `utførSteg skal oppdatere vedtak med nytt vedtaksbrev dersom vedtaket er godkjent `() {
        every {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                godkjentVedtakDto.beslutning,
                emptyList(),
            )
        } returns mockk(relaxed = true)
        every { tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(any()) } just runs

        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns mockk(relaxed = true)
        every { vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(any()) } returns mockk()
        every { brevmottakerService.hentBrevmottakere(any()) } returns listOf(lagBrevmottakerDto(id = 123))
        every { tilbakekrevingService.harÅpenTilbakekrevingsbehandling(any()) } returns false
        every { tilbakekrevingService.finnTilbakekrevingsbehandling(any()) } returns null
        every { simuleringService.hentFeilutbetaling(any()) } returns BigDecimal.ZERO

        beslutteVedtakSteg.utførSteg(200, godkjentVedtakDto)

        verify(exactly = 1) {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                godkjentVedtakDto.beslutning,
                emptyList(),
            )
        }
        verify(exactly = 1) { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) }
        verify(exactly = 1) { vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(any()) }
        verify(exactly = 1) { tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(any()) }
    }

    @Test
    fun `Skal kaste feil dersom saksbehandler uten tilgang til teknisk endring prøve å godkjenne en behandling med årsak=teknisk endring`() {
        every { featureToggleService.isEnabled(FeatureToggle.TEKNISK_ENDRING, any<Long>()) } returns false

        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.TEKNISK_ENDRING)

        every { behandlingService.hentBehandling(any()) } returns behandling

        val feil = assertThrows<FunksjonellFeil> { beslutteVedtakSteg.utførSteg(behandling.id, godkjentVedtakDto) }
        assertThat(
            feil.melding,
            Is("Du har ikke tilgang til å beslutte en behandling med årsak=Teknisk endring. Ta kontakt med teamet dersom dette ikke stemmer."),
        )
    }

    @Test
    fun `utførSteg skal kaste feil dersom vedtaket er godkjent og det finnes ugyldige manuelle brevmottakere`() {
        // Arrange
        every {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                godkjentVedtakDto.beslutning,
                emptyList(),
            )
        } returns mockk(relaxed = true)
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns mockk()
        every { vilkårsvurderingService.oppdater(any()) } returns mockk()
        every { vedtakService.opprettOgInitierNyttVedtakForBehandling(any(), any()) } just runs
        every { brevmottakerService.hentBrevmottakere(any()) } returns listOf(lagBrevmottakerDto(id = 123, postnummer = "0661", poststed = "Stockholm", landkode = "SE"))

        // Act & assert
        val exception =
            assertThrows<FunksjonellFeil> {
                beslutteVedtakSteg.utførSteg(200, godkjentVedtakDto)
            }

        assertThat(exception.message, Is("Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket"))
    }

    @Test
    fun `utførSteg skal opprette og initiere nytt vedtak dersom vedtaket er underkjent selv om det finnes ugyldige brevmottakere`() {
        // Arrange
        every {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                underkjentVedtakDto.beslutning,
                emptyList(),
            )
        } returns mockk(relaxed = true)
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns mockk()
        every { vilkårsvurderingService.oppdater(any()) } returns mockk()
        every { vedtakService.opprettOgInitierNyttVedtakForBehandling(any(), any()) } just runs
        every { brevmottakerService.hentBrevmottakere(any()) } returns listOf(lagBrevmottakerDto(id = 123, postnummer = "0661", poststed = "Stockholm", landkode = "SE"))

        // Act
        beslutteVedtakSteg.utførSteg(200, underkjentVedtakDto)

        // Assert
        verify(exactly = 1) {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                underkjentVedtakDto.beslutning,
                emptyList(),
            )
        }
        verify(exactly = 1) { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) }
        verify(exactly = 2) { taskService.save(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) }
        verify(exactly = 1) { vilkårsvurderingService.oppdater(any()) }
        verify(exactly = 1) { vedtakService.opprettOgInitierNyttVedtakForBehandling(any(), any()) }
    }

    @Test
    fun `utførSteg skal kaste feil dersom det er feilutbetalt beløp men det er ikke valgt å opprette tilbakekrevingssak`() {
        every {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                godkjentVedtakDto.beslutning,
                emptyList(),
            )
        } returns mockk(relaxed = true)
        every { tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(any()) } just runs

        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns mockk(relaxed = true)
        every { vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(any()) } returns mockk()
        every { brevmottakerService.hentBrevmottakere(any()) } returns listOf(lagBrevmottakerDto(id = 123))
        every { tilbakekrevingService.harÅpenTilbakekrevingsbehandling(any()) } returns false
        every { tilbakekrevingService.finnTilbakekrevingsbehandling(any()) } returns null
        every { simuleringService.hentFeilutbetaling(any()) } returns BigDecimal.valueOf(50000)

        // Act & assert
        val exception =
            assertThrows<FunksjonellFeil> {
                beslutteVedtakSteg.utførSteg(200, godkjentVedtakDto)
            }

        assertThat(exception.message, Is("Det er en feilutbetaling som saksbehandler ikke har tatt stilling til. Saken må underkjennes og sendes tilbake til saksbehandler for ny vurdering."))
    }

    @ParameterizedTest
    @EnumSource(Tilbakekrevingsvalg::class, names = ["OPPRETT_TILBAKEKREVING_MED_VARSEL", "OPPRETT_TILBAKEKREVING_UTEN_VARSEL", "OPPRETT_TILBAKEKREVING_AUTOMATISK"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal kaste feil dersom feilutbetaling ikke lenger finnes og det er valgt å opprette en tilbakekrevingssak`(tilbakekrevingsvalg: Tilbakekrevingsvalg) {
        val mocketTilbakekreving =
            mockk<Tilbakekreving>(relaxed = true).apply {
                every { valg } returns tilbakekrevingsvalg
            }

        every {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                godkjentVedtakDto.beslutning,
                emptyList(),
            )
        } returns mockk(relaxed = true)
        every { tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(any()) } just runs

        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns mockk(relaxed = true)
        every { vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(any()) } returns mockk()
        every { brevmottakerService.hentBrevmottakere(any()) } returns listOf(lagBrevmottakerDto(id = 123))
        every { tilbakekrevingService.harÅpenTilbakekrevingsbehandling(any()) } returns false
        every { tilbakekrevingService.finnTilbakekrevingsbehandling(any()) } returns mocketTilbakekreving
        every { simuleringService.hentFeilutbetaling(any()) } returns BigDecimal.ZERO

        // Act & assert
        val exception =
            assertThrows<FunksjonellFeil> {
                beslutteVedtakSteg.utførSteg(200, godkjentVedtakDto)
            }

        assertThat(exception.message, Is("Det er valgt å opprette tilbakekrevingssak men det er ikke lenger feilutbetalt beløp. Behandlingen må underkjennes, og saksbehandler må gå tilbake til behandlingsresultatet og trykke neste og fullføre behandlingen på nytt."))
    }
}
