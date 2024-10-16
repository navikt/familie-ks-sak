package no.nav.familie.ks.sak.kjerne.overgangsordning

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.OvergangsordningAndelDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.fraOvergangsordningAndelDto
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class OvergangsordningAndelServiceTest {
    @MockK
    private lateinit var overgangsordningAndelRepository: OvergangsordningAndelRepository

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @InjectMockKs
    private lateinit var overgangsordningAndelService: OvergangsordningAndelService

    private val søker = lagPerson(aktør = randomAktør())

    @BeforeEach
    fun setup() {
        every { overgangsordningAndelRepository.save(any()) } answers { OvergangsordningAndel(behandlingId = firstArg<OvergangsordningAndel>().behandlingId) }
        every { overgangsordningAndelRepository.saveAndFlush(any()) } answers { OvergangsordningAndel(behandlingId = firstArg<OvergangsordningAndel>().behandlingId) }
        every { overgangsordningAndelRepository.saveAllAndFlush(any<Collection<OvergangsordningAndel>>()) } answers { firstArg<List<OvergangsordningAndel>>().map { OvergangsordningAndel(behandlingId = it.behandlingId) } }
        every { overgangsordningAndelRepository.deleteAll(any()) } just runs
        every { overgangsordningAndelRepository.deleteById(any()) } just runs
        every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns emptyList()
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns lagPersonopplysningGrunnlag(søkerAktør = søker.aktør)
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns mockk()
        every { beregningService.oppdaterTilkjentYtelsePåBehandling(any(), any(), any()) } just runs
    }

    @Test
    fun `opprettTomOvergangsordningAndel - skal opprette en tom OvergangsordningAndel med bare behandlingId satt`() {
        val behandling = lagBehandling()

        val tomOvergangsordningAndel = overgangsordningAndelService.opprettTomOvergangsordningAndel(behandling)

        assertThat(tomOvergangsordningAndel.behandlingId).isEqualTo(behandling.id)
        assertThat(tomOvergangsordningAndel.person).isNull()
        assertThat(tomOvergangsordningAndel.prosent).isNull()
        assertThat(tomOvergangsordningAndel.tom).isNull()
        assertThat(tomOvergangsordningAndel.fom).isNull()

        verify(exactly = 1) { overgangsordningAndelRepository.save(any()) }
    }

    @Nested
    inner class OppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse {
        @Test
        fun `skal oppdatere en OvergangsordningAndel og oppdatere tilkjent ytelse`() {
            val behandling = lagBehandling()
            val gammelOvergangsordningAndel =
                OvergangsordningAndel(
                    id = 0,
                    behandlingId = behandling.id,
                    person = søker,
                    prosent = BigDecimal(100),
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                )

            val overgangsordningAndelDto =
                OvergangsordningAndelDto(
                    id = 1,
                    personIdent = søker.aktør.aktivFødselsnummer(),
                    prosent = BigDecimal(100),
                    fom = YearMonth.of(2024, 2),
                    tom = YearMonth.of(2024, 4),
                )

            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns gammelOvergangsordningAndel
            every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns listOf(gammelOvergangsordningAndel)

            overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 0, overgangsordningAndelDto)

            verify(exactly = 1) {
                overgangsordningAndelRepository.deleteAll(any())
                overgangsordningAndelRepository.saveAllAndFlush(listOf(gammelOvergangsordningAndel.fraOvergangsordningAndelDto(overgangsordningAndelDto, søker)))
            }
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis overgangsordningandel ikke finnes`() {
            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns null

            val exception =
                assertThrows<FunksjonellFeil> {
                    overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(mockk(), 0, mockk())
                }

            assertThat(exception.melding).isEqualTo("Fant ikke overgangsordningandel med id 0")
        }
    }

    @Test
    fun `fjernOvergangsordningAndelOgOppdaterTilkjentYtelse - skal slette overgangsordningandeler og oppdatere tilkjent ytelse`() {
        val behandling = lagBehandling()

        every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns emptyList()

        overgangsordningAndelService.fjernOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 200)

        verify(exactly = 1) { overgangsordningAndelRepository.deleteById(200) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, any(), any()) }
    }

    @Test
    fun `kopierOvergangsordningAndelFraForrigeBehandling - skal kopiere over overgangsordningandeler fra forrige behandling og lagre disse på ny`() {
        val gammelBehandling = lagBehandling()
        val nyBehandling = lagBehandling()

        every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(gammelBehandling.id) } returns
            listOf(
                OvergangsordningAndel(id = 0, behandlingId = gammelBehandling.id),
                OvergangsordningAndel(id = 1, behandlingId = gammelBehandling.id),
                OvergangsordningAndel(id = 2, behandlingId = gammelBehandling.id),
            )

        val nyeOvergangsordningAndeler = overgangsordningAndelService.kopierOvergangsordningAndelFraForrigeBehandling(nyBehandling, gammelBehandling)

        assertThat(nyeOvergangsordningAndeler).allSatisfy {
            assertThat(it.behandlingId).isEqualTo(nyBehandling.id)
            assertThat(it.person).isNull()
            assertThat(it.prosent).isNull()
            assertThat(it.tom).isNull()
            assertThat(it.fom).isNull()
        }

        verify(exactly = 1) { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(gammelBehandling.id) }
        verify(exactly = 3) { overgangsordningAndelRepository.save(any()) }
    }
}
