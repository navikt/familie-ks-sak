package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.mockk
import mockAdopsjonService
import mockIntegrasjonClient
import no.nav.familie.ks.sak.common.TestClockProvider
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonValidator
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.BeregnAndelTilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFebruar2025.LovverkFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025.LovverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.TilpassDifferanseberegningEtterTilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository

class CucumberMock(
    stepDefinition: StepDefinition,
) {
    val clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(stepDefinition.dagensDato)

    val vilkårsvurderingRepositoryMock = mockVilkårsvurderingRepository(stepDefinition)
    val andelTilkjentYtelseRepositoryMock = mockAndelTilkjentYtelseRepository(stepDefinition)
    val valutakursRepositoryMock = mockValutakursRepository(stepDefinition)
    val utenlandskPeriodebeløpRepositoryMock = mockUtenlandskPeriodebeløpRepository(stepDefinition)
    val tilkjentYtelseRepositoryMock = mockTilkjentYtelseRepository(stepDefinition)
    val personopplysningGrunnlagRepositoryMock = mockPersonopplysningGrunnlagRepository(stepDefinition)
    val endretUtbetalingAndelRepositoryMock = mockEndretUtbetalingAndelRepository(stepDefinition)
    val overgangsordningAndelRepositoryMock = mockOvergangsordningAndelRepository(stepDefinition)
    val fagsakRepositoryMock = mockFagsakRepository(stepDefinition)
    val vedtakRepositoryMock = mockVedtakRepository(stepDefinition)
    val taskServiceMock = mockTaskService()
    val loggServiceMock = mockLoggService()

    val personService = mockk<PersonService>()
    val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    val aktørRepositoryMock = mockk<AktørRepository>()
    val pdlClientMock = mockk<PdlClient>()
    val personidentRepositoryMock = mockk<PersonidentRepository>()
    val behandlingRepositoryMock = mockk<BehandlingRepository>()
    val integrasjonServiceMock = mockk<IntegrasjonService>()
    val personRepository = mockk<PersonRepository>()
    val arbeidsfordelingServiceMock = mockk<ArbeidsfordelingService>()
    val praksisendring2024Service = mockPraksisendring2024Service()
    val adopsjonServiceMock = mockAdopsjonService()
    val integrasjonClientMock = mockIntegrasjonClient()

    val beregnAndelTilkjentYtelseService =
        BeregnAndelTilkjentYtelseService(
            andelGeneratorLookup = AndelGenerator.Lookup(listOf(LovverkFebruar2025AndelGenerator(), LovverkFørFebruar2025AndelGenerator())),
            adopsjonService = adopsjonServiceMock,
        )

    val tilkjentYtelseService = TilkjentYtelseService(beregnAndelTilkjentYtelseService, overgangsordningAndelRepositoryMock, praksisendring2024Service)

    val tilpassDifferanseberegningEtterTilkjentYtelseService =
        TilpassDifferanseberegningEtterTilkjentYtelseService(
            valutakursRepository = valutakursRepositoryMock,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepositoryMock,
            tilkjentYtelseRepository = tilkjentYtelseRepositoryMock,
        )

    val andelerTilkjentYtelseOgEndreteUtbetalingerService =
        AndelerTilkjentYtelseOgEndreteUtbetalingerService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepositoryMock,
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepositoryMock,
            vilkårsvurderingRepository = vilkårsvurderingRepositoryMock,
        )

    val personidentService =
        PersonidentService(
            personidentRepository = personidentRepositoryMock,
            aktørRepository = aktørRepositoryMock,
            pdlClient = pdlClientMock,
            taskService = taskServiceMock,
        )

    val fagsakService =
        FagsakService(
            personidentService = personidentService,
            integrasjonService = integrasjonServiceMock,
            personopplysningerService = personopplysningerServiceMock,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepositoryMock,
            fagsakRepository = fagsakRepositoryMock,
            personRepository = personRepository,
            behandlingRepository = behandlingRepositoryMock,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            taskService = taskServiceMock,
            vedtakRepository = vedtakRepositoryMock,
            andelerTilkjentYtelseRepository = andelTilkjentYtelseRepositoryMock,
            clockProvider = clockProvider,
            adopsjonService = adopsjonServiceMock,
            integrasjonClient = integrasjonClientMock,
        )

    val beregningService =
        BeregningService(
            tilkjentYtelseRepository = tilkjentYtelseRepositoryMock,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepositoryMock,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            fagsakService = fagsakService,
            tilkjentYtelseEndretAbonnenter = listOf(tilpassDifferanseberegningEtterTilkjentYtelseService),
            tilkjentYtelseService = tilkjentYtelseService,
        )

    val personopplysningGrunnlagService =
        PersonopplysningGrunnlagService(
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepositoryMock,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepositoryMock,
            beregningService = beregningService,
            personService = personService,
            arbeidsfordelingService = arbeidsfordelingServiceMock,
            personidentService = personidentService,
            loggService = loggServiceMock,
        )

    val adopsjonValidator = AdopsjonValidator(adopsjonServiceMock)

    val vilkårsvurderingService =
        VilkårsvurderingService(
            vilkårsvurderingRepository = vilkårsvurderingRepositoryMock,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            sanityService = mockk(),
            personidentService = personidentService,
            adopsjonService = adopsjonServiceMock,
            adopsjonValidator = adopsjonValidator,
        )
}
