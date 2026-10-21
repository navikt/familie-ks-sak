package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehageBydel
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehageBydelRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.data.lagBarnehagebarn
import no.nav.familie.ks.sak.integrasjon.nbr.NasjonalBarnehageRegisterKlient
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrEnhet
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrRelasjon
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrRelasjonstype
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrRelatertEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BarnehageBydelServiceIntegrasjonsTest(
    @Autowired private val barnehagebarnRepository: BarnehagebarnRepository,
    @Autowired private val barnehageBydelRepository: BarnehageBydelRepository,
) : OppslagSpringRunnerTest() {
    private val nasjonalBarnehageRegisterKlient = mockk<NasjonalBarnehageRegisterKlient>()
    private val barnehageBydelService =
        BarnehageBydelService(barnehagebarnRepository, barnehageBydelRepository, nasjonalBarnehageRegisterKlient)

    @Test
    fun `henter bydel fra NBR og lagrer den når barnehagen ikke er slått opp tidligere`() {
        // Arrange
        val barnehagebarn = barnehagebarnRepository.save(lagBarnehagebarn(kommuneNr = OSLO, barnehageOrgNummer = BARNEHAGE))
        every { nasjonalBarnehageRegisterKlient.hentEnhet(BARNEHAGE) } returns lagEnhet(BARNEHAGE, SAGENE)

        // Act
        barnehageBydelService.oppdaterBydelOrgNummerPåBarnehagebarn(barnehagebarn.id)

        // Assert
        assertThat(barnehagebarnRepository.findById(barnehagebarn.id).get().bydelOrgNummer).isEqualTo(SAGENE)
        assertThat(barnehageBydelRepository.findByBarnehageOrgNummer(BARNEHAGE)?.bydelOrgNummer).isEqualTo(SAGENE)
    }

    @Test
    fun `bruker lagret bydel uten å slå opp i NBR når barnehagen er slått opp tidligere`() {
        // Arrange
        barnehageBydelRepository.save(BarnehageBydel(barnehageOrgNummer = BARNEHAGE, bydelOrgNummer = SAGENE))
        val barnehagebarn = barnehagebarnRepository.save(lagBarnehagebarn(kommuneNr = OSLO, barnehageOrgNummer = BARNEHAGE))

        // Act
        barnehageBydelService.oppdaterBydelOrgNummerPåBarnehagebarn(barnehagebarn.id)

        // Assert
        assertThat(barnehagebarnRepository.findById(barnehagebarn.id).get().bydelOrgNummer).isEqualTo(SAGENE)
        verify(exactly = 0) { nasjonalBarnehageRegisterKlient.hentEnhet(any()) }
    }

    @Test
    fun `lagrer ingen bydel når NBR ikke finner bydel for barnehagen`() {
        // Arrange
        val barnehagebarn = barnehagebarnRepository.save(lagBarnehagebarn(kommuneNr = OSLO, barnehageOrgNummer = BARNEHAGE))
        every { nasjonalBarnehageRegisterKlient.hentEnhet(BARNEHAGE) } returns null

        // Act
        barnehageBydelService.oppdaterBydelOrgNummerPåBarnehagebarn(barnehagebarn.id)

        // Assert
        assertThat(barnehagebarnRepository.findById(barnehagebarn.id).get().bydelOrgNummer).isNull()
        assertThat(barnehageBydelRepository.findByBarnehageOrgNummer(BARNEHAGE)).isNull()
    }

    @Test
    fun `lagrer ikke barnehagen på nytt når den allerede finnes`() {
        // Arrange
        barnehageBydelRepository.lagreHvisIkkeFinnes(barnehageOrgNummer = BARNEHAGE, bydelOrgNummer = SAGENE)

        // Act
        barnehageBydelRepository.lagreHvisIkkeFinnes(barnehageOrgNummer = BARNEHAGE, bydelOrgNummer = SAGENE)

        // Assert
        assertThat(barnehageBydelRepository.findAll()).hasSize(1)
    }

    @Test
    fun `oppdatering av bydel gjør ikke at kommunen regnes som sendt inn siste døgn`() {
        // Arrange
        val barnehagebarnService =
            BarnehagebarnService(
                barnehagebarnRepository = barnehagebarnRepository,
                andelTilkjentYtelseRepository = mockk(),
                fagsakRepository = mockk(),
                behandlingRepository = mockk(),
                vilkårsvurderingService = mockk(),
                taskService = mockk(),
            )
        val toDagerSiden = LocalDateTime.now().minusDays(2)
        val barnehagebarn =
            barnehagebarnRepository.save(
                lagBarnehagebarn(
                    kommuneNr = OSLO,
                    barnehageOrgNummer = BARNEHAGE,
                    opprettetTidspunkt = toDagerSiden,
                ).apply { endretTidspunkt = toDagerSiden },
            )
        every { nasjonalBarnehageRegisterKlient.hentEnhet(BARNEHAGE) } returns lagEnhet(BARNEHAGE, SAGENE)

        // Act
        barnehageBydelService.oppdaterBydelOrgNummerPåBarnehagebarn(barnehagebarn.id)

        // Assert
        val oppdatertBarnehagebarn = barnehagebarnRepository.findById(barnehagebarn.id).get()
        assertThat(oppdatertBarnehagebarn.bydelOrgNummer).isEqualTo(SAGENE)
        assertThat(oppdatertBarnehagebarn.endretTidspunkt).isAfter(LocalDateTime.now().minusDays(1))
        assertThat(barnehagebarnService.finnKommunerSendtInnSisteDøgn()).isEmpty()
    }

    private fun lagEnhet(
        organisasjonsnummer: String,
        bydelOrgNummer: String,
    ) = NbrEnhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = "Barnehage",
        erAktiv = true,
        foreldreRelasjoner =
            listOf(
                NbrRelasjon(
                    enhet = NbrRelatertEnhet(organisasjonsnummer = bydelOrgNummer, navn = "Bydel"),
                    relasjonstype = NbrRelasjonstype(id = "10", navn = "Administrativ struktur"),
                ),
            ),
    )

    companion object {
        private const val OSLO = "0301"
        private const val SAGENE = "974778726"
        private const val BARNEHAGE = "111111111"
    }
}
