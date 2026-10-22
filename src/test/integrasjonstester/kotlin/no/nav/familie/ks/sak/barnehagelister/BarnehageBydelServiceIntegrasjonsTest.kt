package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehageBydel
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehageBydelRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.data.lagBarnehagebarn
import no.nav.familie.ks.sak.fake.FakeNasjonalBarnehageRegisterKlient
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrEnhet
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrRelasjon
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrRelasjonstype
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrRelatertEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class BarnehageBydelServiceIntegrasjonsTest(
    @Autowired private val barnehagebarnRepository: BarnehagebarnRepository,
    @Autowired private val barnehageBydelRepository: BarnehageBydelRepository,
    @Autowired private val barnehageBydelService: BarnehageBydelService,
    @Autowired private val fakeNasjonalBarnehageRegisterKlient: FakeNasjonalBarnehageRegisterKlient,
) : OppslagSpringRunnerTest() {
    @BeforeEach
    fun setup() {
        fakeNasjonalBarnehageRegisterKlient.reset()
    }

    @Test
    fun `henter bydel fra NBR og lagrer den når barnehagen ikke er slått opp tidligere`() {
        // Arrange
        val barnehagebarn = barnehagebarnRepository.save(lagBarnehagebarn(kommuneNr = OSLO, barnehageOrganisasjonsnummer = BARNEHAGE))
        fakeNasjonalBarnehageRegisterKlient.leggTilEnhet(lagEnhet(BARNEHAGE, SAGENE))

        // Act
        barnehageBydelService.oppdaterBydelOrganisasjonsnummerPåBarnehagebarn(barnehagebarn.id)

        // Assert
        assertThat(barnehagebarnRepository.findById(barnehagebarn.id).get().bydelOrganisasjonsnummer).isEqualTo(SAGENE)
        assertThat(barnehageBydelRepository.findByIdOrNull(BARNEHAGE)?.bydelOrganisasjonsnummer).isEqualTo(SAGENE)
    }

    @Test
    fun `bruker lagret bydel uten å slå opp i NBR når barnehagen er slått opp tidligere`() {
        // Arrange
        barnehageBydelRepository.save(BarnehageBydel(barnehageOrganisasjonsnummer = BARNEHAGE, bydelOrganisasjonsnummer = SAGENE))
        val barnehagebarn = barnehagebarnRepository.save(lagBarnehagebarn(kommuneNr = OSLO, barnehageOrganisasjonsnummer = BARNEHAGE))

        // Act
        barnehageBydelService.oppdaterBydelOrganisasjonsnummerPåBarnehagebarn(barnehagebarn.id)

        // Assert
        assertThat(barnehagebarnRepository.findById(barnehagebarn.id).get().bydelOrganisasjonsnummer).isEqualTo(SAGENE)
        assertThat(fakeNasjonalBarnehageRegisterKlient.oppslag).isEmpty()
    }

    @Test
    fun `lagrer ingen bydel når NBR ikke finner bydel for barnehagen`() {
        // Arrange
        val barnehagebarn = barnehagebarnRepository.save(lagBarnehagebarn(kommuneNr = OSLO, barnehageOrganisasjonsnummer = BARNEHAGE))

        // Act
        barnehageBydelService.oppdaterBydelOrganisasjonsnummerPåBarnehagebarn(barnehagebarn.id)

        // Assert
        assertThat(barnehagebarnRepository.findById(barnehagebarn.id).get().bydelOrganisasjonsnummer).isNull()
        assertThat(barnehageBydelRepository.findByIdOrNull(BARNEHAGE)).isNull()
    }

    private fun lagEnhet(
        organisasjonsnummer: String,
        bydelOrganisasjonsnummer: String,
    ) = NbrEnhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = "Barnehage",
        erAktiv = true,
        foreldreRelasjoner =
            listOf(
                NbrRelasjon(
                    enhet = NbrRelatertEnhet(organisasjonsnummer = bydelOrganisasjonsnummer, navn = "Bydel"),
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
