package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class IntegrasjonServiceTest {

    @MockK
    private lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    private lateinit var pdlClient: PdlClient

    @InjectMockKs
    private lateinit var integrasjonService: IntegrasjonService

    @Test
    fun `hentMaskertPersonInfoVedManglendeTilgang skal returnere maskert personinfo hvis SB ikke har tilgang til aktør`() {
        val aktør = mockk<Aktør>()
        val aktørFnr = "1234567891234"

        every { aktør.aktivFødselsnummer() } returns aktørFnr

        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("1234567891234", false))
        every { pdlClient.hentAdressebeskyttelse(aktør) } returns listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND))

        val maskertPersonInfo = integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)!!

        verify(exactly = 1) { integrasjonClient.sjekkTilgangTilPersoner(listOf(aktørFnr)) }
        verify(exactly = 1) { pdlClient.hentAdressebeskyttelse(aktør) }

        assertThat(maskertPersonInfo.personIdent, Is("1234567891234"))
        assertThat(maskertPersonInfo.harTilgang, Is(false))
        assertThat(
            maskertPersonInfo.adressebeskyttelseGradering,
            Is(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND),
        )
    }

    @Test
    fun `hentMaskertPersonInfoVedManglendeTilgang skal returnere null hvis SB har tilgang til aktør`() {
        val aktør = mockk<Aktør>()
        val aktørFnr = "1234567891234"

        every { aktør.aktivFødselsnummer() } returns aktørFnr

        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("1234567891234", true))

        val maskertPersonInfo = integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)

        verify(exactly = 1) { integrasjonClient.sjekkTilgangTilPersoner(listOf(aktørFnr)) }

        assertThat(maskertPersonInfo, Is(nullValue()))
    }

    @Test
    fun `hentJournalpost skal returnere journalpost fra familie-integrasjoner`() {
        val mocketJournalPost = mockk<Journalpost>()

        every { integrasjonClient.hentJournalpost("test") } returns mocketJournalPost

        val hentetJournalPost = integrasjonService.hentJournalpost("test")

        verify(exactly = 1) { integrasjonClient.hentJournalpost("test") }

        assertThat(hentetJournalPost, Is(mocketJournalPost))
    }

    @Test
    fun `sjekkTilgangTilPersoner skal sjekke om SB har tilgang til personidenter`() {
        val listeMedIdenter = listOf("Ident1", "Ident2")

        every { integrasjonClient.sjekkTilgangTilPersoner(listeMedIdenter) } returns listOf(
            Tilgang(
                "Ident1",
                true,
                "test",
            ),
            Tilgang("Ident2", true, "test"),
        )

        val tilgang = integrasjonService.sjekkTilgangTilPersoner(listeMedIdenter)

        verify(exactly = 1) { integrasjonClient.sjekkTilgangTilPersoner(listeMedIdenter) }

        assertThat(tilgang.all { it.value.harTilgang }, Is(true))
        assertThat(tilgang.all { it.value.begrunnelse == "test" }, Is(true))
    }
}
