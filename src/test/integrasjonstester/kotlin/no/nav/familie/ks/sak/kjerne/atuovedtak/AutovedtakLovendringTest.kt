package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.atuovedtak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.autovedtak.AutovedtakLovendringService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutovedtakLovendringTest(
    @Autowired private val autovedtakLovendringService: AutovedtakLovendringService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personRepository: PersonRepository,
    @Autowired private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) : OppslagSpringRunnerTest() {
    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockkBean
    private lateinit var sakStatistikkService: SakStatistikkService

    @MockkBean
    private lateinit var unleashNextMedContextService: UnleashNextMedContextService

    @MockkBean
    private lateinit var personService: PersonService

    @MockkBean
    private lateinit var utbetalingsoppdragService: UtbetalingsoppdragService

    @BeforeEach
    fun setUp() {
        justRun { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) }
        justRun { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
        every { unleashNextMedContextService.isEnabled(any()) } returns true
        every { personService.lagPerson(any(), any(), any(), any(), any()) } answers {
            val aktør = firstArg<Aktør>()
            personRepository.findByAktør(aktør).first()
        }

        justRun { utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(any(), any()) }
    }

    @Test
    fun `automatisk revurdering av fagsak som blir truffet av nytt regelverk gir endret utbetaling`() {
        // arrange
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = Behandlingsresultat.INNVILGET)
        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(
            behandlingId = behandling.id,
            lagBarn = true,
            fødselsdatoBarn = fødselsdatoBarn,
        )
        lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn)

        lagTilkjentYtelse(null)
        tilkjentYtelse.andelerTilkjentYtelse.add(
            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                    aktør = barn,
                    stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth(),
                    stønadTom = fødselsdatoBarn.plusYears(2).minusMonths(1).toYearMonth(), // 11 måneder som i gammelt regelverk
                ),
            ),
        )

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = behandling.fagsak.id)

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.ENDRET_UTBETALING)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(7).toYearMonth()) // 7 måneder som i nytt regelverk
    }

    @Test
    fun `automatisk revurdering av fagsak som ikke blir truffet av nytt regelverk gir ikke endret utbetaling`() {
        // arrange
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = Behandlingsresultat.INNVILGET)
        val fødselsdatoBarn = LocalDate.of(2021, 4, 1)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(
            behandlingId = behandling.id,
            lagBarn = true,
            fødselsdatoBarn = fødselsdatoBarn,
        )
        lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn)

        lagTilkjentYtelse(null)
        val stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth()
        val stønadTom = fødselsdatoBarn.plusYears(2).minusMonths(1).toYearMonth() // 11 måneder som i gammelt regelverk
        tilkjentYtelse.andelerTilkjentYtelse.add(
            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                    aktør = barn,
                    stønadFom = stønadFom,
                    stønadTom = stønadTom,
                ),
            ),
        )

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = behandling.fagsak.id)

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.FORTSATT_OPPHØRT)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(stønadFom)
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(stønadTom)
    }

    private fun lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn: LocalDate?) {
        val vilkårsvurdering =
            Vilkårsvurdering(
                behandling = behandling,
            )
        val søkersPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søker,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdatoBarn,
                periodeTom = null,
                personType = PersonType.SØKER,
                lagFullstendigVilkårResultat = true,
            )
        val barnetsPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdatoBarn,
                periodeTom = null,
                personType = PersonType.BARN,
                lagFullstendigVilkårResultat = true,
            )
        val barnetsAlderVilkår = barnetsPersonResultat.vilkårResultater.find { it.vilkårType == Vilkår.BARNETS_ALDER }!!
        barnetsAlderVilkår.periodeFom = fødselsdatoBarn!!.plusYears(1)
        barnetsAlderVilkår.periodeTom = fødselsdatoBarn.plusYears(2).minusMonths(1)

        vilkårsvurdering.personResultater = setOf(barnetsPersonResultat, søkersPersonResultat)
        vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }
}
