package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.DatabaseCleanupService
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.maskinellrevurdering.MaskinellRevurderingLovendringService
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class MaskinellRevurderingLovendringTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val aktørRepository: AktørRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val personOpplysningerService: PersonOpplysningerService,
    @Autowired private val maskinellRevurderingLovendringService: MaskinellRevurderingLovendringService,
) : OppslagSpringRunnerTest() {
    private var skalVenteLitt = false // for å unngå at behandlingen opprettes med samme tidspunkt

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockkBean
    private lateinit var endretUtbetalingAndelService: EndretUtbetalingAndelService

    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockBean
    private lateinit var personService: PersonService

    @MockkBean
    private lateinit var mockPdlClient: PdlClient

    @BeforeEach
    fun setUp() {
        skalVenteLitt = false
        databaseCleanupService.truncate()
        fagsak = opprettLøpendeFagsak()

        every { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) } just runs // unngå å mocke PDL
    }

    @Test
    fun `maskinellRevurdering lager revurdering og kjører til BESLUTTE_VEDTAK`() {
        behandling = opprettBehandling()
        val barnAktør = lagreAktør(randomAktør())

        val gammelPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )

        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)

        maskinellRevurderingLovendringService.opprettMaskinellRevurderingOgKjørTilBeslutteVedtak(
            fagsak.aktør,
            BehandlingKategori.NASJONAL,
        )
    }

    private fun opprettLøpendeFagsak(): Fagsak =
        fagsakService.lagre(
            lagFagsak(
                aktør = aktørRepository.saveAndFlush(randomAktør()),
                status = FagsakStatus.LØPENDE,
            ),
        )

    private fun opprettBehandling(
        status: BehandlingStatus = BehandlingStatus.AVSLUTTET,
        resultat: Behandlingsresultat = Behandlingsresultat.INNVILGET,
        aktiv: Boolean = true,
    ): Behandling {
        if (skalVenteLitt) {
            Thread.sleep(10)
        } else {
            skalVenteLitt = true
        }
        val behandling =
            Behandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                type = BehandlingType.REVURDERING,
                kategori = BehandlingKategori.NASJONAL,
                status = status,
                aktiv = aktiv,
                resultat = resultat,
            ).initBehandlingStegTilstand()
        return behandlingRepository.saveAndFlush(behandling)
    }
}
