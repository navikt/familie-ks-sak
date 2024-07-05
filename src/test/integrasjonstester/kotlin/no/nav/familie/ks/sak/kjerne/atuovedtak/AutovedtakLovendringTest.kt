package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.atuovedtak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.autovedtak.AutovedtakLovendringService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakLovendringTest(
    @Autowired private val autovedtakLovendringService: AutovedtakLovendringService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personRepository: PersonRepository,
) : OppslagSpringRunnerTest() {
    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockkBean
    private lateinit var sakStatistikkService: SakStatistikkService

    @MockkBean
    private lateinit var unleashNextMedContextService: UnleashNextMedContextService

    @MockkBean
    private lateinit var personService: PersonService

    @BeforeEach
    fun setUp() {
        justRun { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) }
        justRun { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
        every { unleashNextMedContextService.isEnabled(any()) } returns true
        every { personService.lagPerson(any(), any(), any(), any(), any()) } answers {
            val aktør = firstArg<Aktør>()
            personRepository.findByAktør(aktør).first()
        }
    }

    @Test
    fun `automatisk revurdering av fagsak som blir truffet av nytt regelverk gir endret utbetaling`() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = Behandlingsresultat.INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(
            behandlingId = behandling.id,
            lagBarn = true,
            fødselsdatoBarn = LocalDate.of(2023, 4, 1),
        )
        opprettVilkårsvurdering(søker, behandling, Resultat.OPPFYLT)

        lagTilkjentYtelse(null)
        tilkjentYtelse.andelerTilkjentYtelse.add(
            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                    aktør = barn,
                    stønadFom = YearMonth.of(2024, 5),
                    stønadTom = YearMonth.of(2025, 3),
                ),
            ),
        )

        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = behandling.fagsak.id)

        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.ENDRET_UTBETALING)

        // TODO: Hva bør sjekkes her?
    }
}
