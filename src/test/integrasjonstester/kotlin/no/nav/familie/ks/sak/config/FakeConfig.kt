package no.nav.familie.ks.sak.config

import no.nav.familie.ks.sak.fake.FakeBrevKlient
import no.nav.familie.ks.sak.fake.FakeFeatureToggleService
import no.nav.familie.ks.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ks.sak.fake.FakePdlKlient
import no.nav.familie.ks.sak.fake.FakePersonopplysningerService
import no.nav.familie.ks.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ks.sak.fake.FakeTilbakekrevingKlient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.unleash.UnleashService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations

@TestConfiguration
class FakeConfig {
    @Bean
    @Primary
    @Profile("fake-integrasjon-klient")
    fun fakeIntegrasjonKlient(restOperations: RestOperations): FakeIntegrasjonKlient = FakeIntegrasjonKlient(restOperations)

    @Bean
    @Primary
    @Profile("fake-tilbakekreving-klient")
    fun fakeTilbakekrevingKlient(restOperations: RestOperations): FakeTilbakekrevingKlient = FakeTilbakekrevingKlient(restOperations)

    @Bean
    @Primary
    @Profile("fake-pdl-klient")
    fun fakePdlClient(restOperations: RestOperations): FakePdlKlient = FakePdlKlient(restOperations)

    @Bean
    @Primary
    @Profile("mock-pdl")
    fun fakePersonopplysningerService(
        pdlKlient: PdlKlient,
        integrasjonService: IntegrasjonService,
        personidentService: PersonidentService,
        falskIdentitetService: FalskIdentitetService,
    ): FakePersonopplysningerService =
        FakePersonopplysningerService(
            pdlKlient = pdlKlient,
            integrasjonService = integrasjonService,
            personidentService = personidentService,
            falskIdentitetService = falskIdentitetService,
        )

    @Bean
    @Primary
    @Profile("mock-unleash")
    fun fakeFeatureToggleService(
        unleashService: UnleashService,
        behandlingRepository: BehandlingRepository,
        arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    ): FakeFeatureToggleService =
        FakeFeatureToggleService(
            unleashService = unleashService,
            behandlingRepository = behandlingRepository,
            arbeidsfordelingPåBehandlingRepository = arbeidsfordelingPåBehandlingRepository,
        )

    @Bean
    @Primary
    @Profile("mock-brev-klient")
    fun fakeBrevKlient(): FakeBrevKlient = FakeBrevKlient()

    @Bean
    @Primary
    @Profile("fake-task-repository")
    fun fakeTaskRepositoryWrapper(taskService: TaskService): FakeTaskRepositoryWrapper = FakeTaskRepositoryWrapper(taskService)
}
