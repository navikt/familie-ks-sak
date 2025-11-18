package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.kodeverk.Bydel
import no.nav.familie.kontrakter.felles.kodeverk.Fylke
import no.nav.familie.kontrakter.felles.kodeverk.Kommune
import no.nav.familie.kontrakter.felles.kodeverk.LandDto
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GeografiHierarkiServiceTest {
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val service = GeografiHierarkiService(integrasjonKlient)

    @Test
    fun `returnerer kommunen og bydeler når kommunen har bydeler (Oslo)`() {
        // Arrange
        every { integrasjonKlient.hentFylkerOgKommuner() } returns geografiFixture()

        // Act
        val resultat = service.hentBydelEllerKommuneKodeTilNavnFraFylkeNr("03")

        // Assert
        assertEquals("Oslo", resultat["0301"])
        assertEquals("Grünerløkka", resultat["0302"])
        assertEquals("Sagene", resultat["0303"])
        assertEquals(3, resultat.size)
    }

    @Test
    fun `returnerer kun kommunen når den ikke har bydeler (Moss i Viken)`() {
        // Arrange
        every { integrasjonKlient.hentFylkerOgKommuner() } returns geografiFixture()

        // Act
        val resultat = service.hentBydelEllerKommuneKodeTilNavnFraFylkeNr("31")

        // Assert
        assertEquals(mapOf("3103" to "Moss"), resultat)
        assertEquals(1, resultat.size)
    }

    @Test
    fun `kaster feil når fylket ikke finnes`() {
        // Arrange
        every { integrasjonKlient.hentFylkerOgKommuner() } returns geografiFixture()

        // Act + Assert
        assertThrows(NoSuchElementException::class.java) {
            service.hentBydelEllerKommuneKodeTilNavnFraFylkeNr("42")
        }
    }

    private fun nb(label: String) = mapOf("nb" to label)

    private fun geografiFixture(): LandDto =
        LandDto(
            undernoder =
                mapOf(
                    // Oslo (03) – Oslo kommune (0301) med bydeler 0302/0303
                    "03" to
                        Fylke(
                            kode = "03",
                            tekster = nb("Oslo"),
                            termer = emptyMap(),
                            undernoder =
                                mapOf(
                                    "0301" to
                                        Kommune(
                                            kode = "0301",
                                            tekster = nb("Oslo"),
                                            termer = emptyMap(),
                                            undernoder =
                                                mapOf(
                                                    "0302" to
                                                        Bydel(
                                                            kode = "0302",
                                                            tekster = nb("Grünerløkka"),
                                                            termer = emptyMap(),
                                                        ),
                                                    "0303" to
                                                        Bydel(
                                                            kode = "0303",
                                                            tekster = nb("Sagene"),
                                                            termer = emptyMap(),
                                                        ),
                                                ),
                                        ),
                                ),
                        ),
                    // Viken (31) – Moss (3103) uten bydeler
                    "31" to
                        Fylke(
                            kode = "31",
                            tekster = nb("Viken"),
                            termer = emptyMap(),
                            undernoder =
                                mapOf(
                                    "3103" to
                                        Kommune(
                                            kode = "3103",
                                            tekster = nb("Moss"),
                                            termer = emptyMap(),
                                            undernoder = null,
                                        ),
                                ),
                        ),
                ),
        )
}
