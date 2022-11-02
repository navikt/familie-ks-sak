package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class PersonopplysningGrunnlagServiceTest {

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

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

    @Test
    fun `oppdaterPersonopplysningGrunnlag - skal deaktivere eksisterende aktivt personopplysningsgrunnlag og opprette et nytt med barn fra søknad i FGB`() {
        val lagredePersonopplysningsGrunnlag = mutableListOf<PersonopplysningGrunnlag>()
        val søker = randomAktør()
        val barn1 = randomAktør()
        val barn2 = randomAktør()
        val barn3 = randomAktør()
        val fagsak = lagFagsak(søker)
        val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val eksisterendePersonopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer())
        )

        val nyttPersonopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer()
        ).also { it.personer.clear() }

        val nyttPersonopplysningGrunnlagMedBarna = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer())
        )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns eksisterendePersonopplysningGrunnlag
        every { personidentService.hentOgLagreAktør(any(), any()) } returns barn2 andThen barn3
        every { personService.lagPerson(any(), any(), any(), any(), any()) } returns lagPerson(
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            aktør = søker
        ) andThen lagPerson(
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            aktør = barn2,
            personType = PersonType.BARN
        ) andThen lagPerson(
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            aktør = barn3,
            personType = PersonType.BARN
        )
        every { personopplysningGrunnlagRepository.saveAndFlush(capture(lagredePersonopplysningsGrunnlag)) } returns mockk()
        every { personopplysningGrunnlagRepository.save(capture(lagredePersonopplysningsGrunnlag)) } returns nyttPersonopplysningGrunnlag andThen nyttPersonopplysningGrunnlagMedBarna
        personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(
            behandling,
            null,
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(søker.aktivFødselsnummer()),
                barnaMedOpplysninger = listOf(
                    BarnMedOpplysningerDto(barn2.aktivFødselsnummer()),
                    BarnMedOpplysningerDto(barn3.aktivFødselsnummer())
                ),
                endringAvOpplysningerBegrunnelse = ""
            )
        )

        assertThat(lagredePersonopplysningsGrunnlag.first().aktiv, Is(false))
        assertThat(lagredePersonopplysningsGrunnlag[1].aktiv, Is(true))
        assertThat(lagredePersonopplysningsGrunnlag[2].personer.size, Is(3))
        assertThat(lagredePersonopplysningsGrunnlag[2].barna.size, Is(2))
        assertThat(
            lagredePersonopplysningsGrunnlag[2].barna.map { it.aktør.aktivFødselsnummer() },
            containsInAnyOrder(barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer())
        )
    }

    @Test
    fun `oppdaterPersonopplysningGrunnlag - skal deaktivere eksisterende aktivt personopplysningsgrunnlag og opprette et nytt med barn fra søknad og vedtatt behandling i revurdering`() {
        val lagredePersonopplysningsGrunnlag = mutableListOf<PersonopplysningGrunnlag>()
        val søker = randomAktør()
        val barn1 = randomAktør()
        val barn2 = randomAktør()
        val barn3 = randomAktør()
        val fagsak = lagFagsak(søker)
        val vedtattBehandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val eksisterendePersonopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = vedtattBehandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer())
        )

        val nyttPersonopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer()
        ).also { it.personer.clear() }

        val nyttPersonopplysningGrunnlagMedBarna = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer())
        )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns eksisterendePersonopplysningGrunnlag
        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                any(),
                any()
            )
        } returns listOf(
            mockk()
        )
        every { personidentService.hentOgLagreAktør(any(), any()) } returns barn2 andThen barn3
        every { personService.lagPerson(any(), any(), any(), any(), any()) } returns lagPerson(
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            aktør = søker
        ) andThen lagPerson(
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            aktør = barn1,
            personType = PersonType.BARN
        ) andThen lagPerson(
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            aktør = barn2,
            personType = PersonType.BARN
        ) andThen lagPerson(
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            aktør = barn3,
            personType = PersonType.BARN
        )
        every { personopplysningGrunnlagRepository.saveAndFlush(capture(lagredePersonopplysningsGrunnlag)) } returns mockk()
        every { personopplysningGrunnlagRepository.save(capture(lagredePersonopplysningsGrunnlag)) } returns nyttPersonopplysningGrunnlag andThen nyttPersonopplysningGrunnlagMedBarna
        personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(
            behandling,
            vedtattBehandling,
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(søker.aktivFødselsnummer()),
                barnaMedOpplysninger = listOf(
                    BarnMedOpplysningerDto(barn2.aktivFødselsnummer()),
                    BarnMedOpplysningerDto(barn3.aktivFødselsnummer())
                ),
                endringAvOpplysningerBegrunnelse = ""
            )
        )

        assertThat(lagredePersonopplysningsGrunnlag.first().aktiv, Is(false))
        assertThat(lagredePersonopplysningsGrunnlag[1].aktiv, Is(true))
        assertThat(lagredePersonopplysningsGrunnlag[2].personer.size, Is(4))
        assertThat(lagredePersonopplysningsGrunnlag[2].barna.size, Is(3))
        assertThat(
            lagredePersonopplysningsGrunnlag[2].barna.map { it.aktør.aktivFødselsnummer() },
            containsInAnyOrder(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer())
        )
    }

    @Test
    fun `oppdaterPersonopplysningGrunnlag - skal kaste feil dersom det ikke eksisterer noe aktivt persongrunnlag for behandling`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns null

        val feil = assertThrows<Feil> {
            personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(
                behandling,
                null,
                SøknadDto(
                    søkerMedOpplysninger = SøkerMedOpplysningerDto(""),
                    barnaMedOpplysninger = emptyList(),
                    endringAvOpplysningerBegrunnelse = ""
                )
            )
        }
        assertEquals("Det finnes ikke noe aktivt personopplysningsgrunnlag for ${behandling.id}", feil.message)
    }
}
