package no.nav.familie.ks.sak.kjerne.behandling.steg

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPdlPersonInfo
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
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
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    @MockkBean(relaxed = true)
    private lateinit var personOpplysningerService: PersonopplysningerService

    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockkBean
    private lateinit var endretUtbetalingAndelService: EndretUtbetalingAndelService

    @MockkBean
    private lateinit var beregningService: BeregningService

    @BeforeEach
    fun init() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        every { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns lagPdlPersonInfo()
        every { arbeidsfordelingService.fastsettBehandlendeEnhet(any()) } just runs
        every { endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(any(), any()) } just runs
        every { integrasjonClient.hentPoststeder() } returns mockk(relaxed = true)
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for FGB`() {
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(behandling.id) }
        verify(atLeast = 1) { arbeidsfordelingService.fastsettBehandlendeEnhet(behandling) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(behandling.fagsak.aktør) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id).shouldNotBeNull()
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
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for Revurdering med opprettet årsak søknad`() {
        val barnAktør = lagreAktør(randomAktør())
        every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) } returns listOf(barnAktør)
        every { personOpplysningerService.hentPersoninfoEnkel(any()) } returns
            lagPdlPersonInfo(
                enkelPersonInfo = true,
                erBarn = true,
            )

        val gammelPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )
        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)

        lagreBehandling(
            behandling.also {
                it.status = BehandlingStatus.AVSLUTTET
                it.aktiv = false
            },
        )

        val revurdering =
            behandlingRepository.saveAndFlush(
                lagBehandling(
                    fagsak = behandling.fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            )
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(revurdering.id) }

        verify(atLeast = 1) { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) }
        verify(atLeast = 1) { arbeidsfordelingService.fastsettBehandlendeEnhet(revurdering) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(revurdering.fagsak.aktør) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(barnAktør) }
        verify(atMost = 1) {
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                any(),
                any(),
            )
        }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(revurdering.id).shouldNotBeNull()
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

        assertTrue { vilkårsvurderingService.finnAktivVilkårsvurdering(revurdering.id) == null }
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for Revurdering med opprettet årsak ÅRLIG_KONTROLL`() {
        val barnAktør = lagreAktør(randomAktør())
        every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) } returns listOf(barnAktør)
        every { personOpplysningerService.hentPersoninfoEnkel(any()) } returns
            lagPdlPersonInfo(
                enkelPersonInfo = true,
                erBarn = true,
            )

        val gammelPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )
        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)
        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        lagreBehandling(
            behandling.also {
                it.status = BehandlingStatus.AVSLUTTET
                it.aktiv = false
            },
        )

        val revurdering =
            behandlingRepository.saveAndFlush(
                lagBehandling(
                    fagsak = behandling.fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                ),
            )
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(revurdering.id) }

        verify(atLeast = 1) { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandling.id) }
        verify(atLeast = 1) { arbeidsfordelingService.fastsettBehandlendeEnhet(revurdering) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(revurdering.fagsak.aktør) }
        verify(atMost = 1) { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(barnAktør) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(revurdering.id).shouldNotBeNull()
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

        assertTrue { vilkårsvurderingService.finnAktivVilkårsvurdering(revurdering.id) != null }
    }
}
