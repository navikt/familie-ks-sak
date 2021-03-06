package no.nav.familie.ks.sak.app.behandling;

import no.finn.unleash.Unleash;
import no.nav.familie.kontrakter.ks.søknad.testdata.SøknadTestdata;
import no.nav.familie.ks.sak.FaktagrunnlagTestBuilder;
import no.nav.familie.ks.sak.app.behandling.domene.Behandling;
import no.nav.familie.ks.sak.app.behandling.domene.BehandlingRepository;
import no.nav.familie.ks.sak.app.behandling.domene.FagsakRepository;
import no.nav.familie.ks.sak.app.behandling.domene.grunnlag.barnehagebarn.BarnehageBarnGrunnlagRepository;
import no.nav.familie.ks.sak.app.behandling.domene.grunnlag.personopplysning.PersonopplysningGrunnlagRepository;
import no.nav.familie.ks.sak.app.behandling.domene.grunnlag.søknad.SøknadGrunnlagRepository;
import no.nav.familie.ks.sak.app.behandling.domene.typer.AktørId;
import no.nav.familie.ks.sak.app.integrasjon.IntegrasjonTjeneste;
import no.nav.familie.ks.sak.config.ApplicationConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    classes = {ApplicationConfig.class})
@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class)
@ActiveProfiles("dev")
public class BehandlingslagerServiceTest {

    @MockBean
    private IntegrasjonTjeneste integrasjonTjeneste;

    @Autowired
    private BehandlingslagerService tjeneste;

    @Autowired
    private FagsakRepository fagsakRepository;
    @Autowired
    private BehandlingRepository behandlingRepository;
    @Autowired
    private SøknadGrunnlagRepository søknadGrunnlagRepository;
    @Autowired
    private BarnehageBarnGrunnlagRepository barnehageBarnGrunnlagRepository;

    @MockBean
    private PersonopplysningGrunnlagRepository personopplysningGrunnlagRepository;

    @MockBean
    private Unleash unleash;

    @Before
    public void setUp() {
        when(personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()))
            .thenReturn(FaktagrunnlagTestBuilder.genererPersonopplysningGrunnlag());
        when(integrasjonTjeneste.hentAktørId(ArgumentMatchers.any())).thenAnswer(i -> new AktørId(String.valueOf(i.getArguments()[0])));

        when(integrasjonTjeneste.hentPersoninfoFor(any()))
            .thenReturn(
                FaktagrunnlagTestBuilder.faktaBeggeForeldreOgBarnNorskStatsborger().getForelder().getPersoninfo(),
                FaktagrunnlagTestBuilder.faktaBeggeForeldreOgBarnNorskStatsborger().getAnnenForelder().getPersoninfo(),
                FaktagrunnlagTestBuilder.faktaBeggeForeldreOgBarnNorskStatsborger().getBarn().getPersoninfo()
            );
        when(integrasjonTjeneste.hentHistorikkFor(any(), any()))
            .thenReturn(
                FaktagrunnlagTestBuilder.faktaBeggeForeldreOgBarnNorskStatsborger().getForelder().getPersonhistorikkInfo(),
                FaktagrunnlagTestBuilder.faktaBeggeForeldreOgBarnNorskStatsborger().getAnnenForelder().getPersonhistorikkInfo(),
                FaktagrunnlagTestBuilder.faktaBeggeForeldreOgBarnNorskStatsborger().getBarn().getPersonhistorikkInfo()
            );
    }

    @Test
    public void skal_lagre_søknad_og_hente_opp_igjen() {
        final var søknad = SøknadTestdata.norskFamilieMedBarnehageplass();
        final var saksnummer = "TEST123";
        Behandling nyBehandling = tjeneste.nyBehandling(søknad, saksnummer, "12345678");
        tjeneste.trekkUtOgPersisterSøknad(nyBehandling, søknad);
        final var fagsaker = fagsakRepository.findAll();
        assertThat(fagsaker).hasSize(1);
        assertThat(fagsaker.get(0).getAktørId()).isEqualTo(new AktørId(søknad.getSøkerFødselsnummer()));
        assertThat(fagsaker.get(0).getSaksnummer()).isEqualTo(saksnummer);
        final var behandlinger = behandlingRepository.findAll();
        assertThat(behandlinger).hasSize(1);
        final var behandling = behandlinger.get(0);
        assertThat(behandling.getFagsak().getAktørId()).isEqualTo(new AktørId(søknad.getSøkerFødselsnummer()));
        final var søknader = søknadGrunnlagRepository.findAll();
        final var søknad1 = søknader.get(0).getSøknad();
        assertThat(søknad1.getInnsendtTidspunkt()).isEqualTo(søknad.getInnsendtTidspunkt());
        final var erklæring = søknad1.getErklæring();
        assertThat(erklæring).isNotNull();
        assertThat(søknad1.getUtlandsTilknytning()).isNotNull();
        assertThat(søknad1.getUtlandsTilknytning().getAktørerArbeidYtelseIUtlandet()).hasSize(2);
        assertThat(søknad1.getUtlandsTilknytning().getAktørerTilknytningTilUtlandet()).hasSize(2);
        final var barnehageBarnGrunnlagList = barnehageBarnGrunnlagRepository.findAll();
        assertThat(barnehageBarnGrunnlagList).hasSize(1);
        assertThat(barnehageBarnGrunnlagList.get(0).getFamilieforhold()).isNotNull();
    }
}
