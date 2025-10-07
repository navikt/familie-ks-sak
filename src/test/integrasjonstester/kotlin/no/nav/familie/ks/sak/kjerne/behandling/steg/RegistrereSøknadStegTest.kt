package no.nav.familie.ks.sak.kjerne.behandling.steg

import com.ninjasquad.springmockk.MockkBean
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.dto.tilSøknadGrunnlag
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.data.lagPdlPersonInfo
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.RegistrereSøknadSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlagRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class RegistrereSøknadStegTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var registrereSøknadSteg: RegistrereSøknadSteg

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var søknadGrunnlagRepository: SøknadGrunnlagRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    @MockkBean(relaxed = true)
    private lateinit var personOpplysningerService: PersonopplysningerService

    @MockkBean(relaxed = true)
    private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    private lateinit var barn1: Aktør

    private lateinit var barn2: Aktør

    @BeforeEach
    fun init() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        lagVedtak()
        barn1 = aktørRepository.saveAndFlush(randomAktør())
        barn2 = aktørRepository.saveAndFlush(randomAktør())
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(behandling.id, søker.aktivFødselsnummer(), søkerAktør = søker)
        personopplysningGrunnlagRepository.saveAndFlush(personopplysningGrunnlag)

        // Mocker ut tjenester som kjører eksterne kall
        every { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns lagPdlPersonInfo()
        every { arbeidsfordelingService.fastsettBehandlendeEnhet(any()) } just runs
        every { integrasjonClient.hentPoststeder() } returns mockk(relaxed = true)
    }

    @Test
    fun `utførSteg - skal opprette SøknadGrunnlag og oppdatere personopplysningsgrunnlag for FGB`() {
        val søknadDto =
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = barn1.aktivFødselsnummer(), fødselsdato = LocalDate.now().minusYears(1)),
                        BarnMedOpplysningerDto(ident = barn2.aktivFødselsnummer(), fødselsdato = LocalDate.now().minusYears(1)),
                    ),
                endringAvOpplysningerBegrunnelse = "",
            )

        // Valider at aktivt personopplysningsgrunnlag kun inneholder søker
        val personopplysningGrunnlagFørSteg = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id)
        assertEquals(1, personopplysningGrunnlagFørSteg.personer.size)

        registrereSøknadSteg.utførSteg(
            behandling.id,
            RegistrerSøknadDto(
                søknadDto,
                bekreftEndringerViaFrontend = true,
            ),
        )

        // Valider at lagret søknad stemmer med innsendt søknad
        val søknadGrunnlag = søknadGrunnlagRepository.finnAktiv(behandling.id)
        assertNotNull(søknadGrunnlag)
        assertEquals(behandling.id, søknadGrunnlag!!.behandlingId)
        val lagretSøknad = søknadGrunnlag.tilSøknadDto()
        assertTrue(
            lagretSøknad.barnaMedOpplysninger.all {
                it.personnummer in
                    listOf(
                        barn1.aktivFødselsnummer(),
                        barn2.aktivFødselsnummer(),
                    )
            },
        )
        assertEquals(2, lagretSøknad.barnaMedOpplysninger.size)
        assertEquals(søker.aktivFødselsnummer(), lagretSøknad.søkerMedOpplysninger.ident)

        // Valider at aktivt personopplysningsgrunnlag er oppdatert med barna
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandling.id)
        assertEquals(3, personopplysningGrunnlag.personer.size)
        assertEquals(2, personopplysningGrunnlag.barna.size)

        val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandling.id)
        assertTrue { vilkårsvurdering != null }
        assertTrue { checkNotNull(vilkårsvurdering).personResultater.any { it.erSøkersResultater() } }
        assertTrue {
            checkNotNull(vilkårsvurdering).personResultater.any {
                it.vilkårResultater.any { vilkårResultat -> vilkårResultat.vilkårType.parterDetteGjelderFor.contains(PersonType.BARN) }
            }
        }
    }

    @Test
    fun `utførSteg - skal deaktivere eksisterende SøknadGrunnlag dersom det finnes fra før av og deretter opprette et nytt`() {
        val søknadDto =
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = barn1.aktivFødselsnummer(), fødselsdato = LocalDate.now().minusYears(1)),
                    ),
                endringAvOpplysningerBegrunnelse = "",
            )

        registrereSøknadSteg.utførSteg(behandling.id, RegistrerSøknadDto(søknadDto, true))

        val søknadDto2 =
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = barn1.aktivFødselsnummer(), fødselsdato = LocalDate.now().minusYears(1)),
                        BarnMedOpplysningerDto(ident = barn2.aktivFødselsnummer(), fødselsdato = LocalDate.now().minusYears(1)),
                    ),
                endringAvOpplysningerBegrunnelse = "",
            )

        registrereSøknadSteg.utførSteg(behandling.id, RegistrerSøknadDto(søknadDto2, true))

        val søknadsGrunnlag = søknadGrunnlagRepository.hentAlle(behandling.id)
        val aktivtSøknadsGrunnlag = søknadsGrunnlag.singleOrNull { it.aktiv }

        assertEquals(2, søknadsGrunnlag.size)
        assertNotNull { aktivtSøknadsGrunnlag }
        assertEquals(2, aktivtSøknadsGrunnlag?.tilSøknadDto()?.barnaMedOpplysninger?.size)
    }

    @Test
    fun `utførSteg - skal ikke utføre noen endringer dersom det allerede finnes en identisk søknad`() {
        val søknadDto =
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = barn1.aktivFødselsnummer(), fødselsdato = LocalDate.now().minusYears(1)),
                    ),
                endringAvOpplysningerBegrunnelse = "",
            )

        søknadGrunnlagRepository.saveAndFlush(søknadDto.tilSøknadGrunnlag(behandling.id))

        registrereSøknadSteg.utførSteg(behandling.id, RegistrerSøknadDto(søknadDto, false))

        verify { personOpplysningerService wasNot called }
        verify { arbeidsfordelingService wasNot called }
    }
}
