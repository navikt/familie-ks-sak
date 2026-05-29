package no.nav.familie.ks.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.felles.tokenklient.entraid.EntraIDClient
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

@TestConfiguration
class FakeConfig {
    @Bean
    @Primary
    @Profile("postgres", "integrasjonstest", "postgres")
    fun entraIDClientMock(): EntraIDClient {
        val mock = mockk<EntraIDClient>(relaxed = true)
        every { mock.hentMaskinTilMaskinToken(any()) } returns "mock-m2m-token"
        every { mock.hentOboToken(any(), any()) } returns "mock-obo-token"
        return mock
    }

    @Bean
    @Primary
    @Profile("fake-integrasjon-klient")
    fun fakeIntegrasjonKlient(): FakeIntegrasjonKlient = FakeIntegrasjonKlient()

    @Bean
    @Primary
    @Profile("fake-tilbakekreving-klient")
    fun fakeTilbakekrevingKlient(): FakeTilbakekrevingKlient = FakeTilbakekrevingKlient()

    @Bean
    @Primary
    @Profile("fake-pdl-klient")
    fun fakePdlClient(): FakePdlKlient = FakePdlKlient()

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
