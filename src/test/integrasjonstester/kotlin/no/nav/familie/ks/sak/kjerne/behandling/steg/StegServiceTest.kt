package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import jakarta.transaction.Transactional
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.dto.tilSøknadGrunnlag
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBehandlingStegTilstand
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagRegistrerSøknadDto
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.AVSLUTT_BEHANDLING
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.BEHANDLINGSRESULTAT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.BESLUTTE_VEDTAK
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.IVERKSETT_MOT_OPPDRAG
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.JOURNALFØR_VEDTAKSBREV
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.REGISTRERE_PERSONGRUNNLAG
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.REGISTRERE_SØKNAD
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.VEDTAK
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.VILKÅRSVURDERING
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.KLAR
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.TILBAKEFØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.UTFØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.VENTER
import no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev.JournalførVedtaksbrevTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.tilbakekreving.SendOpprettTilbakekrevingsbehandlingRequestTask
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SendBehandlinghendelseTilDvhV2Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class StegServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var taskService: FakeTaskRepositoryWrapper

    @Autowired
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

    @Autowired
    private lateinit var vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService

    @Autowired
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    @Autowired
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    private lateinit var totrinnskontrollService: TotrinnskontrollService

    @BeforeEach
    fun setup() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        opprettVilkårsvurdering(søker, behandling, Resultat.IKKE_VURDERT)
        taskService.reset()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
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
        lagreBehandling(
            behandling.also {
                it.aktiv = false
                it.status = BehandlingStatus.AVSLUTTET
            },
        )
        var revurderingBehandling =
            lagreBehandling(
                lagBehandling(
                    fagsak = fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                ),
            )
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = revurderingBehandling.id))
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
        val vedtak = Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now())
        vedtakRepository.saveAndFlush(vedtak)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD, lagRegistrerSøknadDto()) }
        assertDoesNotThrow { stegService.utførSteg(behandling.id, VILKÅRSVURDERING) }

        val behandlingEtterVilkårsvurdering = behandlingRepository.hentBehandling(behandling.id)
        assertEquals(4, behandlingEtterVilkårsvurdering.behandlingStegTilstand.size)
        assertBehandlingHarSteg(behandlingEtterVilkårsvurdering, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(behandlingEtterVilkårsvurdering, REGISTRERE_SØKNAD, UTFØRT)
        assertBehandlingHarSteg(behandlingEtterVilkårsvurdering, VILKÅRSVURDERING, UTFØRT)
        assertBehandlingHarSteg(behandlingEtterVilkårsvurdering, BEHANDLINGSRESULTAT, KLAR)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD, lagRegistrerSøknadDto()) }
        val behandlingEtterRegistrerSøknad = behandlingRepository.hentBehandling(behandling.id)
        assertEquals(4, behandlingEtterRegistrerSøknad.behandlingStegTilstand.size)
        assertBehandlingHarSteg(behandlingEtterRegistrerSøknad, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertBehandlingHarSteg(behandlingEtterRegistrerSøknad, REGISTRERE_SØKNAD, UTFØRT)
        assertBehandlingHarSteg(behandlingEtterRegistrerSøknad, VILKÅRSVURDERING, KLAR)
        assertBehandlingHarSteg(behandlingEtterRegistrerSøknad, BEHANDLINGSRESULTAT, TILBAKEFØRT)
    }

    @Test
    fun `utførSteg skal gjenoppta REGISTRERE_SØKNAD når steget er på vent for FGB`() {
        assertBehandlingHarSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }
        behandling = behandlingRepository.hentBehandling(behandling.id)

        stegService.settBehandlingstegPåVent(behandling, LocalDate.now().plusMonths(2), VenteÅrsak.AVVENTER_BEHANDLING)

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
    fun `utførSteg skal ikke utføre IVERKSETT_MOT_OPPDRAG steg av beslutter`() {
        behandling.behandlingStegTilstand.clear()
        behandling.leggTilNesteSteg(IVERKSETT_MOT_OPPDRAG)
        lagreBehandling(behandling)

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        val exception = assertThrows<RuntimeException> { stegService.utførSteg(behandling.id, IVERKSETT_MOT_OPPDRAG) }
        assertEquals(
            "Steget ${IVERKSETT_MOT_OPPDRAG.name} kan ikke behandles for behandling ${behandling.id}",
            exception.message,
        )
    }

    @Test
    fun `utførSteg skal ikke utføre REGISTRERE_SØKNAD for behandling med årsak SATSENDRING`() {
        lagreBehandling(
            behandling.also {
                it.aktiv = false
                it.status = BehandlingStatus.AVSLUTTET
            },
        )
        val revurderingBehandling =
            lagreBehandling(
                lagBehandling(
                    fagsak = fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.SATSENDRING,
                ),
            )
        assertBehandlingHarSteg(revurderingBehandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        revurderingBehandling.leggTilNesteSteg(REGISTRERE_SØKNAD)
        lagreBehandling(revurderingBehandling)

        val exception =
            assertThrows<RuntimeException> {
                stegService.utførSteg(
                    revurderingBehandling.id,
                    REGISTRERE_SØKNAD,
                    lagRegistrerSøknadDto(),
                )
            }
        assertEquals(
            "Steget ${REGISTRERE_SØKNAD.name} er ikke gyldig for behandling ${revurderingBehandling.id} " +
                "med opprettetÅrsak ${revurderingBehandling.opprettetÅrsak}",
            exception.message,
        )
    }

    @Test
    fun `utførSteg skal ikke utføre SATSENDRING steg før REGISTRERE_PERSONGRUNNLAG er utført`() {
        behandling.leggTilNesteSteg(REGISTRERE_SØKNAD)
        lagreBehandling(behandling)

        val exception =
            assertThrows<RuntimeException> {
                stegService.utførSteg(
                    behandling.id,
                    REGISTRERE_SØKNAD,
                    lagRegistrerSøknadDto(),
                )
            }
        assertEquals(
            "Behandling ${behandling.id} har allerede et steg " +
                "${REGISTRERE_PERSONGRUNNLAG.name}} som er klar for behandling. " +
                "Kan ikke behandle ${REGISTRERE_SØKNAD.name}",
            exception.message,
        )
    }

    @Test
    fun `utførSteg skal videresende behandling fra BESLUTTE_VEDTAK til IVERKSETT_MOT_OPPDRAG steg når SB godkjenner`() {
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, VEDTAK, UTFØRT))
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, BESLUTTE_VEDTAK, KLAR))
        behandling.resultat = Behandlingsresultat.INNVILGET
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        lagreBehandling(behandling)
        val vedtak = Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now())
        vedtakRepository.saveAndFlush(vedtak)

        søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto("søkerIdent"),
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = "barn1"),
                        BarnMedOpplysningerDto("barn2"),
                    ),
                "begrunnelse",
            ).tilSøknadGrunnlag(behandling.id),
        )

        lagTilkjentYtelse("")
        val andelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                kalkulertUtbetalingsbeløp = 1000,
                behandling = behandling,
            )
        andelTilkjentYtelseRepository.saveAndFlush(andelTilkjentYtelse)

        val vedtaksperioder = vedtaksperiodeService.genererVedtaksperioderMedBegrunnelser(vedtak)
        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioder)

        vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
            vedtaksperioder.first().id,
            begrunnelserFraFrontend =
                listOf(
                    NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING,
                ),
        )

        val beslutteVedtakDto = BesluttVedtakDto(beslutning = Beslutning.GODKJENT, begrunnelse = "Godkjent")
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, BESLUTTE_VEDTAK, beslutteVedtakDto) }

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling.id)
        assertTrue { oppdatertBehandling.status == BehandlingStatus.IVERKSETTER_VEDTAK }
        assertBehandlingHarSteg(oppdatertBehandling, VEDTAK, UTFØRT)
        assertBehandlingHarSteg(oppdatertBehandling, BESLUTTE_VEDTAK, UTFØRT)
        assertBehandlingHarSteg(oppdatertBehandling, IVERKSETT_MOT_OPPDRAG, KLAR)

        assertThat(taskService.lagredeTasker.size).isGreaterThan(0)
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingSteg::class, names = ["BESLUTTE_VEDTAK", "IVERKSETT_MOT_OPPDRAG", "JOURNALFØR_VEDTAKSBREV", "AVSLUTT_BEHANDLING"])
    fun `utførSteg skal kaste feil dersom vi forsøker å utføre et allerede utført steg fra og med BESLUTTE_VEDTAK-steget`(behandlingSteg: BehandlingSteg) {
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, behandlingSteg, UTFØRT))

        lagreBehandling(behandling)

        // Sørger for at vi bytter til system-kontekst dersom vi tester et "system"-steg. Ønsker her kun å teste ny validering på om steget er utført fra før eller ikke.
        if (!behandlingSteg.kanStegBehandles()) {
            mockkObject(SikkerhetContext)
            every { SikkerhetContext.erSystemKontekst() } returns true
        }

        val alleredeUtførtStegFeil = assertThrows<Feil> { stegService.utførSteg(behandling.id, behandlingSteg, null) }
        assertTrue(alleredeUtførtStegFeil.message!!.contains("allerede utført"))
        unmockkObject(SikkerhetContext)
    }

    @ParameterizedTest
    @EnumSource(BehandlingSteg::class)
    fun `utførSteg skal kaste feil dersom vi forsøker å utføre et steg i en avsluttet behandling`(behandlingSteg: BehandlingSteg) {
        behandling.behandlingStegTilstand.clear()

        BehandlingSteg.entries.forEach {
            behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, it, UTFØRT))
        }

        behandling.status = BehandlingStatus.AVSLUTTET

        lagreBehandling(behandling)

        // Sørger for at vi bytter til system-kontekst dersom vi tester et "system"-steg. Ønsker her kun å teste ny validering på om behandling er avsluttet eller ikke.
        if (!behandlingSteg.kanStegBehandles()) {
            mockkObject(SikkerhetContext)
            every { SikkerhetContext.erSystemKontekst() } returns true
        }

        val behandlingAvsluttetFeil = assertThrows<Feil> { stegService.utførSteg(behandling.id, behandlingSteg, null) }
        assertTrue(behandlingAvsluttetFeil.message!!.contains("avsluttet behandling"))
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `utførSteg skal tilbakeføre behandling fra BESLUTTE_VEDTAK til VEDTAK steg når SB underkjenner vedtaket`() {
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, VEDTAK, UTFØRT))
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, BESLUTTE_VEDTAK, KLAR))

        behandling.status = BehandlingStatus.FATTER_VEDTAK
        lagreBehandling(behandling)

        val beslutteVedtakDto = BesluttVedtakDto(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Underkjent")
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, BESLUTTE_VEDTAK, beslutteVedtakDto) }

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling.id)
        assertTrue { oppdatertBehandling.status == BehandlingStatus.UTREDES }
        assertBehandlingHarSteg(oppdatertBehandling, VEDTAK, KLAR)
        assertBehandlingHarSteg(oppdatertBehandling, BESLUTTE_VEDTAK, TILBAKEFØRT)
    }

    @Test
    @Transactional
    fun `utførStegEtterIverksettelseAutomatisk skal utføre AVSLUTT_BEHANDLING steg automatisk`() {
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, JOURNALFØR_VEDTAKSBREV, UTFØRT))
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, AVSLUTT_BEHANDLING, KLAR))
        behandling.status = BehandlingStatus.IVERKSETTER_VEDTAK
        behandling.resultat = Behandlingsresultat.INNVILGET
        lagTilkjentYtelse("")
        val andelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                kalkulertUtbetalingsbeløp = 1000,
                behandling = behandling,
            )
        andelTilkjentYtelseRepository.saveAndFlush(andelTilkjentYtelse)
        val lagretBehandling = lagreBehandling(behandling)

        assertDoesNotThrow { stegService.utførStegEtterIverksettelseAutomatisk(lagretBehandling.id) }
        assertNotNull(
            taskService.lagredeTasker.find {
                it.type == SendBehandlinghendelseTilDvhV2Task.TASK_TYPE
            },
        )

        val oppdatertBehandling = behandlingRepository.hentBehandling(lagretBehandling.id)
        assertBehandlingHarSteg(oppdatertBehandling, JOURNALFØR_VEDTAKSBREV, UTFØRT)
        assertBehandlingHarSteg(oppdatertBehandling, AVSLUTT_BEHANDLING, UTFØRT)
    }

    @Test
    fun `utførStegEtterIverksettelseAutomatisk skal opprette task for å utføre JOURNALFØR_VEDTAKSBREV steg automatisk`() {
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, IVERKSETT_MOT_OPPDRAG, UTFØRT))
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, JOURNALFØR_VEDTAKSBREV, KLAR))
        behandling.status = BehandlingStatus.IVERKSETTER_VEDTAK
        lagreBehandling(behandling)
        vedtakRepository.saveAndFlush(Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now()))

        assertDoesNotThrow { stegService.utførStegEtterIverksettelseAutomatisk(behandling.id) }

        assertNotNull(
            taskService.lagredeTasker.find {
                it.type == JournalførVedtaksbrevTask.TASK_STEP_TYPE
            },
        )
    }

    @Test
    fun `utførStegEtterIverksettelseAutomatisk skal opprette tilbakekreving task for revurdering`() {
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, IVERKSETT_MOT_OPPDRAG, UTFØRT))
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, JOURNALFØR_VEDTAKSBREV, KLAR))
        behandling.status = BehandlingStatus.IVERKSETTER_VEDTAK
        lagreBehandling(behandling)
        vedtakRepository.saveAndFlush(Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now()))
        tilbakekrevingRepository.save(
            Tilbakekreving(
                behandling = behandling,
                valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
                begrunnelse = "begrunnelse",
                tilbakekrevingsbehandlingId = null,
            ),
        )

        assertDoesNotThrow { stegService.utførStegEtterIverksettelseAutomatisk(behandling.id) }
        assertNotNull(
            taskService.lagredeTasker.find {
                it.type == SendOpprettTilbakekrevingsbehandlingRequestTask.TASK_STEP_TYPE
            },
        )
        assertNotNull(
            taskService.lagredeTasker.find {
                it.type == JournalførVedtaksbrevTask.TASK_STEP_TYPE
            },
        )
    }

    @Test
    fun `utførStegEtterIverksettelseAutomatisk skal ikke opprette tilbakekreving task for revurdering med valg IGNORER_TILBAKEKREVING`() {
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, IVERKSETT_MOT_OPPDRAG, UTFØRT))
        behandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(behandling, JOURNALFØR_VEDTAKSBREV, KLAR))
        behandling.status = BehandlingStatus.IVERKSETTER_VEDTAK
        lagreBehandling(behandling)
        vedtakRepository.saveAndFlush(Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now()))
        tilbakekrevingRepository.save(
            Tilbakekreving(
                behandling = behandling,
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "begrunnelse",
                tilbakekrevingsbehandlingId = null,
            ),
        )

        assertDoesNotThrow { stegService.utførStegEtterIverksettelseAutomatisk(behandling.id) }
        assertNotNull(
            taskService.lagredeTasker.find {
                it.type == JournalførVedtaksbrevTask.TASK_STEP_TYPE
            },
        )
    }

    @Test
    fun `utførSteg skal utføre TEKNISK_ENDRING behandling som ikke iverksetter og ikke sender brev til bruker`() {
        val aktør = lagreAktør(aktør = randomAktør())
        val fagsak = lagreFagsak(lagFagsak(aktør = aktør, status = FagsakStatus.LØPENDE))
        val tekniskEndringBehandling = lagreBehandling(lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.TEKNISK_ENDRING, type = BehandlingType.TEKNISK_ENDRING))
        opprettPersonopplysningGrunnlagOgPersonForBehandling(tekniskEndringBehandling.id)
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = tekniskEndringBehandling.id))

        tekniskEndringBehandling.resultat = Behandlingsresultat.ENDRET_UTEN_UTBETALING
        tekniskEndringBehandling.status = BehandlingStatus.FATTER_VEDTAK
        tekniskEndringBehandling.behandlingStegTilstand.clear()
        tekniskEndringBehandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(tekniskEndringBehandling, BESLUTTE_VEDTAK, KLAR))
        lagreBehandling(tekniskEndringBehandling)
        vedtakRepository.saveAndFlush(Vedtak(behandling = tekniskEndringBehandling, vedtaksdato = LocalDateTime.now()))

        val beslutteVedtakDto = BesluttVedtakDto(beslutning = Beslutning.GODKJENT, begrunnelse = "Godkjent")
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = tekniskEndringBehandling)

        assertDoesNotThrow { stegService.utførSteg(tekniskEndringBehandling.id, BESLUTTE_VEDTAK, beslutteVedtakDto) }

        val oppdatertBehandling = behandlingRepository.hentBehandling(tekniskEndringBehandling.id)
        assertBehandlingHarSteg(oppdatertBehandling, BESLUTTE_VEDTAK, UTFØRT)
        assertBehandlingHarSteg(oppdatertBehandling, AVSLUTT_BEHANDLING, KLAR)
    }

    @Test
    fun `utførSteg skal utføre TEKNISK_ENDRING behandling som iverksetter`() {
        val aktør = lagreAktør(aktør = randomAktør())
        val fagsak = lagreFagsak(lagFagsak(aktør = aktør, status = FagsakStatus.LØPENDE))
        val tekniskEndringBehandling = lagreBehandling(lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.TEKNISK_ENDRING, type = BehandlingType.TEKNISK_ENDRING))
        lagreArbeidsfordeling(lagArbeidsfordelingPåBehandling(behandlingId = tekniskEndringBehandling.id))

        lagTilkjentYtelse("")
        val andelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                kalkulertUtbetalingsbeløp = 1000,
                behandling = tekniskEndringBehandling,
            )
        andelTilkjentYtelseRepository.saveAndFlush(andelTilkjentYtelse)

        tekniskEndringBehandling.resultat = Behandlingsresultat.ENDRET_UTBETALING
        tekniskEndringBehandling.status = BehandlingStatus.FATTER_VEDTAK
        tekniskEndringBehandling.behandlingStegTilstand.clear()
        tekniskEndringBehandling.behandlingStegTilstand.add(lagBehandlingStegTilstand(tekniskEndringBehandling, BESLUTTE_VEDTAK, KLAR))
        lagreBehandling(tekniskEndringBehandling)
        vedtakRepository.saveAndFlush(Vedtak(behandling = tekniskEndringBehandling, vedtaksdato = LocalDateTime.now()))

        val beslutteVedtakDto = BesluttVedtakDto(beslutning = Beslutning.GODKJENT, begrunnelse = "Godkjent")
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = tekniskEndringBehandling)
        assertDoesNotThrow { stegService.utførSteg(tekniskEndringBehandling.id, BESLUTTE_VEDTAK, beslutteVedtakDto) }

        val oppdatertBehandling = behandlingRepository.hentBehandling(tekniskEndringBehandling.id)
        assertBehandlingHarSteg(oppdatertBehandling, BESLUTTE_VEDTAK, UTFØRT)
        assertBehandlingHarSteg(oppdatertBehandling, IVERKSETT_MOT_OPPDRAG, KLAR)
    }

    private fun assertBehandlingHarSteg(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg,
        behandlingStegStatus: BehandlingStegStatus,
    ) = assertTrue(
        behandling.behandlingStegTilstand.any {
            it.behandlingSteg == behandlingSteg &&
                it.behandlingStegStatus == behandlingStegStatus
        },
    )
}
