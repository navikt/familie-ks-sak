package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.maskinellrevurdering.MaskinellRevurderingLovendringService
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class MaskinellRevurderingLovendringTest(
    @Autowired private val maskinellRevurderingLovendringService: MaskinellRevurderingLovendringService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val behandlingService: BehandlingService,
) : OppslagSpringRunnerTest() {
    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockkBean
    private lateinit var personOpplysningerService: PersonOpplysningerService

    @MockkBean
    private lateinit var sakStatistikkService: SakStatistikkService

    @BeforeEach
    fun setUp() {
        every { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) } just runs
        every { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) } just runs
        every { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } answers {
            PdlPersonInfo(
                fødselsdato = LocalDate.of(2023, 4, 1),
            )
        }
    }

    @Test
    fun `maskinellRevurdering lager revurdering og kjører til BESLUTTE_VEDTAK`() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = Behandlingsresultat.INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)
        opprettVilkårsvurdering(søker, behandling, Resultat.OPPFYLT, VilkårRegelsett.LOV_AUGUST_2021)

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

        maskinellRevurderingLovendringService.revurderFagsak(fagsakId = behandling.fagsak.id)

        val behandlinger = behandlingService.hentBehandlingerPåFagsak(fagsakId = behandling.fagsak.id)

        println(behandlinger)
    }
}
