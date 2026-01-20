package no.nav.familie.ks.sak.kjerne.falskidentitet

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlFalskIdentitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FalskIdentitetServiceTest {
    private val personRepository = mockk<PersonRepository>()
    private val pdlRestKlient = mockk<PdlKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val falskIdentitetService =
        FalskIdentitetService(
            personRepository = personRepository,
            pdlRestKlient = pdlRestKlient,
            featureToggleService = featureToggleService,
        )

    @Nested
    inner class HentFalskIdentitet {
        @Test
        fun `skal hente og returnere FalskIdentitet dersom aktør er falsk identitet med tidligere behandling`() {
            // Arrange
            val fødselsdato = LocalDate.of(1990, 1, 1)
            val person =
                lagPerson(
                    navn = "Falsk Falskesen",
                    kjønn = Kjønn.MANN,
                    fødselsdato = fødselsdato,
                )

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_HÅNDTERE_FALSK_IDENTITET) } returns true
            every { pdlRestKlient.hentFalskIdentitet(person.aktør.aktivFødselsnummer()) } returns PdlFalskIdentitet(erFalsk = true)
            every { personRepository.findByAktør(person.aktør) } returns listOf(person)

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(person.aktør)

            // Assert
            assertThat(falskIdentitet).isNotNull
            assertThat(falskIdentitet!!.navn).isEqualTo(person.navn)
            assertThat(falskIdentitet.fødselsdato).isEqualTo(person.fødselsdato)
            assertThat(falskIdentitet.kjønn).isEqualTo(person.kjønn)
        }

        @Test
        fun `skal hente og returnere FalskIdentitet dersom aktør er falsk identitet uten tidligere behandling`() {
            // Arrange
            val aktør = randomAktør()

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_HÅNDTERE_FALSK_IDENTITET) } returns true
            every { pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer()) } returns PdlFalskIdentitet(erFalsk = true)
            every { personRepository.findByAktør(aktør) } returns emptyList()

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)

            // Assert
            assertThat(falskIdentitet).isNotNull
            assertThat(falskIdentitet!!.navn).isEqualTo("Ukjent navn")
            assertThat(falskIdentitet.fødselsdato).isNull()
            assertThat(falskIdentitet.kjønn).isEqualTo(Kjønn.UKJENT)
        }

        @Test
        fun `skal returnere null dersom pdl ikke returnerer noen falsk identitet for aktør`() {
            // Arrange
            val aktør = randomAktør()

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_HÅNDTERE_FALSK_IDENTITET) } returns true
            every { pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer()) } returns null

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)

            // Assert
            assertThat(falskIdentitet).isNull()
        }

        @Test
        fun `skal returnere null dersom pdl returnerer falsk identitet for aktør som ikke er falsk`() {
            // Arrange
            val aktør = randomAktør()

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_HÅNDTERE_FALSK_IDENTITET) } returns true
            every { pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer()) } returns PdlFalskIdentitet(erFalsk = false)

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)

            // Assert
            assertThat(falskIdentitet).isNull()
        }
    }
}
