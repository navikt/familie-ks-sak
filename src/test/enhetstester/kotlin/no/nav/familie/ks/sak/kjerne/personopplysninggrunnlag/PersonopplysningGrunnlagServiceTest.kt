package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
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
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class PersonopplysningGrunnlagServiceTest {
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val beregningService = mockk<BeregningService>()
    private val personService = mockk<PersonService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val personidentService = mockk<PersonidentService>()
    private val loggService = mockk<LoggService>()

    private val personopplysningGrunnlagService =
        PersonopplysningGrunnlagService(
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            beregningService = beregningService,
            personService = personService,
            arbeidsfordelingService = arbeidsfordelingService,
            personidentService = personidentService,
            loggService = loggService,
        )

    @BeforeEach
    fun init() {
        every { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) } just runs
    }

    @Test
    fun `opprettPersonopplysningGrunnlag skal opprette personopplysninggrunnlag for FGB`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns null

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
            )
        every { personopplysningGrunnlagRepository.save(any()) } returns personopplysningGrunnlag
        every { personService.lagPerson(any(), any(), any(), any(), any()) } returns personopplysningGrunnlag.søker

        assertDoesNotThrow { personopplysningGrunnlagService.opprettPersonopplysningGrunnlag(behandling, null) }

        verify(atMost = 0, atLeast = 0) { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(any()) }
        verify(atMost = 1) { personService.lagPerson(any(), any(), any(), any(), any()) }
        verify(atMost = 2) { personopplysningGrunnlagRepository.save(any()) }
        verify(atMost = 1) { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) }

        assertEquals(1, personopplysningGrunnlag.personer.size)
    }

    @Test
    fun `opprettPersonopplysningGrunnlag skal opprette personopplysninggrunnlag for revurdering`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val revurdering =
            lagBehandling(
                fagsak = behandling.fagsak,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD,
            )
        val barnAktør = randomAktør()

        every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) } returns listOf(barnAktør)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = revurdering.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = revurdering.fagsak.aktør,
                barnAktør = listOf(barnAktør),
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
            )
        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag
        every { personopplysningGrunnlagRepository.save(any()) } returns personopplysningGrunnlag
        every { personopplysningGrunnlagRepository.saveAndFlush(any()) } returns personopplysningGrunnlag
        every {
            personService.lagPerson(
                aktør = revurdering.fagsak.aktør,
                personopplysningGrunnlag = personopplysningGrunnlag,
                målform = Målform.NB,
                personType = PersonType.SØKER,
                krevesEnkelPersonInfo = false,
            )
        } returns personopplysningGrunnlag.søker
        every {
            personService.lagPerson(
                aktør = barnAktør,
                personopplysningGrunnlag = personopplysningGrunnlag,
                målform = Målform.NB,
                personType = PersonType.BARN,
                krevesEnkelPersonInfo = false,
            )
        } returns personopplysningGrunnlag.barna.single()

        assertDoesNotThrow { personopplysningGrunnlagService.opprettPersonopplysningGrunnlag(revurdering, behandling) }

        verify(atMost = 1, atLeast = 1) { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(any()) }
        verify(atMost = 2) { personService.lagPerson(any(), any(), any(), any(), any()) }
        verify(atMost = 2) { personopplysningGrunnlagRepository.save(any()) }
        verify(atMost = 1) { personopplysningGrunnlagRepository.saveAndFlush(any()) }
        verify(atMost = 1) { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) }

        assertEquals(2, personopplysningGrunnlag.personer.size)
    }

    @Test
    fun `oppdaterPersonopplysningGrunnlag - skal i førstegangsbehandlinger deaktivere eksisterende aktivt personopplysningsgrunnlag og opprette et nytt med barn fra innsendt søknad`() {
        val deaktivertPersonopplysningGrunnlagSlot = slot<PersonopplysningGrunnlag>()
        val søker = randomAktør()
        val barn1 = randomAktør()
        val barn2 = randomAktør()
        val barn3 = randomAktør()
        val fagsak = lagFagsak(søker)
        val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val eksisterendePersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktivFødselsnummer()),
            )

        val nyttPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
            ).also { it.personer.clear() }

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns eksisterendePersonopplysningGrunnlag
        every { personidentService.hentOgLagreAktør(any(), any()) } returns barn2 andThen barn3
        every { personService.lagPerson(any(), any(), any(), any(), any()) } returns
            lagPerson(
                personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
                aktør = søker,
            ) andThen
            lagPerson(
                personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
                aktør = barn2,
                personType = PersonType.BARN,
            ) andThen
            lagPerson(
                personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
                aktør = barn3,
                personType = PersonType.BARN,
            )
        every { personopplysningGrunnlagRepository.saveAndFlush(capture(deaktivertPersonopplysningGrunnlagSlot)) } returnsArgument 0
        every { personopplysningGrunnlagRepository.save(any()) } returnsArgument 0
        val lagretPersonopplysningsgrunnlag =
            personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = null,
                søknadDto =
                    SøknadDto(
                        søkerMedOpplysninger = SøkerMedOpplysningerDto(søker.aktivFødselsnummer()),
                        barnaMedOpplysninger =
                            listOf(
                                BarnMedOpplysningerDto(barn2.aktivFødselsnummer()),
                                BarnMedOpplysningerDto(barn3.aktivFødselsnummer()),
                            ),
                        endringAvOpplysningerBegrunnelse = "",
                    ),
            )

        assertThat(deaktivertPersonopplysningGrunnlagSlot.captured.aktiv, Is(false))

        assertThat(lagretPersonopplysningsgrunnlag.aktiv, Is(true))
        assertThat(lagretPersonopplysningsgrunnlag.personer.size, Is(3))
        assertThat(lagretPersonopplysningsgrunnlag.barna.size, Is(2))
        assertThat(
            lagretPersonopplysningsgrunnlag.barna.map { it.aktør.aktivFødselsnummer() },
            containsInAnyOrder(barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer()),
        )
    }

    @Test
    fun `oppdaterPersonopplysningGrunnlag - skal i revurdering deaktivere eksisterende aktivt personopplysningsgrunnlag og opprette et nytt med barn fra søknad og tidligere vedtatt behandling`() {
        val deaktivertPersonopplysningGrunnlagSlot = slot<PersonopplysningGrunnlag>()
        val søker = randomAktør()
        val barn1 = randomAktør()
        val barn2 = randomAktør()
        val barn3 = randomAktør()
        val fagsak = lagFagsak(søker)
        val vedtattBehandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val eksisterendePersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = vedtattBehandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktivFødselsnummer()),
            )

        val nyttPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
            ).also { it.personer.clear() }

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns eksisterendePersonopplysningGrunnlag
        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                any(),
                any(),
            )
        } returns
            listOf(
                mockk(),
            )
        every { personidentService.hentOgLagreAktør(any(), any()) } returns barn2 andThen barn3
        every { personService.lagPerson(any(), any(), any(), any(), any()) } returns
            lagPerson(
                personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
                aktør = søker,
            ) andThen
            lagPerson(
                personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
                aktør = barn1,
                personType = PersonType.BARN,
            ) andThen
            lagPerson(
                personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
                aktør = barn2,
                personType = PersonType.BARN,
            ) andThen
            lagPerson(
                personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
                aktør = barn3,
                personType = PersonType.BARN,
            )
        every { personopplysningGrunnlagRepository.saveAndFlush(capture(deaktivertPersonopplysningGrunnlagSlot)) } returnsArgument 0
        every { personopplysningGrunnlagRepository.save(any()) } returnsArgument 0
        val lagretPersonopplysningGrunnlag =
            personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = vedtattBehandling,
                søknadDto =
                    SøknadDto(
                        søkerMedOpplysninger = SøkerMedOpplysningerDto(søker.aktivFødselsnummer()),
                        barnaMedOpplysninger =
                            listOf(
                                BarnMedOpplysningerDto(barn2.aktivFødselsnummer()),
                                BarnMedOpplysningerDto(barn3.aktivFødselsnummer()),
                            ),
                        endringAvOpplysningerBegrunnelse = "",
                    ),
            )

        assertThat(deaktivertPersonopplysningGrunnlagSlot.captured.aktiv, Is(false))
        assertThat(lagretPersonopplysningGrunnlag.aktiv, Is(true))
        assertThat(lagretPersonopplysningGrunnlag.personer.size, Is(4))
        assertThat(lagretPersonopplysningGrunnlag.barna.size, Is(3))
        assertThat(
            lagretPersonopplysningGrunnlag.barna.map { it.aktør.aktivFødselsnummer() },
            containsInAnyOrder(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer()),
        )
    }

    @Test
    fun `oppdaterPersonopplysningGrunnlag - skal kaste feil dersom det ikke eksisterer noe aktivt persongrunnlag for behandling`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns null

        val feil =
            assertThrows<Feil> {
                personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(
                    behandling,
                    null,
                    SøknadDto(
                        søkerMedOpplysninger = SøkerMedOpplysningerDto(""),
                        barnaMedOpplysninger = emptyList(),
                        endringAvOpplysningerBegrunnelse = "",
                    ),
                )
            }
        assertEquals("Det finnes ikke noe aktivt personopplysningsgrunnlag for ${behandling.id}", feil.message)
    }

    @Test
    fun `leggTilBarnIPersonopplysningGrunnlagOgOpprettLogg skal ikke legge til barn når det allerede finnes`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val barn1 = randomAktør()

        every { personidentService.hentOgLagreAktør(any(), any()) } returns barn1
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                barnasIdenter = listOf(barn1.aktivFødselsnummer()),
                barnAktør = listOf(barn1),
            )

        val exception =
            assertThrows<FunksjonellFeil> {
                personopplysningGrunnlagService.leggTilBarnIPersonopplysningGrunnlagOgOpprettLogg(behandling, barn1.aktivFødselsnummer())
            }
        assertEquals("Forsøker å legge til barn som allerede finnes i personopplysningsgrunnlag id=0", exception.message)
        assertEquals("Barn finnes allerede på behandling og er derfor ikke lagt til.", exception.frontendFeilmelding)
    }

    @Test
    fun `leggTilBarnIPersonopplysningGrunnlagOgOpprettLogg skal legge til barn pg opprette historikkinnslag`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val barn1 = randomAktør()
        val søker = randomAktør()
        val nyBarn = randomAktør()
        val eksisterendePersonOpplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
                søkerAktør = søker,
                barnasIdenter = listOf(barn1.aktivFødselsnummer()),
                barnAktør = listOf(barn1),
            )
        val nyPersonopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

        every { personidentService.hentOgLagreAktør(any(), any()) } returns nyBarn
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns eksisterendePersonOpplysningGrunnlag
        every { personService.lagPerson(any(), any(), any(), any(), any()) } returns
            lagPerson(personopplysningGrunnlag = nyPersonopplysningGrunnlag, aktør = søker, personType = PersonType.SØKER) andThen
            lagPerson(personopplysningGrunnlag = nyPersonopplysningGrunnlag, aktør = barn1, personType = PersonType.BARN) andThen
            lagPerson(personopplysningGrunnlag = nyPersonopplysningGrunnlag, aktør = nyBarn, personType = PersonType.BARN)
        every { loggService.opprettBarnLagtTilLogg(any(), any()) } just runs
        every { personopplysningGrunnlagRepository.saveAndFlush(any()) } returns eksisterendePersonOpplysningGrunnlag
        every { personopplysningGrunnlagRepository.save(any()) } returns nyPersonopplysningGrunnlag

        assertDoesNotThrow {
            personopplysningGrunnlagService.leggTilBarnIPersonopplysningGrunnlagOgOpprettLogg(behandling, barn1.aktivFødselsnummer())
        }
        verify(exactly = 1) { personidentService.hentOgLagreAktør(any(), any()) }
        verify(exactly = 2) { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) }
        verify(exactly = 2) { personopplysningGrunnlagRepository.save(any()) } // opprett ny og så oppdaterer det med barn
        verify(exactly = 1) { personopplysningGrunnlagRepository.saveAndFlush(any()) } // deaktiverer eksisterende
        verify(exactly = 1) { arbeidsfordelingService.fastsettBehandlendeEnhet(behandling) }
        verify(exactly = 3) { personService.lagPerson(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { loggService.opprettBarnLagtTilLogg(any(), any()) }
    }

    @Nested
    inner class HentBarnaThrows {
        @Test
        fun `skal hente barna`() {
            // Arrange
            val dagensDato = LocalDate.now()
            val behandling = lagBehandling()

            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandlingId = behandling.id)

            val søker =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    id = 0L,
                    type = PersonType.SØKER,
                    fødselsdato = dagensDato.minusYears(35),
                )

            val barn1 =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    id = 1L,
                    type = PersonType.BARN,
                    fødselsdato = dagensDato.minusYears(1),
                )

            val barn2 =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    id = 2L,
                    type = PersonType.BARN,
                    fødselsdato = dagensDato.minusYears(5),
                )

            personopplysningGrunnlag.personer.addAll(setOf(søker, barn1, barn2))

            every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag

            // Act
            val barna = personopplysningGrunnlagService.hentBarnaThrows(behandling.id)

            // Assert
            assertThat(barna).hasSize(2)
            assertThat(barna).anySatisfy { assertThat(it).isEqualTo(barn1) }
            assertThat(barna).anySatisfy { assertThat(it).isEqualTo(barn2) }
        }

        @Test
        fun `skal kaste exception hvis ingen personopplysningsgrunnlag finnes`() {
            // Arrange
            val behandling = lagBehandling()

            every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id) } throws Feil("Ops! Feil oppstod.")

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    personopplysningGrunnlagService.hentBarnaThrows(behandling.id)
                }
            assertThat(exception.message).isEqualTo("Ops! Feil oppstod.")
        }
    }
}
