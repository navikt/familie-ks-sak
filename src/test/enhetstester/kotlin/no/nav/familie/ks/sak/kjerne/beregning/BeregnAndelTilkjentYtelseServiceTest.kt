package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.unleash.UnleashService
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregnAndelTilkjentYtelseServiceTest {
    private val unleashService: UnleashService = mockk()
    private val andelGeneratorFactory: AndelGeneratorFactory = mockk()
    private val beregnAndelTilkjentYtelseService =
        BeregnAndelTilkjentYtelseService(
            andelGeneratorFactory = andelGeneratorFactory,
            unleashService = unleashService,
        )

    @Test
    fun `skal bruke utdatert funksjon for generering av andeler dersom toggle er skrudd av`() {
        // Arrange
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()
        val vilkårsvurdering = mockk<Vilkårsvurdering>()
        val tilkjentYtelse = mockk<TilkjentYtelse>()

        every { vilkårsvurdering.personResultater } returns emptySet()
        every { personopplysningGrunnlag.søker } returns lagPerson(aktør = randomAktør())
        every { personopplysningGrunnlag.barna } returns listOf(lagPerson(aktør = randomAktør()))
        every { unleashService.isEnabled(FeatureToggleConfig.BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER, false) } returns false

        // Act
        beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, vilkårsvurdering, tilkjentYtelse)

        // Assert
        verify(exactly = 0) { andelGeneratorFactory.hentGeneratorForRegelverk(any()) }
    }

    @Test
    fun `skal hente andel generator og beregne andeler dersom toggle er skrudd på`() {
        // Arrange
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()
        val andelGenerator = mockk<AndelGenerator>()

        every { personopplysningGrunnlag.søker } returns lagPerson(aktør = randomAktør())
        every { personopplysningGrunnlag.barna } returns listOf(lagPerson(aktør = randomAktør(), fødselsdato = LocalDate.of(2023, 12, 31)))

        every { unleashService.isEnabled(FeatureToggleConfig.BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER, false) } returns true
        every { andelGeneratorFactory.hentGeneratorForRegelverk(any()) } returns andelGenerator
        every { andelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) } returns emptyList()

        // Act
        beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, mockk<Vilkårsvurdering>(), mockk<TilkjentYtelse>())

        // Assert
        verify(exactly = 1) { andelGeneratorFactory.hentGeneratorForRegelverk(any()) }
        verify(exactly = 1) { andelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
    }
}
