package no.nav.familie.ks.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.datagenerator.lagPersonTilgangAvvistGrunnetSkjerming
import no.nav.familie.ks.sak.datagenerator.lagPersonTilgangAvvistGrunnetStrengtFortrolig
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.integrasjon.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.tilgangsmaskin.Avvisningskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PersonTilgangServiceTest {
    private val tilgangsmaskinTilgangskontrollKlient = mockk<TilgangsmaskinTilgangskontrollKlient>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val pdlKlient = mockk<PdlKlient>()
    private val personTilgangService =
        PersonTilgangService(tilgangsmaskinTilgangskontrollKlient, integrasjonKlient, featureToggleService, pdlKlient)

    @BeforeEach
    fun setUp() {
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns true
    }

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
        verify(exactly = 0) { integrasjonKlient.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `skal sjekke tilgang mot familie-integrasjoner når Tilgangsmaskinen er skrudd av`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        every { integrasjonKlient.sjekkTilgangTilPersoner(listOf(PERSONIDENT, PERSONIDENT_2)) } returns
            listOf(Tilgang(personIdent = PERSONIDENT, harTilgang = true), Tilgang(personIdent = PERSONIDENT_2, harTilgang = true))

        // Act
        val tilgangPerIdent = personTilgangService.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2))

        // Assert
        assertThat(tilgangPerIdent).containsExactlyInAnyOrderEntriesOf(
            mapOf(PERSONIDENT to PersonTilgang.medTilgang(PERSONIDENT), PERSONIDENT_2 to PersonTilgang.medTilgang(PERSONIDENT_2)),
        )
        verify(exactly = 0) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `skal bruke kilden som sendes inn uavhengig av toggle`() {
        // Arrange
        every { integrasjonKlient.sjekkTilgangTilPersoner(listOf(PERSONIDENT)) } returns
            listOf(Tilgang(personIdent = PERSONIDENT, harTilgang = true))

        // Act
        val tilgangPerIdent = personTilgangService.sjekkTilgangTilPersoner(setOf(PERSONIDENT), skalBrukeTilgangsmaskinen = false)

        // Assert
        assertThat(tilgangPerIdent).containsExactlyEntriesOf(mapOf(PERSONIDENT to PersonTilgang.medTilgang(PERSONIDENT)))
        verify(exactly = 0) { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) }
        verify(exactly = 0) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `skal kaste feil når familie-integrasjoner ikke svarer for alle identer`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        every { integrasjonKlient.sjekkTilgangTilPersoner(listOf(PERSONIDENT, PERSONIDENT_2)) } returns
            listOf(Tilgang(personIdent = PERSONIDENT, harTilgang = true))

        // Act
        val feil = assertThrows<Feil> { personTilgangService.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2)) }

        // Assert
        assertThat(feil.message).isEqualTo("Fikk ikke svar fra familie-integrasjoner for 1 av 2 identer.")
    }

    @Test
    fun `skal avvise med ukjent avvisningskode og begrunnelsen fra familie-integrasjoner når familie-integrasjoner avviser tilgang`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        every { integrasjonKlient.sjekkTilgangTilPersoner(listOf(PERSONIDENT)) } returns
            listOf(Tilgang(personIdent = PERSONIDENT, harTilgang = false, begrunnelse = "Bruker mangler rolle 'TEST_ROLLE'"))

        // Act
        val tilgang = personTilgangService.sjekkTilgangTilPerson(PERSONIDENT)

        // Assert
        assertThat(tilgang).isEqualTo(
            PersonTilgang.avvist(
                personIdent = PERSONIDENT,
                avvisningskode = Avvisningskode.UKJENT,
                begrunnelse = "Bruker mangler rolle 'TEST_ROLLE'",
            ),
        )
    }

    @Test
    fun `skal bruke standard begrunnelse når familie-integrasjoner avviser tilgang uten begrunnelse`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        every { integrasjonKlient.sjekkTilgangTilPersoner(listOf(PERSONIDENT)) } returns
            listOf(Tilgang(personIdent = PERSONIDENT, harTilgang = false, begrunnelse = null))

        // Act
        val tilgang = personTilgangService.sjekkTilgangTilPerson(PERSONIDENT)

        // Assert
        assertThat(tilgang.harTilgang).isFalse
        assertThat(tilgang.avvisning?.avvisningskode).isEqualTo(Avvisningskode.UKJENT)
        assertThat(tilgang.avvisning?.begrunnelse).isEqualTo("familie-integrasjoner ga ikke tilgang til personen")
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
