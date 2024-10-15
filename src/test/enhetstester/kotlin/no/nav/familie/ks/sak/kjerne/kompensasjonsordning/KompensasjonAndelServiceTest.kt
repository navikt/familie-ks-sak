package no.nav.familie.ks.sak.kjerne.kompensasjonsordning

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.KompensasjonAndelDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndel
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndelRepository
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.fraKompenasjonAndelDto
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
class KompensasjonAndelServiceTest {
    @MockK
    private lateinit var kompensasjonAndelRepository: KompensasjonAndelRepository

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @InjectMockKs
    private lateinit var kompensasjonAndelService: KompensasjonAndelService

    private val søker = lagPerson(aktør = randomAktør())

    @BeforeEach
    fun setup() {
        every { kompensasjonAndelRepository.save(any()) } answers { KompensasjonAndel(behandlingId = firstArg<KompensasjonAndel>().behandlingId) }
        every { kompensasjonAndelRepository.saveAndFlush(any()) } answers { KompensasjonAndel(behandlingId = firstArg<KompensasjonAndel>().behandlingId) }
        every { kompensasjonAndelRepository.saveAllAndFlush(any<Collection<KompensasjonAndel>>()) } answers { firstArg<List<KompensasjonAndel>>().map { KompensasjonAndel(behandlingId = it.behandlingId) } }
        every { kompensasjonAndelRepository.deleteAll(any()) } just runs
        every { kompensasjonAndelRepository.deleteById(any()) } just runs
        every { kompensasjonAndelRepository.hentKompensasjonAndelerForBehandling(any()) } returns emptyList()
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns lagPersonopplysningGrunnlag(søkerAktør = søker.aktør)
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns mockk()
        every { beregningService.oppdaterTilkjentYtelsePåBehandling(any(), any(), any()) } just runs
    }

    @Test
    fun `opprettTomKompensasjonAndel - skal opprette en tom KompensasjonAndel med bare behandlingId satt`() {
        val behandling = lagBehandling()

        val tomKompensasjonAndel = kompensasjonAndelService.opprettTomKompensasjonAndel(behandling)

        assertThat(tomKompensasjonAndel.behandlingId).isEqualTo(behandling.id)
        assertThat(tomKompensasjonAndel.person).isNull()
        assertThat(tomKompensasjonAndel.prosent).isNull()
        assertThat(tomKompensasjonAndel.tom).isNull()
        assertThat(tomKompensasjonAndel.fom).isNull()

        verify(exactly = 1) { kompensasjonAndelRepository.save(any()) }
    }

    @Nested
    inner class OppdaterKompensasjonAndelOgOppdaterTilkjentYtelse {
        @Test
        fun `skal oppdatere en KompensasjonAndel og oppdatere tilkjent ytelse`() {
            val behandling = lagBehandling()
            val gammelKompensasjonAndel =
                KompensasjonAndel(
                    id = 0,
                    behandlingId = behandling.id,
                    person = søker,
                    prosent = BigDecimal(100),
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                )

            val kompensasjonAndelDto =
                KompensasjonAndelDto(
                    id = 1,
                    personIdent = søker.aktør.aktivFødselsnummer(),
                    prosent = BigDecimal(100),
                    fom = YearMonth.of(2024, 2),
                    tom = YearMonth.of(2024, 4),
                )

            every { kompensasjonAndelRepository.finnKompensasjonAndel(any()) } returns gammelKompensasjonAndel
            every { kompensasjonAndelRepository.hentKompensasjonAndelerForBehandling(any()) } returns listOf(gammelKompensasjonAndel)

            kompensasjonAndelService.oppdaterKompensasjonAndelOgOppdaterTilkjentYtelse(behandling, 0, kompensasjonAndelDto)

            verify(exactly = 1) {
                kompensasjonAndelRepository.deleteAll(any())
                kompensasjonAndelRepository.saveAllAndFlush(listOf(gammelKompensasjonAndel.fraKompenasjonAndelDto(kompensasjonAndelDto, søker)))
            }
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis kompensasjonsandel ikke finnes`() {
            every { kompensasjonAndelRepository.finnKompensasjonAndel(any()) } returns null

            val exception =
                assertThrows<FunksjonellFeil> {
                    kompensasjonAndelService.oppdaterKompensasjonAndelOgOppdaterTilkjentYtelse(mockk(), 0, mockk())
                }

            assertThat(exception.melding).isEqualTo("Fant ikke kompensasjonsandel med id 0")
        }
    }

    @Test
    fun `fjernKompensasjonAndelOgOppdaterTilkjentYtelse - skal slette kompensasjonandeler og oppdatere tilkjent ytelse`() {
        val behandling = lagBehandling()

        every { kompensasjonAndelRepository.hentKompensasjonAndelerForBehandling(any()) } returns emptyList()

        kompensasjonAndelService.fjernKompensasjonAndelOgOppdaterTilkjentYtelse(behandling, 200)

        verify(exactly = 1) { kompensasjonAndelRepository.deleteById(200) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, any(), any()) }
    }

    @Test
    fun `kopierKompensasjonAndelFraForrigeBehandling - skal kopiere over kompensasjonandeler fra forrige behandling og lagre disse på ny`() {
        val gammelBehandling = lagBehandling()
        val nyBehandling = lagBehandling()

        every { kompensasjonAndelRepository.hentKompensasjonAndelerForBehandling(gammelBehandling.id) } returns
            listOf(
                KompensasjonAndel(id = 0, behandlingId = gammelBehandling.id),
                KompensasjonAndel(id = 1, behandlingId = gammelBehandling.id),
                KompensasjonAndel(id = 2, behandlingId = gammelBehandling.id),
            )

        val nyeKompensasjonAndeler = kompensasjonAndelService.kopierKompensasjonAndelFraForrigeBehandling(nyBehandling, gammelBehandling)

        assertThat(nyeKompensasjonAndeler).allSatisfy {
            assertThat(it.behandlingId).isEqualTo(nyBehandling.id)
            assertThat(it.person).isNull()
            assertThat(it.prosent).isNull()
            assertThat(it.tom).isNull()
            assertThat(it.fom).isNull()
        }

        verify(exactly = 1) { kompensasjonAndelRepository.hentKompensasjonAndelerForBehandling(gammelBehandling.id) }
        verify(exactly = 3) { kompensasjonAndelRepository.save(any()) }
    }
}
