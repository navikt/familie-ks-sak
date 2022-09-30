package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class PersonopplysningGrunnlagServiceTest {

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var personService: PersonService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var personidentService: PersonidentService

    @InjectMockKs
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @BeforeEach
    fun init() {
        every { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) } just runs
    }

    @Test
    fun `opprettPersonopplysningGrunnlag skal opprette personopplysninggrunnlag for FGB`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns null

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            søkerAktør = behandling.fagsak.aktør
        )
        every { personopplysningGrunnlagRepository.save(any()) } returns personopplysningGrunnlag
        every { personService.lagPerson(any(), any(), any(), any(), any()) } returns personopplysningGrunnlag.søker

        assertDoesNotThrow { personopplysningGrunnlagService.opprettPersonopplysningGrunnlag(behandling, null) }

        verify(atMost = 0, atLeast = 0) { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(any()) }
        verify(atMost = 1) { personService.lagPerson(any(), any(), any(), any(), any()) }
        verify(atMost = 2) { personopplysningGrunnlagRepository.save(any()) }
        verify(atMost = 1) { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) }

        assertEquals(1, personopplysningGrunnlag.personer.size)
    }

    @Test
    fun `opprettPersonopplysningGrunnlag skal opprette personopplysninggrunnlag for revurdering`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val revurdering = lagBehandling(
            fagsak = behandling.fagsak,
            type = BehandlingType.REVURDERING,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD
        )
        val barnAktør = randomAktør()

        every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) } returns listOf(barnAktør)
        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = revurdering.id,
            søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            søkerAktør = revurdering.fagsak.aktør,
            barnAktør = listOf(barnAktør),
            barnasIdenter = listOf(barnAktør.aktivFødselsnummer())
        )
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag
        every { personopplysningGrunnlagRepository.save(any()) } returns personopplysningGrunnlag
        every { personopplysningGrunnlagRepository.saveAndFlush(any()) } returns personopplysningGrunnlag
        every {
            personService.lagPerson(
                aktør = revurdering.fagsak.aktør,
                personopplysningGrunnlag = personopplysningGrunnlag,
                målform = Målform.NB,
                personType = PersonType.SØKER,
                krevesEnkelPersonInfo = false
            )
        } returns personopplysningGrunnlag.søker
        every {
            personService.lagPerson(
                aktør = barnAktør,
                personopplysningGrunnlag = personopplysningGrunnlag,
                målform = Målform.NB,
                personType = PersonType.BARN,
                krevesEnkelPersonInfo = false
            )
        } returns personopplysningGrunnlag.barna.single()

        assertDoesNotThrow { personopplysningGrunnlagService.opprettPersonopplysningGrunnlag(revurdering, behandling) }

        verify(atMost = 1, atLeast = 1) { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(any()) }
        verify(atMost = 2) { personService.lagPerson(any(), any(), any(), any(), any()) }
        verify(atMost = 2) { personopplysningGrunnlagRepository.save(any()) }
        verify(atMost = 1) { personopplysningGrunnlagRepository.saveAndFlush(any()) }
        verify(atMost = 1) { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) }

        assertEquals(2, personopplysningGrunnlag.personer.size)
    }
}
