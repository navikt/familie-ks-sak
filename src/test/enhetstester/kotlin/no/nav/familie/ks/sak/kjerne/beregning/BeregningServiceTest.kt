package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class BeregningServiceTest {

    @MockK
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var fagsakService: FagsakService

    @InjectMockKs
    private lateinit var beregningService: BeregningService

    @Test
    fun `finnBarnFraBehandlingMedTilkjentYtelse skal returnere når tom liste når det ikke finnes en andel tilkjent ytelse`() {
        val behandlngId = 111L
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlngId) } returns emptyList()
        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlngId) } returns
            lagPersonopplysningGrunnlag(
                behandlngId,
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr())
            )

        assertTrue { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandlngId).isEmpty() }
    }

    @Test
    fun `finnBarnFraBehandlingMedTilkjentYtelse skal returnere tom liste når det ikke finnes barn`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(lagAndelTilkjentYtelse(behandling = behandling))
        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                barnasIdenter = emptyList()
            )

        assertTrue { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id).isEmpty() }
    }

    @Test
    fun `finnBarnFraBehandlingMedTilkjentYtelse skal returnere liste med barn når det finnes andeler for dem`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val tilkjentYtelse = lagInitieltTilkjentYtelse(behandling)
        val barnAktør = randomAktør()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = behandling.fagsak.aktør
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = barnAktør
                )
            )
        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør)
            )
        val barn = beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id)
        assertTrue { barn.isNotEmpty() }
        assertEquals(1, barn.size)
        assertEquals(barnAktør, barn.single())
    }

    @Test
    fun `hentRelevanteTilkjentYtelserForBarn - skal hente tilkjente ytelser fra behandlinger sendt til godkjenning som barnet forekommer i`() {
        val barnAktør = randomAktør()
        val fagak = lagFagsak(barnAktør, 1)
        val annenFagsak = lagFagsak(barnAktør, 2)
        val behandlingTilGodkjenning = lagBehandling(annenFagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandlingTilGodkjenning,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = behandlingTilGodkjenning,
                    aktør = barnAktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(2),
                    sats = 8000
                )
            )
        )

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingTilGodkjenning.id,
            "12345678910",
            listOf(barnAktør.aktivFødselsnummer()),
            barnAktør = listOf(barnAktør)
        )
        every { fagsakService.hentFagsakerPåPerson(barnAktør) } returns listOf(
            fagak,
            annenFagsak
        )

        every { behandlingRepository.finnBehandlingerSendtTilGodkjenning(any()) } returns listOf(
            behandlingTilGodkjenning
        )

        every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandlingTilGodkjenning.id) } returns tilkjentYtelse

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingTilGodkjenning.id) } returns personopplysningGrunnlag

        val relevanteTilkjenteYtelserForBarn = beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør, fagak.id)

        assertEquals(behandlingTilGodkjenning.id, relevanteTilkjenteYtelserForBarn.single().behandling.id)
    }

    @Test
    fun `hentRelevanteTilkjentYtelserForBarn - skal hente tilkjente ytelser fra godkjente behandlinger som ikke er iverksatt som barnet forekommer i`() {
        val barnAktør = randomAktør()
        val fagak = lagFagsak(barnAktør, 1)
        val annenFagsak = lagFagsak(barnAktør, 2)
        val godkjentBehandlingSomIkkeErIverksatt =
            lagBehandling(fagsak = annenFagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val tilkjentYtelse = TilkjentYtelse(
            behandling = godkjentBehandlingSomIkkeErIverksatt,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = godkjentBehandlingSomIkkeErIverksatt,
                    aktør = barnAktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(2),
                    sats = 8000
                )
            )
        )

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            godkjentBehandlingSomIkkeErIverksatt.id,
            "12345678910",
            listOf(barnAktør.aktivFødselsnummer()),
            barnAktør = listOf(barnAktør)
        )
        every { fagsakService.hentFagsakerPåPerson(barnAktør) } returns listOf(
            fagak,
            annenFagsak
        )

        every { behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsakId = annenFagsak.id) } returns emptyList()

        every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(godkjentBehandlingSomIkkeErIverksatt.id) } returns tilkjentYtelse

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(godkjentBehandlingSomIkkeErIverksatt.id) } returns personopplysningGrunnlag

        every { behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = annenFagsak.id) } returns listOf(
            godkjentBehandlingSomIkkeErIverksatt
        )

        val relevanteTilkjenteYtelserForBarn = beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør, fagak.id)

        assertEquals(godkjentBehandlingSomIkkeErIverksatt.id, relevanteTilkjenteYtelserForBarn.single().behandling.id)
    }

    @Test
    fun `hentRelevanteTilkjentYtelserForBarn - skal hente tilkjente ytelser fra iverksatte behandlinger som barnet forekommer i`() {
        val barnAktør = randomAktør()
        val fagak = lagFagsak(barnAktør, 1)
        val annenFagsak = lagFagsak(barnAktør, 2)
        val iverksatteBehandlinger = lagBehandling(
            fagsak = annenFagsak,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD
        )
        iverksatteBehandlinger.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = iverksatteBehandlinger,
                behandlingSteg = BehandlingSteg.BEHANDLING_AVSLUTTET
            )
        )

        val tilkjentYtelse = TilkjentYtelse(
            behandling = iverksatteBehandlinger,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = iverksatteBehandlinger,
                    aktør = barnAktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(2),
                    sats = 8000
                )
            )
        )

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            iverksatteBehandlinger.id,
            "12345678910",
            listOf(barnAktør.aktivFødselsnummer()),
            barnAktør = listOf(barnAktør)
        )
        every { fagsakService.hentFagsakerPåPerson(barnAktør) } returns listOf(
            fagak,
            annenFagsak
        )

        every { behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsakId = annenFagsak.id) } returns emptyList()

        every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(iverksatteBehandlinger.id) } returns tilkjentYtelse

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(iverksatteBehandlinger.id) } returns personopplysningGrunnlag

        every { behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = annenFagsak.id) } returns emptyList()

        every { behandlingRepository.finnIverksatteBehandlinger(fagsakId = annenFagsak.id) } returns listOf(
            iverksatteBehandlinger
        )

        val relevanteTilkjenteYtelserForBarn = beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør, fagak.id)

        assertEquals(iverksatteBehandlinger.id, relevanteTilkjenteYtelserForBarn.single().behandling.id)
    }

    @Test
    fun `hentRelevanteTilkjentYtelserForBarn - skal returnere tom liste dersom det ikke finnes noen andre fagsaker tilknyttet barnet`() {
        val barnAktør = randomAktør()
        val fagak = lagFagsak(barnAktør, 1)

        every { fagsakService.hentFagsakerPåPerson(barnAktør) } returns listOf(
            fagak
        )

        val relevanteTilkjenteYtelserForBarn = beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør, fagak.id)

        assertEquals(0, relevanteTilkjenteYtelserForBarn.size)
    }
}
