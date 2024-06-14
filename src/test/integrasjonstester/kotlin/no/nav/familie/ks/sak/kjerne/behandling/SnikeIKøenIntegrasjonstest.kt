package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.config.DatabaseCleanupService
import no.nav.familie.ks.sak.data.lagInitiellTilkjentYtelse
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.Reaktivert
import no.nav.familie.ks.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
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
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import java.time.LocalDate

class SnikeIKøenIntegrasjonstest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val snikeIKøenService: SnikeIKøenService,
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
        settBehandlingPåVent(behandling1, LocalDate.now(), VenteÅrsak.AVVENTER_DOKUMENTASJON)
        settAktivBehandlingTilPåMaskinellVent(behandling1)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling1.id)
        val oppdatertBehandlingStegTilstand = oppdatertBehandling.behandlingStegTilstand.single { it.behandlingSteg == oppdatertBehandling.steg }

        assertThat(oppdatertBehandlingStegTilstand.behandlingStegStatus).isEqualTo(BehandlingStegStatus.VENTER)
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
        behandling =
            opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false).let {
                leggTilSteg(it, BehandlingSteg.VILKÅRSVURDERING, BehandlingStegStatus.UTFØRT)
                leggTilSteg(it, BehandlingSteg.SIMULERING)
                behandlingRepository.saveAndFlush(it)
            }
        lagVedtak()
        vedtaksperiodeHentOgPersisterService.lagre(
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            ),
        )
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        assertThat(vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(vedtak.id)).isEmpty()
    }

    @Test
    fun `reaktivering skal tilbakestille til vilkårsvurdering kun dersom steget er lagt til på behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val initielStegTilstand = BehandlingSteg.REGISTRERE_SØKNAD

        leggTilSteg(behandling1, initielStegTilstand)
        behandlingRepository.saveAndFlush(behandling1)

        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.hentBehandling(behandling1.id)

        assertThat(oppdatertBehandling.steg).isEqualTo(initielStegTilstand)

        leggTilSteg(oppdatertBehandling, BehandlingSteg.VILKÅRSVURDERING, BehandlingStegStatus.UTFØRT)
        leggTilSteg(oppdatertBehandling, BehandlingSteg.BEHANDLINGSRESULTAT)
        settBehandlingTilPåMaskinellVent(oppdatertBehandling)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertStegTilstand = behandlingRepository.hentBehandling(behandling1.id).behandlingStegTilstand.last()

        assertThat(oppdatertStegTilstand.behandlingSteg).isEqualTo(BehandlingSteg.VILKÅRSVURDERING)
        assertThat(oppdatertStegTilstand.behandlingStegStatus).isEqualTo(BehandlingStegStatus.KLAR)
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

        assertThat(snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)).isEqualTo(Reaktivert.NEI)
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

        with (behandlingRepository) {
            val sisteIverksatteBehandlingPåFagsak = finnIverksatteBehandlinger(fagsakId).filter { it.erAvsluttet() }.maxBy { it.aktivertTidspunkt }
            val sisteIverksatteBehandlingFraLøpendeFagsaker = finnSisteIverksatteBehandlingFraLøpendeFagsaker(page = Pageable.unpaged())

            assertThat(sisteIverksatteBehandlingPåFagsak.id).isEqualTo(behandling.id)
            assertThat(sisteIverksatteBehandlingFraLøpendeFagsaker).containsExactly(behandling.id)
        }

        with(behandlingService) {
            assertThat(hentSisteBehandlingSomErIverksatt(fagsakId)!!.id).isEqualTo(behandling.id)
            assertThat(hentSisteBehandlingSomErVedtatt(fagsakId)!!.id).isEqualTo(behandling.id)
            assertThat(
                hentSisteBehandlingSomErAvsluttetEllerSendtTilØkonomiPerFagsak(setOf(fagsakId)).single().id,
            ).isEqualTo(behandling.id)
        }
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

    private fun opprettLøpendeFagsak(): Fagsak {
        val aktør = tilfeldigPerson().aktør
        with(fagsakService) {
            val fagsakId = hentEllerOpprettFagsak(FagsakRequestDto(aktør.aktivFødselsnummer())).id
            val fagsak = hentFagsak(fagsakId)

            return oppdaterStatus(fagsak, FagsakStatus.LØPENDE)
        }
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

    private fun settBehandlingPåVent(behandling: Behandling, frist: LocalDate, årsak: VenteÅrsak) {
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }
        behandlingStegTilstand.frist = frist
        behandlingStegTilstand.årsak = årsak
        behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.VENTER
        behandlingRepository.saveAndFlush(behandling)
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
