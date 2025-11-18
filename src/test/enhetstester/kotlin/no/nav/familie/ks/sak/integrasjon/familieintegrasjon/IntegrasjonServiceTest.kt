package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.data.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ks.sak.data.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

internal class IntegrasjonServiceTest {
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val pdlKlient = mockk<PdlKlient>()
    private val integrasjonService = IntegrasjonService(integrasjonKlient, pdlKlient)

    @Test
    fun `hentMaskertPersonInfoVedManglendeTilgang skal returnere maskert personinfo hvis SB ikke har tilgang til aktør`() {
        val aktør = mockk<Aktør>()
        val aktørFnr = "1234567891234"

        every { aktør.aktivFødselsnummer() } returns aktørFnr

        every { integrasjonKlient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("1234567891234", false))
        every { pdlKlient.hentAdressebeskyttelse(aktør) } returns listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND))

        mockBrukerContext()

        val maskertPersonInfo = integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)!!

        verify(exactly = 1) { integrasjonKlient.sjekkTilgangTilPersoner(listOf(aktørFnr)) }
        verify(exactly = 1) { pdlKlient.hentAdressebeskyttelse(aktør) }

        assertThat(maskertPersonInfo.personIdent, Is("1234567891234"))
        assertThat(maskertPersonInfo.harTilgang, Is(false))
        assertThat(
            maskertPersonInfo.adressebeskyttelseGradering,
            Is(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND),
        )
        clearBrukerContext()
    }

    @Test
    fun `hentMaskertPersonInfoVedManglendeTilgang skal returnere null hvis SB har tilgang til aktør`() {
        val aktør = mockk<Aktør>()
        val aktørFnr = "1234567891234"

        every { aktør.aktivFødselsnummer() } returns aktørFnr

        every { integrasjonKlient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("1234567891234", true))

        mockBrukerContext()

        val maskertPersonInfo = integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)

        verify(exactly = 1) { integrasjonKlient.sjekkTilgangTilPersoner(listOf(aktørFnr)) }

        assertThat(maskertPersonInfo, Is(nullValue()))

        clearBrukerContext()
    }

    @Test
    fun `hentJournalpost skal returnere journalpost fra familie-integrasjoner`() {
        val mocketJournalPost = mockk<Journalpost>()

        every { integrasjonKlient.hentJournalpost("test") } returns mocketJournalPost

        val hentetJournalPost = integrasjonService.hentJournalpost("test")

        verify(exactly = 1) { integrasjonKlient.hentJournalpost("test") }

        assertThat(hentetJournalPost, Is(mocketJournalPost))
    }

    @Test
    fun `sjekkTilgangTilPersoner skal sjekke om SB har tilgang til personidenter`() {
        val listeMedIdenter = listOf("Ident1", "Ident2")

        every { integrasjonKlient.sjekkTilgangTilPersoner(listeMedIdenter) } returns
            listOf(
                Tilgang(
                    "Ident1",
                    true,
                    "test",
                ),
                Tilgang("Ident2", true, "test"),
            )

        mockBrukerContext()

        val tilgang = integrasjonService.sjekkTilgangTilPersoner(listeMedIdenter)

        verify(exactly = 1) { integrasjonKlient.sjekkTilgangTilPersoner(listeMedIdenter) }

        assertThat(tilgang.all { it.harTilgang }, Is(true))
        assertThat(tilgang.all { it.begrunnelse == "test" }, Is(true))

        clearBrukerContext()
    }

    @Test
    fun `sjekkTilgangTilPersoner skal gi tilgang om SB er systembruker`() {
        val listeMedIdenter = listOf("Ident1", "Ident2")

        mockBrukerContext("VL")

        val tilgang = integrasjonService.sjekkTilgangTilPersoner(listeMedIdenter)

        verify(exactly = 0) { integrasjonKlient.sjekkTilgangTilPersoner(listeMedIdenter) }

        assertThat(tilgang.all { it.harTilgang }, Is(true))

        clearBrukerContext()
    }
}
