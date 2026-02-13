package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagTilkjentYtelse
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class BeregningServiceTest {
    private val mockTilkjentYtelseRepository: TilkjentYtelseRepository = mockk()
    private val mockAndelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val mockPersonopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()
    private val mockBehandlingRepository: BehandlingRepository = mockk()
    private val mockAndelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService = mockk()
    private val mockFagsakService: FagsakService = mockk()
    private val mockTilkjentYtelseService: TilkjentYtelseService = mockk()
    private val beregningService: BeregningService =
        BeregningService(
            tilkjentYtelseRepository = mockTilkjentYtelseRepository,
            andelTilkjentYtelseRepository = mockAndelTilkjentYtelseRepository,
            personopplysningGrunnlagRepository = mockPersonopplysningGrunnlagRepository,
            behandlingRepository = mockBehandlingRepository,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = mockAndelerTilkjentYtelseOgEndreteUtbetalingerService,
            fagsakService = mockFagsakService,
            tilkjentYtelseService = mockTilkjentYtelseService,
        )

    @Test
    fun `finnBarnFraBehandlingMedTilkjentYtelse skal returnere når tom liste når det ikke finnes en andel tilkjent ytelse`() {
        val behandlngId = 111L
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlngId) } returns emptyList()
        every { mockPersonopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlngId) } returns
            lagPersonopplysningGrunnlag(
                behandlngId,
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
            )

        assertTrue { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandlngId).isEmpty() }
    }

    @Test
    fun `finnBarnFraBehandlingMedTilkjentYtelse skal returnere tom liste når det ikke finnes barn`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(lagAndelTilkjentYtelse(behandling = behandling))
        every { mockPersonopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                barnasIdenter = emptyList(),
            )

        assertTrue { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id).isEmpty() }
    }

    @Test
    fun `finnBarnFraBehandlingMedTilkjentYtelse skal returnere liste med barn når det finnes andeler for dem`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val tilkjentYtelse = lagInitieltTilkjentYtelse(behandling)
        val barnAktør = randomAktør()
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = behandling.fagsak.aktør,
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = barnAktør,
                ),
            )
        every { mockPersonopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
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

        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = behandlingTilGodkjenning,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse =
                    mutableSetOf(
                        lagAndelTilkjentYtelse(
                            behandling = behandlingTilGodkjenning,
                            aktør = barnAktør,
                            stønadFom = YearMonth.now().minusMonths(4),
                            stønadTom = YearMonth.now().minusMonths(2),
                            sats = 8000,
                        ),
                    ),
            )

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingTilGodkjenning.id,
                randomFnr(),
                listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )
        every { mockFagsakService.hentFagsakerPåPerson(barnAktør) } returns
            listOf(
                fagak,
                annenFagsak,
            )

        every { mockBehandlingRepository.finnBehandlingerSendtTilGodkjenning(any()) } returns
            listOf(
                behandlingTilGodkjenning,
            )

        every { mockTilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandlingTilGodkjenning.id) } returns tilkjentYtelse

        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingTilGodkjenning.id) } returns personopplysningGrunnlag

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

        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = godkjentBehandlingSomIkkeErIverksatt,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse =
                    mutableSetOf(
                        lagAndelTilkjentYtelse(
                            behandling = godkjentBehandlingSomIkkeErIverksatt,
                            aktør = barnAktør,
                            stønadFom = YearMonth.now().minusMonths(4),
                            stønadTom = YearMonth.now().minusMonths(2),
                            sats = 8000,
                        ),
                    ),
            )

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                godkjentBehandlingSomIkkeErIverksatt.id,
                randomFnr(),
                listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )
        every { mockFagsakService.hentFagsakerPåPerson(barnAktør) } returns
            listOf(
                fagak,
                annenFagsak,
            )

        every { mockBehandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsakId = annenFagsak.id) } returns emptyList()

        every { mockTilkjentYtelseRepository.hentTilkjentYtelseForBehandling(godkjentBehandlingSomIkkeErIverksatt.id) } returns tilkjentYtelse

        every {
            mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(godkjentBehandlingSomIkkeErIverksatt.id)
        } returns personopplysningGrunnlag

        every { mockBehandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = annenFagsak.id) } returns
            listOf(
                godkjentBehandlingSomIkkeErIverksatt,
            )

        val relevanteTilkjenteYtelserForBarn = beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør, fagak.id)

        assertEquals(godkjentBehandlingSomIkkeErIverksatt.id, relevanteTilkjenteYtelserForBarn.single().behandling.id)
    }

    @Test
    fun `hentRelevanteTilkjentYtelserForBarn - skal hente tilkjente ytelser fra iverksatte behandlinger som barnet forekommer i`() {
        val barnAktør = randomAktør()
        val fagak = lagFagsak(barnAktør, 1)
        val annenFagsak = lagFagsak(barnAktør, 2)
        val iverksatteBehandlinger =
            lagBehandling(
                fagsak = annenFagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD,
            ).also { it.status = BehandlingStatus.AVSLUTTET }

        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = iverksatteBehandlinger,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse =
                    mutableSetOf(
                        lagAndelTilkjentYtelse(
                            behandling = iverksatteBehandlinger,
                            aktør = barnAktør,
                            stønadFom = YearMonth.now().minusMonths(4),
                            stønadTom = YearMonth.now().minusMonths(2),
                            sats = 8000,
                        ),
                    ),
            )

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                iverksatteBehandlinger.id,
                randomFnr(),
                listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )
        every { mockFagsakService.hentFagsakerPåPerson(barnAktør) } returns
            listOf(
                fagak,
                annenFagsak,
            )

        every { mockBehandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsakId = annenFagsak.id) } returns emptyList()

        every { mockTilkjentYtelseRepository.hentTilkjentYtelseForBehandling(iverksatteBehandlinger.id) } returns tilkjentYtelse

        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(iverksatteBehandlinger.id) } returns personopplysningGrunnlag

        every { mockBehandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = annenFagsak.id) } returns emptyList()

        every { mockBehandlingRepository.finnIverksatteBehandlinger(fagsakId = annenFagsak.id) } returns
            listOf(
                iverksatteBehandlinger,
            )

        val relevanteTilkjenteYtelserForBarn = beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør, fagak.id)

        assertEquals(iverksatteBehandlinger.id, relevanteTilkjenteYtelserForBarn.single().behandling.id)
    }

    @Test
    fun `hentRelevanteTilkjentYtelserForBarn - skal returnere tom liste dersom det ikke finnes noen andre fagsaker tilknyttet barnet`() {
        val barnAktør = randomAktør()
        val fagak = lagFagsak(barnAktør, 1)

        every { mockFagsakService.hentFagsakerPåPerson(barnAktør) } returns
            listOf(
                fagak,
            )

        val relevanteTilkjenteYtelserForBarn = beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør, fagak.id)

        assertEquals(0, relevanteTilkjenteYtelserForBarn.size)
    }

    @Test
    fun `hentAndelerTilkjentYtelseMedUtbetalingerForBehandling - skal returnere liste av andel tilkjent ytelse som har utbetalinger`() {
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD), sats = 5000),
            )

        val andelerTilkjentYtelseMedUtbetalingerForBehandling =
            beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(1)

        assertEquals(1, andelerTilkjentYtelseMedUtbetalingerForBehandling.size)
    }

    @Test
    fun `hentAndelerTilkjentYtelseMedUtbetalingerForBehandling - skal returnere tom liste dersom ingen andeler tilkjent ytelse har utbetalinger`() {
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD), sats = 0),
            )

        val andelerTilkjentYtelseMedUtbetalingerForBehandling =
            beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(1)

        assertEquals(0, andelerTilkjentYtelseMedUtbetalingerForBehandling.size)
    }

    @Test
    fun `hentTilkjentYtelseForBehandlingerIverksattMotØkonomi - skal returnere liste over tilkjente ytelser som inneholder andeler med utbetalinger`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { mockBehandlingRepository.finnByFagsakAndAvsluttet(any()) } returns listOf(behandling)
        every { mockTilkjentYtelseRepository.finnByBehandlingAndHasUtbetalingsoppdrag(any()) } returns
            lagTilkjentYtelse(
                mockk(),
                mockk(),
            ).also {
                it.andelerTilkjentYtelse.add(
                    lagAndelTilkjentYtelse(behandling = behandling, sats = 5000),
                )
            }

        val tilkjentYtelseIverksattMotØkonomi = beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(1)

        assertEquals(1, tilkjentYtelseIverksattMotØkonomi.size)
    }

    @Test
    fun `hentTilkjentYtelseForBehandlingerIverksattMotØkonomi - skal returnere tom liste hvis det ikke finnes noen tilkjente ytelser som inneholder andeler med utbetalinger`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { mockBehandlingRepository.finnByFagsakAndAvsluttet(any()) } returns listOf(behandling)
        every { mockTilkjentYtelseRepository.finnByBehandlingAndHasUtbetalingsoppdrag(any()) } returns
            lagTilkjentYtelse(
                mockk(),
                mockk(),
            ).also {
                it.andelerTilkjentYtelse.add(
                    lagAndelTilkjentYtelse(behandling = behandling, sats = 0),
                )
            }

        val tilkjentYtelseIverksattMotØkonomi = beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(1)

        assertEquals(0, tilkjentYtelseIverksattMotØkonomi.size)
    }

    @Nested
    inner class OppdaterTilkjentYtelsePåBehandlingTest {
        @Test
        fun `skal oppdatere tilkjentytelse basert på endretutbetalings andeler uavhengig av overlapp med andeler dersom behandlingen er automatisk`() {
            // Arrange
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.LOVENDRING_2024)
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandling.id)
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val endretUtbetalingMedAndel1 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 1), emptyList())
            val endretUtbetalingMedAndel2 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 2), emptyList())

            val endretUtbetalingAndelerMedAndeler =
                listOf(
                    endretUtbetalingMedAndel1,
                    endretUtbetalingMedAndel2,
                )

            val lagretEndretUtbetalingAndelerMedAndelerSlot = slot<MutableList<EndretUtbetalingAndelMedAndelerTilkjentYtelse>>()
            val opprettetTilkjentYtelse = mockk<TilkjentYtelse>()

            every { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) } returns endretUtbetalingAndelerMedAndeler
            every { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) } just runs
            every { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, capture(lagretEndretUtbetalingAndelerMedAndelerSlot)) } returns opprettetTilkjentYtelse
            every { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse) } returns opprettetTilkjentYtelse

            // Act
            beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, personopplysningGrunnlag, vilkårsvurdering)

            // Assert
            verify(exactly = 1) { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) }
            verify(exactly = 1) { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) }
            verify(exactly = 1) { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, capture(lagretEndretUtbetalingAndelerMedAndelerSlot)) }
            verify(exactly = 1) { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse) }

            val lagretEndretUtbetalingAndelerMedAndeler = lagretEndretUtbetalingAndelerMedAndelerSlot.captured

            assertThat(lagretEndretUtbetalingAndelerMedAndeler).hasSize(2)
            assertThat(lagretEndretUtbetalingAndelerMedAndeler[0].id).isEqualTo(1)
            assertThat(lagretEndretUtbetalingAndelerMedAndeler[1].id).isEqualTo(2)
        }

        @Test
        fun `skal oppdatere tilkjentytelse basert på endretutbetalings andeler som har overlapp med andeler dersom behandlingen ikke er automatisk`() {
            // Arrange
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandling.id)
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val endretUtbetalingMedAndel1 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 1), emptyList())
            val endretUtbetalingMedAndel2 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 2), listOf(mockk()))

            val endretUtbetalingAndelerMedAndeler =
                listOf(
                    endretUtbetalingMedAndel1,
                    endretUtbetalingMedAndel2,
                )

            val lagretEndretUtbetalingAndelerMedAndelerSlot = slot<MutableList<EndretUtbetalingAndelMedAndelerTilkjentYtelse>>()
            val opprettetTilkjentYtelse = mockk<TilkjentYtelse>()

            every { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) } returns endretUtbetalingAndelerMedAndeler
            every { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) } just runs
            every { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, capture(lagretEndretUtbetalingAndelerMedAndelerSlot)) } returns opprettetTilkjentYtelse
            every { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse) } returns opprettetTilkjentYtelse

            // Act
            beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, personopplysningGrunnlag, vilkårsvurdering)

            // Assert
            verify(exactly = 1) { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) }
            verify(exactly = 1) { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) }
            verify(exactly = 1) { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, capture(lagretEndretUtbetalingAndelerMedAndelerSlot)) }
            verify(exactly = 1) { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse) }

            val lagretEndretUtbetalingAndelerMedAndeler = lagretEndretUtbetalingAndelerMedAndelerSlot.captured

            assertThat(lagretEndretUtbetalingAndelerMedAndeler).hasSize(1)
            assertThat(lagretEndretUtbetalingAndelerMedAndeler[0].id).isEqualTo(2)
        }

        @Test
        fun `skal oppdatere tilkjentytelse basert på endretutbetalings andeler uavhengig av overlapp med andeler dersom endretutbetaling legges til av SB`() {
            // Arrange
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandling.id)
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val endretUtbetalingMedAndel1 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 1), emptyList())
            val endretUtbetalingMedAndel2 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 2), listOf(mockk()))

            val endretUtbetalingAndelerMedAndeler =
                listOf(
                    endretUtbetalingMedAndel1,
                    endretUtbetalingMedAndel2,
                )

            val lagretEndretUtbetalingAndelerMedAndelerSlot = slot<MutableList<EndretUtbetalingAndelMedAndelerTilkjentYtelse>>()
            val opprettetTilkjentYtelse = mockk<TilkjentYtelse>()

            every { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) } returns endretUtbetalingAndelerMedAndeler
            every { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) } just runs
            every { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, capture(lagretEndretUtbetalingAndelerMedAndelerSlot)) } returns opprettetTilkjentYtelse
            every { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse) } returns opprettetTilkjentYtelse

            // Act
            beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, personopplysningGrunnlag, vilkårsvurdering, endretUtbetalingMedAndel1.endretUtbetalingAndel)

            // Assert
            verify(exactly = 1) { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) }
            verify(exactly = 1) { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) }
            verify(exactly = 1) { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, capture(lagretEndretUtbetalingAndelerMedAndelerSlot)) }
            verify(exactly = 1) { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse) }

            val lagretEndretUtbetalingAndelerMedAndeler = lagretEndretUtbetalingAndelerMedAndelerSlot.captured

            assertThat(lagretEndretUtbetalingAndelerMedAndeler).hasSize(2)
            assertThat(lagretEndretUtbetalingAndelerMedAndeler[0].id).isEqualTo(1)
            assertThat(lagretEndretUtbetalingAndelerMedAndeler[1].id).isEqualTo(2)
        }
    }

    @Nested
    inner class OppdaterTilkjentYtelsePåBehandlingFraVilkårsvurdering {
        @Test
        fun `Skal først opprette tilkjent ytelse en gang uten hensyn til endret utbetalingsandeler før den oppretter tilkjent ytelse en gang til`() {
            // Arrange
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandling.id)
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val endretUtbetalingMedAndel1 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 1), emptyList())
            val endretUtbetalingMedAndel2 = EndretUtbetalingAndelMedAndelerTilkjentYtelse(lagEndretUtbetalingAndel(id = 2), listOf(mockk()))

            val endretUtbetalingAndelerMedAndeler =
                listOf(
                    endretUtbetalingMedAndel1,
                    endretUtbetalingMedAndel2,
                )

            val lagretEndretUtbetalingAndelerMedAndelerSlot = slot<MutableList<EndretUtbetalingAndelMedAndelerTilkjentYtelse>>()
            val opprettetTilkjentYtelse1 = lagInitieltTilkjentYtelse(behandling)
            val opprettetTilkjentYtelse2 = lagInitieltTilkjentYtelse(behandling)

            every { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) } returns endretUtbetalingAndelerMedAndeler
            every { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) } just runs
            every { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, emptyList()) } returns opprettetTilkjentYtelse1
            every { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse1) } returns opprettetTilkjentYtelse1

            every { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, capture(lagretEndretUtbetalingAndelerMedAndelerSlot)) } returns opprettetTilkjentYtelse2
            every { mockTilkjentYtelseRepository.save(opprettetTilkjentYtelse2) } returns opprettetTilkjentYtelse2

            // Act
            beregningService.oppdaterTilkjentYtelsePåBehandlingFraVilkårsvurdering(behandling, personopplysningGrunnlag, vilkårsvurdering)

            // Assert
            verify(exactly = 1) { mockAndelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) }
            verify(exactly = 1) { mockTilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling) }
            verify(exactly = 2) { mockTilkjentYtelseService.beregnTilkjentYtelse(vilkårsvurdering, personopplysningGrunnlag, any()) }
            verify(exactly = 2) { mockTilkjentYtelseRepository.saveAndFlush(opprettetTilkjentYtelse1) }
        }
    }
}
