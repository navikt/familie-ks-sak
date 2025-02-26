package no.nav.familie.ks.sak

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.nimbusds.jose.JOSEObjectType
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.DatabaseCleanupService
import no.nav.familie.ks.sak.config.DbContainerInitializer
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [DevLauncherLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrasjonstest")
@EnableMockOAuth2Server
@Tag("integrationTest")
abstract class OppslagSpringRunnerTest {
    private val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var personIdentRepository: PersonidentRepository

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var vedtaksperiodeRepository: VedtaksperiodeRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    lateinit var søker: Aktør

    lateinit var barn: Aktør

    lateinit var fagsak: Fagsak

    lateinit var behandling: Behandling

    lateinit var personopplysningGrunnlag: PersonopplysningGrunnlag

    lateinit var vedtak: Vedtak

    lateinit var tilkjentYtelse: TilkjentYtelse

    lateinit var søkerPerson: Person

    @LocalServerPort
    var port: Int = 0

    @AfterEach
    @Transactional
    fun reset() {
        loggingEvents.clear()
        resetTableForAllEntityClass()
        clearCaches()
        resetWiremockServers()
    }

    fun opprettSøkerFagsakOgBehandling(
        søker: Aktør = randomAktør(),
        barn: Aktør = randomAktør(),
        fagsakStatus: FagsakStatus = FagsakStatus.OPPRETTET,
        behandlingStatus: BehandlingStatus = BehandlingStatus.UTREDES,
        behandlingResultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT,
        behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    ) {
        this.søker = lagreAktør(søker)
        this.barn = lagreAktør(barn)
        this.fagsak = lagreFagsak(lagFagsak(aktør = søker, status = fagsakStatus))
        this.behandling = lagreBehandling(lagBehandling(fagsak = fagsak, opprettetÅrsak = behandlingÅrsak, status = behandlingStatus, resultat = behandlingResultat))
    }

    fun opprettOgLagreSøker(søker: Aktør = randomAktør()): Aktør {
        this.søker = lagreAktør(søker)
        return this.søker
    }

    fun opprettOgLagreBarn(barn: Aktør = randomAktør()): Aktør {
        this.barn = lagreAktør(barn)
        return this.barn
    }

    fun opprettOgLagreFagsak(fagsak: Fagsak = lagFagsak()): Fagsak {
        this.fagsak = lagreFagsak(fagsak)
        return this.fagsak
    }

    fun opprettOgLagreBehandling(behandling: Behandling = lagBehandling()): Behandling {
        this.behandling = lagreBehandling(behandling)
        return this.behandling
    }

    fun opprettPersonopplysningGrunnlagOgPersonForBehandling(
        behandlingId: Long = behandling.id,
        lagBarn: Boolean = false,
        fødselsdatoBarn: LocalDate = LocalDate.of(2022, 1, 1),
    ) {
        personopplysningGrunnlag = lagrePersonopplysningGrunnlag(PersonopplysningGrunnlag(behandlingId = behandlingId))

        søkerPerson =
            lagrePerson(
                Person(
                    aktør = søker,
                    type = PersonType.SØKER,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = LocalDate.of(2019, 1, 1),
                    navn = "",
                    kjønn = Kjønn.KVINNE,
                ).also { søker ->
                    søker.statsborgerskap =
                        mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
                    søker.bostedsadresser = mutableListOf()
                    søker.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.GIFT, person = søker))
                    personopplysningGrunnlag.personer.add(søker)
                },
            )

        if (lagBarn) {
            lagrePerson(
                Person(
                    aktør = barn,
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = fødselsdatoBarn,
                    navn = "",
                    kjønn = Kjønn.KVINNE,
                ).also { barn ->
                    barn.statsborgerskap =
                        mutableListOf(
                            GrStatsborgerskap(
                                landkode = "NOR",
                                medlemskap = Medlemskap.NORDEN,
                                person = barn,
                            ),
                        )
                    barn.bostedsadresser = mutableListOf()
                    barn.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.GIFT, person = barn))
                    personopplysningGrunnlag.personer.add(barn)
                },
            )
        }
    }

    fun opprettVilkårsvurdering(
        aktør: Aktør,
        behandling: Behandling,
        resultat: Resultat,
    ) {
        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = aktør,
                behandling = behandling,
                resultat = resultat,
            )
        vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    fun opprettOppfyltVilkårsvurdering(
        personer: Collection<Person> = this.personopplysningGrunnlag.personer,
        behandling: Behandling = this.behandling,
        periodeFom: LocalDate? = null,
        periodeTom: LocalDate? = null,
    ) {
        val vilkårsvurdering =
            lagVilkårsvurderingOppfylt(
                personer = personer,
                behandling = behandling,
                periodeFom = periodeFom,
                periodeTom = periodeTom,
            )
        vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    fun opprettVedtaksperiodeMedBegrunnelser(
        vedtak: Vedtak = this.vedtak,
        begrunnelser: List<NasjonalEllerFellesBegrunnelse> = emptyList(),
    ) {
        vedtaksperiodeRepository.save(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak,
                begrunnelser = { vedtaksperiodeMedBegrunnelser ->
                    begrunnelser.map {
                        NasjonalEllerFellesBegrunnelseDB(
                            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                            nasjonalEllerFellesBegrunnelse = it,
                        )
                    }
                },
            ),
        )
    }

    protected fun lokalTestToken(
        issuerId: String = "azuread",
        subject: String = "subject1",
        behandlerRolle: BehandlerRolle? = null,
    ): String {
        val behandlerRolleId =
            when (behandlerRolle) {
                BehandlerRolle.VEILEDER -> rolleConfig.VEILEDER_ROLLE
                BehandlerRolle.SAKSBEHANDLER -> rolleConfig.SAKSBEHANDLER_ROLLE
                BehandlerRolle.BESLUTTER -> rolleConfig.BESLUTTER_ROLLE
                else -> ""
            }

        return mockOAuth2Server
            .issueToken(
                issuerId,
                "theclientid",
                DefaultOAuth2TokenCallback(
                    issuerId,
                    subject,
                    JOSEObjectType.JWT.type,
                    listOf("familie-ks-sak-test"),
                    mapOf(Pair("NAVident", "test"), Pair("groups", listOf(behandlerRolleId))),
                    3600,
                ),
            ).serialize()
    }

    private fun resetWiremockServers() = applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)

    private fun clearCaches() =
        cacheManager.cacheNames
            .mapNotNull { cacheManager.getCache(it) }
            .forEach { it.clear() }

    private fun resetTableForAllEntityClass() = databaseCleanupService.truncate()

    fun lagreAktør(aktør: Aktør): Aktør =
        aktørRepository.saveAndFlush(aktør).also {
            personIdentRepository.saveAndFlush(
                Personident(
                    fødselsnummer = it.aktivFødselsnummer(),
                    aktør = it,
                    aktiv = true,
                    gjelderTil = null,
                ),
            )
        }

    fun lagrePerson(person: Person): Person = personRepository.saveAndFlush(person)

    fun lagrePersonopplysningGrunnlag(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag = personopplysningGrunnlagRepository.saveAndFlush(personopplysningGrunnlag)

    fun lagreFagsak(fagsak: Fagsak): Fagsak = fagsakRepository.saveAndFlush(fagsak)

    fun lagreBehandling(behandling: Behandling): Behandling = behandlingRepository.saveAndFlush(behandling)

    fun lagreArbeidsfordeling(arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling) {
        arbeidsfordelingPåBehandlingRepository.saveAndFlush(arbeidsfordelingPåBehandling)
    }

    fun lagTilkjentYtelse(utbetalingsOppdrag: String? = null) {
        tilkjentYtelse =
            tilkjentYtelseRepository.saveAndFlush(
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now(),
                    utbetalingsoppdrag = utbetalingsOppdrag,
                ),
            )
    }

    fun lagVedtak(
        behandling: Behandling = this.behandling,
    ) {
        vedtak = vedtakRepository.saveAndFlush(Vedtak(behandling = behandling))
    }

    companion object {
        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> = ListAppender<ILoggingEvent>().apply { start() }
    }
}
