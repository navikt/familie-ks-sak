package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling.steg

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPdlPersonInfo
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.RegistrereSøknadSteg
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.kjerne.søknad.domene.SøknadGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RegistrereSøknadStegTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var registrereSøknadSteg: RegistrereSøknadSteg

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var søknadGrunnlagRepository: SøknadGrunnlagRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockkBean(relaxed = true)
    private lateinit var personOpplysningerService: PersonOpplysningerService

    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    private lateinit var behandling: Behandling

    private lateinit var søker: Aktør

    private lateinit var barn1: Aktør

    private lateinit var barn2: Aktør

    @BeforeEach
    fun init() {
        søker = aktørRepository.saveAndFlush(randomAktør())
        barn1 = aktørRepository.saveAndFlush(randomAktør())
        barn2 = aktørRepository.saveAndFlush(randomAktør())
        val fagsak = fagsakRepository.saveAndFlush(lagFagsak(søker))
        behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(behandling.id, søker.aktivFødselsnummer(), søkerAktør = søker)
        personopplysningGrunnlagRepository.saveAndFlush(personopplysningGrunnlag)

        // Mocker ut tjenester som kjører eksterne kall
        every { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns lagPdlPersonInfo()
        every { arbeidsfordelingService.fastsettBehandledeEnhet(any()) } just runs
    }

    @Test
    fun `utførSteg - skal opprette SøknadGrunnlag og oppdatere personopplysningsgrunnlag for FGB`() {
        val søknadDto = SøknadDto(
            søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
            barnaMedOpplysninger = listOf(
                BarnMedOpplysningerDto(ident = barn1.aktivFødselsnummer()),
                BarnMedOpplysningerDto(ident = barn2.aktivFødselsnummer())
            ),
            endringAvOpplysningerBegrunnelse = ""
        )

        // Valider at aktivt personopplysningsgrunnlag kun inneholder søker
        val personopplysningGrunnlagFørSteg = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id)
        assertEquals(1, personopplysningGrunnlagFørSteg.personer.size)

        registrereSøknadSteg.utførSteg(
            behandling.id,
            RegistrerSøknadDto(
                søknadDto,
                bekreftEndringerViaFrontend = true
            )
        )

        // Valider at lagret søknad stemmer med innsendt søknad
        val søknadGrunnlag = søknadGrunnlagRepository.finnAktiv(behandling.id)
        assertNotNull(søknadGrunnlag)
        assertEquals(behandling.id, søknadGrunnlag!!.behandlingId)
        val lagretSøknad = søknadGrunnlag.tilSøknadDto()
        assertTrue(
            lagretSøknad.barnaMedOpplysninger.all {
                it.ident in listOf(
                    barn1.aktivFødselsnummer(),
                    barn2.aktivFødselsnummer()
                )
            }
        )
        assertEquals(2, lagretSøknad.barnaMedOpplysninger.size)
        assertEquals(søker.aktivFødselsnummer(), lagretSøknad.søkerMedOpplysninger.ident)

        // Valider at aktivt personopplysningsgrunnlag er oppdatert med barna
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id)
        assertEquals(3, personopplysningGrunnlag.personer.size)
        assertEquals(2, personopplysningGrunnlag.barna.size)
    }

    @Test
    fun `utførSteg - skal deaktivere eksisterende SøknadGrunnlag dersom det finnes fra før av og deretter opprette et nytt`() {
        val søknadDto = SøknadDto(
            søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
            barnaMedOpplysninger = listOf(
                BarnMedOpplysningerDto(ident = barn1.aktivFødselsnummer())
            ),
            endringAvOpplysningerBegrunnelse = ""
        )

        registrereSøknadSteg.utførSteg(behandling.id, RegistrerSøknadDto(søknadDto, true))

        val søknadDto2 = SøknadDto(
            søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
            barnaMedOpplysninger = listOf(
                BarnMedOpplysningerDto(ident = barn1.aktivFødselsnummer()),
                BarnMedOpplysningerDto(ident = barn2.aktivFødselsnummer())
            ),
            endringAvOpplysningerBegrunnelse = ""
        )

        registrereSøknadSteg.utførSteg(behandling.id, RegistrerSøknadDto(søknadDto2, true))

        val søknadsGrunnlag = søknadGrunnlagRepository.hentAlle(behandling.id)
        val aktivtSøknadsGrunnlag = søknadsGrunnlag.singleOrNull { it.aktiv }

        assertEquals(2, søknadsGrunnlag.size)
        assertNotNull { aktivtSøknadsGrunnlag }
        assertEquals(2, aktivtSøknadsGrunnlag?.tilSøknadDto()?.barnaMedOpplysninger?.size)
    }
}
