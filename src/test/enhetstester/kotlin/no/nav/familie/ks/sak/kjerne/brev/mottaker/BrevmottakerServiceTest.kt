package no.nav.familie.ks.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.Bruker
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull

internal class BrevmottakerServiceTest {
    private val brevmottakerRepository = mockk<BrevmottakerRepository>()
    private val validerBrevmottakerService = mockk<ValiderBrevmottakerService>()
    private val loggService = mockk<LoggService>()

    private val brevmottakerService =
        BrevmottakerService(
            loggService = loggService,
            validerBrevmottakerService = validerBrevmottakerService,
            brevmottakerRepository = brevmottakerRepository,
        )

    private val søkersnavn = "Test søker"

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har norsk adresse`() {
        val brevmottakere = listOf(lagBrevMottaker(søkersnavn, mottakerType = MottakerType.FULLMEKTIG))
        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

        assertTrue { mottakerInfo.size == 2 }

        assertTrue(mottakerInfo.first() is Bruker)
        assertTrue { mottakerInfo.first().manuellAdresseInfo == null }

        assertEquals(søkersnavn, mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har utenlandsk adresse`() {
        val brevmottakere =
            listOf(
                lagBrevMottaker(mottakerType = MottakerType.FULLMEKTIG, navn = "Fullmektig navn"),
                lagBrevMottaker(
                    navn = søkersnavn,
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )
        // every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere.map { it.tilBrevMottakerDb(1) }

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

        assertTrue { mottakerInfo.size == 2 }

        assertTrue(mottakerInfo.first().navn.isEmpty()) // (kun adressen til bruker som overstyres)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }

        assertEquals("Fullmektig navn", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er VERGE og bruker har utenlandsk adresse`() {
        val brevmottakere =
            listOf(
                lagBrevMottaker(mottakerType = MottakerType.VERGE, navn = "Verge navn"),
                lagBrevMottaker(
                    navn = søkersnavn,
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

        assertTrue { mottakerInfo.size == 2 }

        assertTrue(mottakerInfo.first().navn.isEmpty()) // (kun adressen til bruker som overstyres)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }

        assertEquals("Verge navn", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har utenlandsk adresse`() {
        val brevmottakere =
            listOf(
                lagBrevMottaker(
                    navn = søkersnavn,
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

        assertTrue { mottakerInfo.size == 1 }

        assertTrue(mottakerInfo.first().navn.isEmpty()) // (kun adressen som overstyrers)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har dødsbo`() {
        val brevmottakere =
            listOf(
                lagBrevMottaker(
                    navn = søkersnavn,
                    mottakerType = MottakerType.DØDSBO,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

        assertTrue { mottakerInfo.size == 1 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal kaste feil når brevmottakere inneholder ugyldig kombinasjon`() {
        val brevmottakere =
            listOf(
                lagBrevMottaker(
                    navn = "Verge navn",
                    mottakerType = MottakerType.VERGE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
                lagBrevMottaker(
                    navn = "Fullmektig navn",
                    mottakerType = MottakerType.FULLMEKTIG,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )

        assertThrows<FunksjonellFeil> {
            brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)
        }.also {
            assertTrue(it.frontendFeilmelding!!.contains("kan ikke kombineres"))
        }
    }

    @Test
    fun `leggTilBrevmottaker skal lagre logg på at brevmottaker legges til`() {
        val brevmottakerDto = mockk<BrevmottakerDto>(relaxed = true)

        every {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                any(),
                any(),
                any(),
            )
        } just runs
        every { loggService.opprettBrevmottakerLogg(any(), false) } just runs
        every { brevmottakerRepository.save(any()) } returns mockk()

        brevmottakerService.leggTilBrevmottaker(brevmottakerDto, 200)

        verify { loggService.opprettBrevmottakerLogg(any(), false) }
        verify { brevmottakerRepository.save(any()) }
    }

    @Test
    fun `fjernBrevmottaker skal kaste feil dersom brevmottakeren ikke finnes`() {
        every { brevmottakerRepository.findByIdOrNull(404) } returns null

        assertThrows<Feil> {
            brevmottakerService.fjernBrevmottaker(404)
        }

        verify { brevmottakerRepository.findByIdOrNull(404) }
    }

    @Test
    fun `fjernBrevmottaker skal lagre logg på at brevmottaker fjernes`() {
        val mocketBrevmottaker = mockk<BrevmottakerDb>()

        every { brevmottakerRepository.findByIdOrNull(200) } returns mocketBrevmottaker
        every { loggService.opprettBrevmottakerLogg(mocketBrevmottaker, true) } just runs
        every { brevmottakerRepository.deleteById(200) } just runs

        brevmottakerService.fjernBrevmottaker(200)

        verify { brevmottakerRepository.findByIdOrNull(200) }
        verify { loggService.opprettBrevmottakerLogg(mocketBrevmottaker, true) }
        verify { brevmottakerRepository.deleteById(200) }
    }

    private fun lagBrevMottaker(
        navn: String,
        mottakerType: MottakerType,
        poststed: String = "Oslo",
        landkode: String = "NO",
    ) = BrevmottakerDto(
        id = 1,
        type = mottakerType,
        navn = navn,
        adresselinje1 = "adresse 1",
        adresselinje2 = "adresse 2",
        postnummer = "000",
        poststed = poststed,
        landkode = landkode,
    )
}
