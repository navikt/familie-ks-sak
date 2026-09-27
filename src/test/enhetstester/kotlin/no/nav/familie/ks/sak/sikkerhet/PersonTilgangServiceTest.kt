package no.nav.familie.ks.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.datagenerator.lagPersonTilgangAvvistGrunnetSkjerming
import no.nav.familie.ks.sak.datagenerator.lagPersonTilgangAvvistGrunnetStrengtFortrolig
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.integrasjon.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonTilgangServiceTest {
    private val tilgangsmaskinTilgangskontrollKlient = mockk<TilgangsmaskinTilgangskontrollKlient>()
    private val pdlKlient = mockk<PdlKlient>()
    private val personTilgangService = PersonTilgangService(tilgangsmaskinTilgangskontrollKlient, pdlKlient)

    @Test
    fun `skal returnere tilgangen til personen`() {
        // Arrange
        val tilgang = lagPersonTilgangAvvistGrunnetSkjerming(PERSONIDENT)
        every { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT)) } returns listOf(tilgang)

        // Act
        val resultat = personTilgangService.sjekkTilgangTilPerson(PERSONIDENT)

        // Assert
        assertThat(resultat).isEqualTo(tilgang)
    }

    @Test
    fun `skal returnere tilgang per ident`() {
        // Arrange
        val tilganger = listOf(PersonTilgang.medTilgang(PERSONIDENT), lagPersonTilgangAvvistGrunnetSkjerming(PERSONIDENT_2))
        every { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2)) } returns tilganger

        // Act
        val tilgangPerIdent = personTilgangService.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2))

        // Assert
        assertThat(tilgangPerIdent).containsExactlyInAnyOrderEntriesOf(tilganger.associateBy { it.personIdent })
        verify(exactly = 1) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2)) }
    }

    @Test
    fun `skal returnere maskert personinfo når saksbehandler ikke har tilgang til personen`() {
        // Arrange
        val aktør = randomAktør()
        val personIdent = aktør.aktivFødselsnummer()
        every { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(personIdent)) } returns
            listOf(lagPersonTilgangAvvistGrunnetStrengtFortrolig(personIdent))
        every { pdlKlient.hentAdressebeskyttelse(aktør) } returns
            listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND))

        // Act
        val maskertPersonInfo = personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(aktør)

        // Assert
        assertThat(maskertPersonInfo).isNotNull
        assertThat(maskertPersonInfo!!.personIdent).isEqualTo(personIdent)
        assertThat(maskertPersonInfo.harTilgang).isFalse
        assertThat(maskertPersonInfo.adressebeskyttelseGradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND)
        verify(exactly = 1) { pdlKlient.hentAdressebeskyttelse(aktør) }
    }

    @Test
    fun `skal ikke returnere maskert personinfo når saksbehandler har tilgang til personen`() {
        // Arrange
        val aktør = randomAktør()
        val personIdent = aktør.aktivFødselsnummer()
        every { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(personIdent)) } returns
            listOf(PersonTilgang.medTilgang(personIdent))

        // Act
        val maskertPersonInfo = personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(aktør)

        // Assert
        assertThat(maskertPersonInfo).isNull()
        verify(exactly = 0) { pdlKlient.hentAdressebeskyttelse(any()) }
    }

    companion object {
        private const val PERSONIDENT = "12345678910"
        private const val PERSONIDENT_2 = "10987654321"
    }
}
