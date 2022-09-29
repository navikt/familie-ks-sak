package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BeregningServiceTest {

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @InjectMockKs
    private lateinit var beregningService: BeregningService

    @Test
    fun `finnBarnFraBehandlingMedTilkjentYtelse skal returnere når tom liste når det ikke finnes en andel tilkjent ytelse`() {
        val behandlngId = 111L
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlngId) } returns emptyList()
        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlngId) } returns
            lagPersonopplysningGrunnlag(behandlngId, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))

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

        assertTrue { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id).isNotEmpty() }
    }
}
