package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling.steg

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPdlPersonInfo
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.RegistrerPersonGrunnlagSteg
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired

class RegistrerPersonGrunnlagStegTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var registrerPersonGrunnlagSteg: RegistrerPersonGrunnlagSteg

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockkBean(relaxed = true)
    private lateinit var personOpplysningerService: PersonOpplysningerService

    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockkBean
    private lateinit var beregningService: BeregningService

    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        val aktør = aktørRepository.saveAndFlush(randomAktør())
        val fagsak = fagsakRepository.saveAndFlush(lagFagsak(aktør))
        behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )
        every { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns lagPdlPersonInfo()
        every { arbeidsfordelingService.fastsettBehandledeEnhet(any()) } just runs
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for FGB`() {
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(behandling.id) }
        verify(atLeast = 1) { arbeidsfordelingService.fastsettBehandledeEnhet(behandling) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(behandling.fagsak.aktør) }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id).shouldNotBeNull()
        assertEquals(1, personopplysningGrunnlag.personer.size)

        val person = personopplysningGrunnlag.personer.single()
        assertEquals(behandling.fagsak.aktør, person.aktør)
        assertEquals(Målform.NB, person.målform)
        assertEquals(PersonType.SØKER, person.type)

        assertTrue(person.sivilstander.isNotEmpty())
        assertTrue(person.bostedsadresser.isNotEmpty())
        assertTrue(person.statsborgerskap.isNotEmpty())
        assertTrue(person.statsborgerskap.isNotEmpty())
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for Revurdering`() {
        val barnAktør = aktørRepository.saveAndFlush(randomAktør())
        every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) } returns listOf(barnAktør)
        every { personOpplysningerService.hentPersoninfoEnkel(any()) } returns lagPdlPersonInfo(enkelPersonInfo = true, erBarn = true)

        val gammelPersonopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            søkerAktør = behandling.fagsak.aktør,
            barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
            barnAktør = listOf(barnAktør)
        )
        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)
        behandlingRepository.saveAndFlush(behandling.copy(status = BehandlingStatus.AVSLUTTET, aktiv = false))

        val revurdering = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = behandling.fagsak,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(revurdering.id) }

        verify(atLeast = 1) { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) }
        verify(atLeast = 1) { arbeidsfordelingService.fastsettBehandledeEnhet(revurdering) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(revurdering.fagsak.aktør) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(barnAktør) }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(revurdering.id).shouldNotBeNull()
        assertEquals(2, personopplysningGrunnlag.personer.size)

        val søker = personopplysningGrunnlag.personer.single { it.type == PersonType.SØKER }
        assertEquals(revurdering.fagsak.aktør, søker.aktør)
        assertEquals(Målform.NB, søker.målform)

        assertTrue(søker.sivilstander.isNotEmpty())
        assertTrue(søker.bostedsadresser.isNotEmpty())
        assertTrue(søker.statsborgerskap.isNotEmpty())
        assertTrue(søker.statsborgerskap.isNotEmpty())

        val barn = personopplysningGrunnlag.personer.single { it.type == PersonType.BARN }
        assertEquals(barnAktør, barn.aktør)
        assertEquals(Målform.NB, barn.målform)

        assertTrue(barn.sivilstander.isNotEmpty())
        assertTrue(barn.bostedsadresser.isNotEmpty())
        assertTrue(barn.statsborgerskap.isNotEmpty())
        assertTrue(barn.statsborgerskap.isNotEmpty())
    }
}
