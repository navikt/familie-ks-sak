package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.DatabaseCleanupService
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class FagsakServiceIntegrasjonTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @BeforeEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `ikke oppdater status på fagsaker som er løpende og har løpende utbetalinger`() {
        val søker = randomAktør(randomFnr())

        opprettSøkerFagsakOgBehandling(søker, fagsakStatus = FagsakStatus.LØPENDE)

        val søkersFagsak = fagsakRepository.finnFagsakForAktør(søker)!!
        val behandling = behandlingRepository.findByFagsakAndAktiv(søkersFagsak.id)!!

        opprettOgLagreAndeler(
            behandling = behandling,
            offsetPåAndeler = listOf(1L),
            aktør = behandling.fagsak.aktør,
        )

        val løpendeFagsaker = fagsakRepository.finnLøpendeFagsaker()

        Assertions.assertTrue(løpendeFagsaker.any { it.id == søkersFagsak.id })

        fagsakService.finnOgAvsluttFagsakerSomSkalAvsluttes()
        val løpendeFagsaker2 = fagsakRepository.finnLøpendeFagsaker()

        Assertions.assertTrue(løpendeFagsaker2.any { it.id == søkersFagsak.id })
    }

    @Test
    fun `skal sette status til avsluttet hvis ingen løpende utbetalinger`() {
        val søker = randomAktør(randomFnr())

        opprettSøkerFagsakOgBehandling(søker, fagsakStatus = FagsakStatus.LØPENDE)

        val søkersFagsak = fagsakRepository.finnFagsakForAktør(søker)!!
        val søkersBehandling = behandlingRepository.findByFagsakAndAktiv(søkersFagsak.id)!!

        opprettOgLagreAndeler(
            behandling = søkersBehandling,
            offsetPåAndeler = listOf(1L),
            aktør = søkersBehandling.fagsak.aktør,
        )

        val tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(søkersBehandling.id)
        tilkjentYtelse.stønadTom = LocalDate.now().minusMonths(1).toYearMonth()
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)

        fagsakService.finnOgAvsluttFagsakerSomSkalAvsluttes()
        val løpendeFagsaker = fagsakRepository.finnLøpendeFagsaker()

        Assertions.assertFalse(løpendeFagsaker.any { it.id == søkersFagsak.id })
    }

    @Test
    fun `Skal kun hente løpende fagsak for søker`() {
        val søker = randomAktør(randomFnr())

        opprettSøkerFagsakOgBehandling(søker, fagsakStatus = FagsakStatus.LØPENDE)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandling.id)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeKontantstøtte(søker)

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
    }

    @Test
    fun `Skal hente fagsak hvor barn har løpende andel`() {
        val søker = randomAktør(randomFnr())

        opprettSøkerFagsakOgBehandling(søker, fagsakStatus = FagsakStatus.LØPENDE)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)

        opprettOgLagreAndeler(
            behandling = behandling,
            offsetPåAndeler = listOf(1L),
            aktør = barn,
        )

        lagreBehandling(behandling.apply { status = BehandlingStatus.AVSLUTTET })

        val fagsakDerBarnharLøpendeAndel = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeKontantstøtte(barn)

        assertEquals(1, fagsakDerBarnharLøpendeAndel.size)
    }

    private fun opprettOgLagreAndeler(
        behandling: Behandling,
        offsetPåAndeler: List<Long> = emptyList(),
        erIverksatt: Boolean = true,
        aktør: Aktør,
    ): Behandling {
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        val andelerTilkjentYtelse =
            offsetPåAndeler.map {
                andelPåTilkjentYtelse(tilkjentYtelse = tilkjentYtelse, periodeOffset = it, aktør = aktør)
            }
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)
        return behandling
    }

    private fun tilkjentYtelse(
        behandling: Behandling,
        erIverksatt: Boolean,
    ) = TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
        utbetalingsoppdrag = if (erIverksatt) "Skal ikke være null" else null,
    )

    private data class PeriodeForAktør(
        val fom: YearMonth,
        val tom: YearMonth,
        val aktør: Aktør,
        val ytelseType: YtelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
    )

    private fun andelPåTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        periodeOffset: Long,
        aktør: Aktør = randomAktør(),
    ) = AndelTilkjentYtelse(
        aktør = aktør,
        behandlingId = tilkjentYtelse.behandling.id,
        tilkjentYtelse = tilkjentYtelse,
        kalkulertUtbetalingsbeløp = 1054,
        nasjonaltPeriodebeløp = 1054,
        stønadFom =
            LocalDate
                .now()
                .minusMonths(12)
                .toYearMonth(),
        stønadTom =
            LocalDate
                .now()
                .plusMonths(12)
                .toYearMonth(),
        type = YtelseType.ORDINÆR_KONTANTSTØTTE,
        periodeOffset = periodeOffset,
        forrigePeriodeOffset = null,
        sats = 1054,
        prosent = BigDecimal(100),
    )
}
