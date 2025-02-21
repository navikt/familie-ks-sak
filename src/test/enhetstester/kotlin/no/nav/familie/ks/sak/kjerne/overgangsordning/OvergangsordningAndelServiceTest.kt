package no.nav.familie.ks.sak.kjerne.overgangsordning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class OvergangsordningAndelServiceTest {
    private val overgangsordningAndelRepository = mockk<OvergangsordningAndelRepository>()
    private val beregningService = mockk<BeregningService>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()

    private val overgangsordningAndelService =
        OvergangsordningAndelService(
            overgangsordningAndelRepository = overgangsordningAndelRepository,
            beregningService = beregningService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
        )

    private val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barnasIdenter = listOf(randomFnr(fødselsdato = LocalDate.of(2023, 2, 15))))
    private val vilkårsvurdering = lagVilkårsvurderingOppfylt(personer = personopplysningGrunnlag.personer)
    private val barn = personopplysningGrunnlag.barna.first()
    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.OVERGANGSORDNING_2024)
    private val gammelOvergangsordningAndel =
        OvergangsordningAndel(
            id = 0,
            behandlingId = behandling.id,
            person = barn,
            antallTimer = BigDecimal.ZERO,
            deltBosted = false,
            fom = barn.fødselsdato.plusMonths(20).toYearMonth(),
            tom = barn.fødselsdato.plusMonths(23).toYearMonth(),
        )

    @BeforeEach
    fun setup() {
        every { overgangsordningAndelRepository.save(any()) } answers { OvergangsordningAndel(behandlingId = firstArg<OvergangsordningAndel>().behandlingId) }
        every { overgangsordningAndelRepository.saveAndFlush(any()) } answers { OvergangsordningAndel(behandlingId = firstArg<OvergangsordningAndel>().behandlingId) }
        every { overgangsordningAndelRepository.saveAllAndFlush(any<Collection<OvergangsordningAndel>>()) } answers { firstArg<List<OvergangsordningAndel>>().map { OvergangsordningAndel(behandlingId = it.behandlingId) } }
        every { overgangsordningAndelRepository.deleteAll(any()) } just runs
        every { overgangsordningAndelRepository.deleteById(any()) } just runs
        every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns emptyList()
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns vilkårsvurdering
        every { beregningService.oppdaterTilkjentYtelsePåBehandling(any(), any(), any()) } just runs
    }

    @Test
    fun `opprettTomOvergangsordningAndel - skal opprette en tom OvergangsordningAndel med bare behandlingId satt`() {
        val tomOvergangsordningAndel = overgangsordningAndelService.opprettTomOvergangsordningAndel(behandling)

        assertThat(tomOvergangsordningAndel.behandlingId).isEqualTo(behandling.id)
        assertThat(tomOvergangsordningAndel.antallTimer).isEqualTo(BigDecimal.ZERO)
        assertThat(tomOvergangsordningAndel.deltBosted).isFalse()
        assertThat(tomOvergangsordningAndel.person).isNull()
        assertThat(tomOvergangsordningAndel.tom).isNull()
        assertThat(tomOvergangsordningAndel.fom).isNull()

        verify(exactly = 1) { overgangsordningAndelRepository.save(any()) }
    }

    @Nested
    inner class OppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse {
        @Test
        fun `skal oppdatere en OvergangsordningAndel og oppdatere tilkjent ytelse`() {
            val overgangsordningAndelDto = gammelOvergangsordningAndel.tilOvergangsordningAndelDto()

            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns gammelOvergangsordningAndel
            every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns listOf(gammelOvergangsordningAndel)

            val personResultat = vilkårsvurdering.personResultater.first { it.aktør == barn.aktør }
            personResultat.vilkårResultater.removeIf { it.vilkårType == Vilkår.BARNEHAGEPLASS }
            personResultat.vilkårResultater.add(
                VilkårResultat(
                    behandlingId = behandling.id,
                    personResultat = personResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "Har ikke barnehageplass",
                    periodeFom = barn.fødselsdato.plusMonths(13),
                    periodeTom = null,
                ),
            )

            overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 0, overgangsordningAndelDto)

            verify(exactly = 1) {
                overgangsordningAndelRepository.deleteAll(any())
                overgangsordningAndelRepository.saveAllAndFlush(listOf(gammelOvergangsordningAndel.fraOvergangsordningAndelDto(overgangsordningAndelDto, barn)))
            }
        }

        @Test
        fun `skal kaste feil hvis fom-dato på andel er tidligere enn måneden barnet er 20 måneder`() {
            val overgangsordningAndelDto =
                gammelOvergangsordningAndel
                    .copy(fom = barn.fødselsdato.plusMonths(19).toYearMonth())
                    .tilOvergangsordningAndelDto()

            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns gammelOvergangsordningAndel
            every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns listOf(gammelOvergangsordningAndel)

            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 0, overgangsordningAndelDto)
                }

            assertThat(funksjonellFeil.melding).isEqualTo("F.o.m. dato (sep. 2024) kan ikke være tidligere enn barnet fyller 20 måneder (okt. 2024)")
            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("F.o.m. dato (sep. 2024) kan ikke være tidligere enn barnet fyller 20 måneder (okt. 2024)")
        }

        @Test
        fun `skal kaste feil hvis tom-dato på andel er senere enn måneden barnet er 23 måneder`() {
            val overgangsordningAndelDto =
                gammelOvergangsordningAndel
                    .copy(tom = barn.fødselsdato.plusMonths(24).toYearMonth())
                    .tilOvergangsordningAndelDto()

            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns gammelOvergangsordningAndel
            every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns listOf(gammelOvergangsordningAndel)

            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 0, overgangsordningAndelDto)
                }

            assertThat(funksjonellFeil.melding).isEqualTo("T.o.m. dato (feb. 2025) kan ikke være senere enn barnet fyller 23 måneder (jan. 2025)")
            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("T.o.m. dato (feb. 2025) kan ikke være senere enn barnet fyller 23 måneder (jan. 2025)")
        }

        @Test
        fun `skal kaste feil hvis ny andel overlapper med en eksisterende andel`() {
            val overgangsordningAndelDto =
                gammelOvergangsordningAndel
                    .copy(tom = barn.fødselsdato.plusMonths(23).toYearMonth())
                    .tilOvergangsordningAndelDto()

            val annenOvergangsordningAndel =
                OvergangsordningAndel(
                    id = 1,
                    behandlingId = behandling.id,
                    person = barn,
                    antallTimer = BigDecimal.ZERO,
                    deltBosted = false,
                    fom = barn.fødselsdato.plusMonths(22).toYearMonth(),
                    tom = barn.fødselsdato.plusMonths(23).toYearMonth(),
                )

            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns gammelOvergangsordningAndel
            every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns listOf(gammelOvergangsordningAndel, annenOvergangsordningAndel)

            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 0, overgangsordningAndelDto)
                }

            assertThat(funksjonellFeil.melding).isEqualTo("Perioden som blir forsøkt lagt til overlapper med eksisterende periode for overgangsordning på person.")
            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("Perioden du forsøker å legge til overlapper med eksisterende periode for overgangsordning på personen.")
        }

        @Test
        fun `skal kaste feil hvis barnehagevilkåret ikke er oppfylt i én periode`() {
            val overgangsordningAndelDto = gammelOvergangsordningAndel.tilOvergangsordningAndelDto()

            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns gammelOvergangsordningAndel
            every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns listOf(gammelOvergangsordningAndel)

            val personResultat = vilkårsvurdering.personResultater.first { it.aktør == barn.aktør }
            personResultat.vilkårResultater.removeIf { it.vilkårType == Vilkår.BARNEHAGEPLASS }
            personResultat.vilkårResultater.addAll(
                setOf(
                    VilkårResultat(
                        behandlingId = behandling.id,
                        personResultat = personResultat,
                        vilkårType = Vilkår.BARNEHAGEPLASS,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "Har ikke barnehageplass",
                        periodeFom = barn.fødselsdato,
                        periodeTom = barn.fødselsdato.plusMonths(22).minusDays(1),
                    ),
                    VilkårResultat(
                        behandlingId = behandling.id,
                        personResultat = personResultat,
                        vilkårType = Vilkår.BARNEHAGEPLASS,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "Har barnehageplass",
                        periodeFom = barn.fødselsdato.plusMonths(22),
                        periodeTom = barn.fødselsdato.plusMonths(23),
                    ),
                ),
            )

            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 0, overgangsordningAndelDto)
                }

            assertThat(funksjonellFeil.melding).isEqualTo("Barnehageplassvilkåret er ikke oppfylt i perioden som blir forsøkt lagt til.")
            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("Barnehageplassvilkåret må være oppfylt alle periodene det er overgangsordning for barn født ${barn.fødselsdato.tilKortString()}. Vilkåret er ikke oppfylt i perioden 15. des. 24 til 31. jan. 25.")
        }

        @Test
        fun `skal kaste feil hvis barnehagevilkåret ikke er oppfylt i flere perioder`() {
            val overgangsordningAndelDto = gammelOvergangsordningAndel.tilOvergangsordningAndelDto()

            every { overgangsordningAndelRepository.finnOvergangsordningAndel(any()) } returns gammelOvergangsordningAndel
            every { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(any()) } returns listOf(gammelOvergangsordningAndel)

            val personResultat = vilkårsvurdering.personResultater.first { it.aktør == barn.aktør }
            personResultat.vilkårResultater.removeIf { it.vilkårType == Vilkår.BARNEHAGEPLASS }
            personResultat.vilkårResultater.addAll(
                setOf(
                    VilkårResultat(
                        behandlingId = behandling.id,
                        personResultat = personResultat,
                        vilkårType = Vilkår.BARNEHAGEPLASS,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "Har barnehageplass",
                        periodeFom = barn.fødselsdato,
                        periodeTom = barn.fødselsdato.plusMonths(21).minusDays(1),
                    ),
                    VilkårResultat(
                        behandlingId = behandling.id,
                        personResultat = personResultat,
                        vilkårType = Vilkår.BARNEHAGEPLASS,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "Har ikke barnehageplass",
                        periodeFom = barn.fødselsdato.plusMonths(21),
                        periodeTom = barn.fødselsdato.plusMonths(22).minusDays(1),
                    ),
                    VilkårResultat(
                        behandlingId = behandling.id,
                        personResultat = personResultat,
                        vilkårType = Vilkår.BARNEHAGEPLASS,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "Har barnehageplass",
                        periodeFom = barn.fødselsdato.plusMonths(22),
                        periodeTom = barn.fødselsdato.plusMonths(23),
                    ),
                ),
            )

            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(behandling, 0, overgangsordningAndelDto)
                }

            assertThat(funksjonellFeil.melding).isEqualTo("Barnehageplassvilkåret er ikke oppfylt i perioden som blir forsøkt lagt til.")
            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("Barnehageplassvilkåret må være oppfylt alle periodene det er overgangsordning for barn født ${barn.fødselsdato.tilKortString()}. Vilkåret er ikke oppfylt i periodene 1. okt. 24 til 14. nov. 24 og 15. des. 24 til 31. jan. 25.")
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
            assertThat(it.antallTimer).isEqualTo(BigDecimal.ZERO)
            assertThat(it.deltBosted).isFalse()
            assertThat(it.person).isNull()
            assertThat(it.tom).isNull()
            assertThat(it.fom).isNull()
        }

        verify(exactly = 1) { overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(gammelBehandling.id) }
        verify(exactly = 3) { overgangsordningAndelRepository.save(any()) }
    }
}
