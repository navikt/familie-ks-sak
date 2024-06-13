package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.config.DatabaseCleanupService
import no.nav.familie.ks.sak.data.lagInitiellTilkjentYtelse
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SnikeIKøenServiceTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val snikeIKøenService: SnikeIKøenService,
    @Autowired private val vedtakRepository: VedtakRepository,
    @Autowired private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    @Autowired private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) : OppslagSpringRunnerTest() {
    private var skalVenteLitt = false // for å unngå at behandlingen opprettes med samme tidspunkt

    @BeforeEach
    fun setUp() {
        skalVenteLitt = false
        databaseCleanupService.truncate()
        fagsak = opprettLøpendeFagsak()
    }

    private fun opprettLøpendeFagsak(): Fagsak {
        val aktør = tilfeldigPerson().aktør
        return with(fagsakService) {
            lagre(
                hentEllerOpprettFagsak(FagsakRequestDto(aktør.aktivFødselsnummer())).let {
                    hentFagsak(it.id).copy(status = FagsakStatus.LØPENDE)
                }
            )
        }
    }

    @ParameterizedTest
    @EnumSource(BehandlingStatus::class, names = ["UTREDES", "SATT_PÅ_VENT"], mode = EnumSource.Mode.INCLUDE)
    fun `skal kunne sette en behandling med status UTREDES eller SATT_PÅ_VENT på maskinell vent`(status: BehandlingStatus) {
        val behandling = opprettBehandling(status = status)

        settAktivBehandlingTilPåMaskinellVent(behandling)

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling.id)
        assertThat(behandling.status).isEqualTo(status)
        assertThat(behandling.aktiv).isTrue()
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
        assertThat(oppdatertBehandling.aktiv).isFalse()
    }

    @Test
    fun `reaktivering av behandling skal sette status tilbake til UTREDES`() {
        val behandling1 = opprettBehandling()
        settAktivBehandlingTilPåMaskinellVent(behandling1)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling1.id)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(oppdatertBehandling.aktiv).isTrue()
    }

    @Test
    fun `reaktivering av behandling som er på vent skal sette status tilbake til SATT_PÅ_VENT`() {
        val behandling1 = opprettBehandling()
        lagreArbeidsfordeling(behandling1)
        settPåVentService.settBehandlingPåVent(behandling1.id, LocalDate.now(), SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
        settAktivBehandlingTilPåMaskinellVent(behandling1)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling1.id)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
        assertThat(oppdatertBehandling.aktiv).isTrue()
    }

    @Test
    fun `siste behandlingen er den som er aktiv til at behandlingen som er satt på vent er aktivert på nytt`() {
        opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        validerSisteBehandling(behandling2)
        validerErAktivBehandling(behandling2)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert, men ennå ikke iverksatt, og er då siste aktive behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        validerSisteBehandling(behandling2)
        validerErAktivBehandling(behandling1)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert og iverksatt, og er då siste aktive behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)
        lagUtbetalingsoppdragOgAvslutt(behandling1)

        validerSisteBehandling(behandling1)
        validerErAktivBehandling(behandling1)
    }

    @Test
    fun `reaktivering skal tilbakestille behandling på vent`() {
        val behandling1 =
            opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false).let {
                leggTilSteg(it, StegType.VILKÅRSVURDERING, BehandlingStegStatus.UTFØRT)
                leggTilSteg(it, StegType.VURDER_TILBAKEKREVING)
                behandlingRepository.saveAndFlush(it)
            }
        val vedtak = vedtakRepository.saveAndFlush(lagVedtak(behandling = behandling1))
        vedtaksperiodeHentOgPersisterService.lagre(
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            ),
        )
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        assertThat(vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtak.id)).isEmpty()
    }

    @Test
    fun `reaktivering skal tilbakestille til vilkårsvurdering kun dersom steget er lagt til på behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val initielStegTilstand = StegType.REGISTRERE_SØKNAD

        leggTilSteg(behandling1, initielStegTilstand)
        behandlingRepository.saveAndFlush(behandling1)

        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling1.id)

        assertThat(oppdatertBehandling.steg).isEqualTo(initielStegTilstand)

        leggTilSteg(oppdatertBehandling, StegType.VILKÅRSVURDERING, BehandlingStegStatus.UTFØRT)
        leggTilSteg(oppdatertBehandling, StegType.BEHANDLINGSRESULTAT)
        settBehandlingTilPåMaskinellVent(oppdatertBehandling)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertStegTilstand = behandlingRepository.hentBehandling(behandling1.id).behandlingStegTilstand.last()

        assertThat(oppdatertStegTilstand.behandlingSteg).isEqualTo(StegType.VILKÅRSVURDERING)
        assertThat(oppdatertStegTilstand.behandlingStegStatus).isEqualTo(BehandlingStegStatus.IKKE_UTFØRT)
    }

    private fun settBehandlingTilPåMaskinellVent(oppdatertBehandling: Behandling) {
        oppdatertBehandling.aktiv = false
        oppdatertBehandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandlingRepository.saveAndFlush(oppdatertBehandling)
    }

    @Test
    fun `skal ikke reaktivere noe hvis det ikke finnes en behandling som er på maskinell vent`() {
        val behandling2 =
            opprettBehandling(
                status = BehandlingStatus.AVSLUTTET,
                aktiv = true,
            )

        assertThat(snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)).isFalse()
        assertThat(behandlingRepository.hentBehandling(behandling2.id).aktiv).isTrue()
    }

    @Test
    fun `skal kunne reaktivere en behandling selv om det ikke finnes en annen behandling som er aktiv, eks henlagt`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val behandling2 =
            opprettBehandling(
                status = BehandlingStatus.AVSLUTTET,
                aktiv = false,
                resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
            )

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        assertThat(behandlingRepository.hentBehandling(behandling1.id).aktiv).isTrue()
        assertThat(behandlingRepository.hentBehandling(behandling2.id).aktiv).isFalse()
    }

    @Nested
    inner class ValideringAvSettPåVent {
        @Test
        fun `kan ikke sette en inaktiv behandling på vent`() {
            val behandling = opprettBehandling(aktiv = false)

            assertThatThrownBy {
                settAktivBehandlingTilPåMaskinellVent(behandling)
            }.hasMessageContaining("er ikke aktiv")
        }

        @ParameterizedTest
        @EnumSource(BehandlingStatus::class, names = ["UTREDES", "SATT_PÅ_VENT"], mode = EnumSource.Mode.EXCLUDE)
        fun `kan ikke sette en behandling på vent med annen status enn UTREDES eller SATT_PÅ_VENT`(status: BehandlingStatus) {
            val behandling = opprettBehandling(status = status)

            assertThatThrownBy {
                settAktivBehandlingTilPåMaskinellVent(behandling)
            }.hasMessageContaining("kan ikke settes på maskinell vent då status")
        }
    }

    @Nested
    inner class ValideringAvReaktiverBehandling {
        @Suppress("UNUSED_VARIABLE")
        @Test
        fun `skal feile når åpen behandling er aktiv`() {
            val behandlingPåVent = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = true)
            val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false)

            assertThatThrownBy { snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen) }
                .hasMessageContaining("Åpen behandling har feil tilstand")
        }

        @Test
        fun `skal feile når behandling som snek i køen har status satt på vent`() {
            opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
            val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.UTREDES, aktiv = true)

            assertThatThrownBy { snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen) }
                .hasMessageContaining("er ikke avsluttet")
        }
    }

    private fun settAktivBehandlingTilPåMaskinellVent(behandling: Behandling) {
        snikeIKøenService.settAktivBehandlingPåMaskinellVent(behandling.id, SettPåMaskinellVentÅrsak.SATSENDRING)
    }

    private fun validerSisteBehandling(behandling: Behandling) {
        val fagsakId = fagsak.id
        assertThat(behandlingRepository.finnIverksatteBehandlinger(fagsakId).maxBy { it.aktivertTidspunkt }.id).isEqualTo(behandling.id)
        assertThat(behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()).containsExactly(behandling.id)
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsakId)!!.id).isEqualTo(behandling.id)

        assertThat(behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(
            behandlingService.hentSisteBehandlingSomErSendtTilØkonomiPerFagsak(setOf(fagsakId)).single().id,
        ).isEqualTo(behandling.id)
    }

    private fun validerErAktivBehandling(behandling: Behandling) {
        assertThat(behandlingService.hentAktivtBehandling(behandling.id).id).isEqualTo(behandling.id)
    }

    private fun lagUtbetalingsoppdragOgAvslutt(behandling: Behandling) {
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling, utbetalingsoppdrag = "utbetalingsoppdrag")
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)
        behandlingRepository.hentBehandling(behandling.id).let { behandlingFraDb ->
            leggTilSteg(behandlingFraDb, BehandlingSteg.AVSLUTT_BEHANDLING)
            behandlingFraDb.status = BehandlingStatus.AVSLUTTET
            behandlingRepository.saveAndFlush(behandlingFraDb)
        }
    }

    private fun leggTilSteg(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg,
        behandlingStegStatus: BehandlingStegStatus = BehandlingStegStatus.KLAR,
    ) {
        val stegTilstand =
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = behandlingSteg,
                behandlingStegStatus = behandlingStegStatus,
            )
        behandling.behandlingStegTilstand.add(stegTilstand)
    }

    private fun opprettBehandling(
        status: BehandlingStatus = BehandlingStatus.UTREDES,
        resultat: Behandlingsresultat = Behandlingsresultat.INNVILGET,
        aktiv: Boolean = true,
    ): Behandling = opprettBehandling(fagsak, status, resultat, aktiv)

    private fun opprettBehandling(
        fagsak: Fagsak,
        status: BehandlingStatus,
        resultat: Behandlingsresultat,
        aktiv: Boolean,
    ): Behandling {
        if (skalVenteLitt) {
            Thread.sleep(10)
        } else {
            skalVenteLitt = true
        }
        val behandling =
            Behandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                type = BehandlingType.REVURDERING,
                kategori = BehandlingKategori.NASJONAL,
                status = status,
                aktiv = aktiv,
                resultat = resultat,
            ).initBehandlingStegTilstand()
        return behandlingRepository.saveAndFlush(behandling)
    }

    private fun lagreArbeidsfordeling(behandling1: Behandling) {
        val arbeidsfordelingPåBehandling =
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling1.id,
                behandlendeEnhetId = "4820",
                behandlendeEnhetNavn = "Enhet",
            )
        arbeidsfordelingPåBehandlingRepository.saveAndFlush(arbeidsfordelingPåBehandling)
    }
}
