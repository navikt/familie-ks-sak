package no.nav.familie.ks.sak.kjerne.fagsak.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.task.LåsFagsakTask
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var taskService: TaskService

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `finnFagsak skal returnere fagsak dersom det eksisterer en fagsak med id`() {
        // Arrange (data er opprettet av @BeforeEach)

        // Act
        val hentetFagsak = fagsakRepository.finnFagsak(fagsak.id)!!

        // Assert
        assertThat(hentetFagsak.id, Is(fagsak.id))
        assertThat(hentetFagsak.aktør, Is(fagsak.aktør))
    }

    @Test
    fun `finnFagsak skal returnere null dersom det ikke eksisterer en fagsak med id`() {
        // Arrange

        // Act
        val ikkeEksisterendeFagsak = fagsakRepository.finnFagsak(404)

        // Assert
        assertThat(ikkeEksisterendeFagsak, Is(nullValue()))
    }

    @Test
    fun `finnFagsakForAktør skal returnere null dersom det ikke finnes fagsak for aktør`() {
        // Arrange
        val randomAktør = randomAktør()

        // Act
        val ikkeEksisterendeFagsak = fagsakRepository.finnFagsakForAktør(randomAktør)

        // Assert
        assertThat(ikkeEksisterendeFagsak, Is(nullValue()))
    }

    @Test
    fun `finnFagsakForAktør skal returnere fagsak dersom det finnes fagsak for aktør`() {
        // Arrange (data er opprettet av @BeforeEach)

        // Act
        val hentetFagsak = fagsakRepository.finnFagsakForAktør(søker)!!

        // Assert
        assertThat(hentetFagsak.id, Is(fagsak.id))
        assertThat(hentetFagsak.aktør, Is(fagsak.aktør))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal returnere fagsak når siste utbetaling og siste vedtak var for mer enn 1 år siden`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(listOf(fagsak.id)))
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, names = ["FERDIG", "AVVIKSHÅNDTERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `finnAvsluttedeFagsakerSomSkalLåses skal ikke returnere fagsak som allerede har en LåsFagsakTask som ikke er ferdig behandlet`(status: Status) {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))
        lagreLåsFagsakTask(fagsakId = fagsak.id, status = status)

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(emptyList<Long>()))
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, names = ["FERDIG", "AVVIKSHÅNDTERT"])
    fun `finnAvsluttedeFagsakerSomSkalLåses skal returnere fagsak som har en LåsFagsakTask som er ferdig behandlet`(status: Status) {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))
        lagreLåsFagsakTask(fagsakId = fagsak.id, status = status)

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(listOf(fagsak.id)))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal returnere fagsak når det kun finnes en uferdig LåsFagsakTask for en annen fagsak`() {
        // Arrange
        val annenFagsakId = fagsak.id
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))
        lagreLåsFagsakTask(fagsakId = annenFagsakId, status = Status.UBEHANDLET)

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(listOf(fagsak.id)))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal ikke returnere fagsak når siste utbetaling var for under 1 år siden`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusMonths(6))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(emptyList()))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal ikke returnere fagsak når siste vedtak var for under 1 år siden`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusMonths(6))

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(emptyList()))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal ignorere henlagte behandlinger`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(emptyList()))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal returnere fagsak når det ikke finnes tilkjent ytelse`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(listOf(fagsak.id)))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal returnere fagsak når det ikke finnes aktivt vedtak`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(listOf(fagsak.id)))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal returnere fagsaken kun én gang selv om behandlingen har flere tilkjente ytelser`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
        lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(3))
        lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

        // Assert
        assertThat(fagsakerSomSkalLåses, Is(listOf(fagsak.id)))
    }

    @Test
    fun `finnAvsluttedeFagsakerSomSkalLåses skal ikke returnere flere fagsaker enn maksAntall`() {
        // Arrange
        repeat(3) {
            opprettSøkerFagsakOgBehandling(
                fagsakStatus = FagsakStatus.AVSLUTTET,
                behandlingStatus = BehandlingStatus.AVSLUTTET,
                behandlingResultat = Behandlingsresultat.INNVILGET,
            )
            lagreTilkjentYtelseMedStønadTom(YearMonth.now().minusYears(2))
            lagreVedtakMedVedtaksdato(LocalDateTime.now().minusYears(2))
        }

        // Act
        val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 2)

        // Assert
        assertThat(fagsakerSomSkalLåses.size, Is(2))
    }

    private fun lagreTilkjentYtelseMedStønadTom(stønadTom: YearMonth) {
        tilkjentYtelseRepository.saveAndFlush(
            TilkjentYtelse(
                behandling = behandling,
                stønadTom = stønadTom,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
            ),
        )
    }

    private fun lagreVedtakMedVedtaksdato(vedtaksdato: LocalDateTime) {
        vedtakRepository.saveAndFlush(Vedtak(behandling = behandling, vedtaksdato = vedtaksdato))
    }

    private fun lagreLåsFagsakTask(
        fagsakId: Long,
        status: Status,
    ) {
        // triggerTid frem i tid slik at TaskScheduler ikke plukker tasken mens testen kjører
        taskService.save(
            LåsFagsakTask
                .opprettTask(fagsakId)
                .copy(status = status, triggerTid = LocalDateTime.now().plusDays(1)),
        )
    }
}
