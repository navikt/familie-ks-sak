package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.RegelverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.regelverkLovendringFebruar2025.RegelverkLovendringFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.regelverk.RegelverkUtleder
import no.nav.familie.unleash.UnleashService
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregnAndelTilkjentYtelseServiceTest {
    private val regelverkLovendringFebruar2025AndelGenerator: RegelverkLovendringFebruar2025AndelGenerator = mockk()
    private val regelverkFørFebruar2025AndelGenerator: RegelverkFørFebruar2025AndelGenerator = mockk()
    private val unleashService: UnleashService = mockk()
    private val beregnAndelTilkjentYtelseService =
        BeregnAndelTilkjentYtelseService(
            regelverkLovendringFebruar2025AndelGenerator = regelverkLovendringFebruar2025AndelGenerator,
            regelverkFørFebruar2025AndelGenerator = regelverkFørFebruar2025AndelGenerator,
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
        verify(exactly = 0) { regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
        verify(exactly = 0) { regelverkLovendringFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
    }

    @Test
    fun `skal bruke RegelverkFørFebruar2025AndelGenerator på barn født før 01-01-2024 dersom toggle er skrudd på`() {
        // Arrange
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()

        every { personopplysningGrunnlag.søker } returns lagPerson(aktør = randomAktør())
        every { personopplysningGrunnlag.barna } returns listOf(lagPerson(aktør = randomAktør(), fødselsdato = LocalDate.of(2023, 12, 31)))

        every { unleashService.isEnabled(FeatureToggleConfig.BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER, false) } returns true
        every { regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) } returns emptyList()

        // Act
        beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, mockk<Vilkårsvurdering>(), mockk<TilkjentYtelse>())

        // Assert
        verify(exactly = 1) { regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
        verify(exactly = 0) { regelverkLovendringFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
    }

    @Test
    fun `skal bruke RegelverkLovendringFebruar2025AndelGenerator på barn født 01-01-2024 eller senere dersom toggle er skrudd på`() {
        // Arrange
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()

        every { personopplysningGrunnlag.søker } returns lagPerson(aktør = randomAktør())
        every { personopplysningGrunnlag.barna } returns listOf(lagPerson(aktør = randomAktør(), fødselsdato = RegelverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025))

        every { unleashService.isEnabled(FeatureToggleConfig.BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER, false) } returns true
        every { regelverkLovendringFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) } returns emptyList()

        // Act
        beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, mockk<Vilkårsvurdering>(), mockk<TilkjentYtelse>())

        // Assert
        verify(exactly = 0) { regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
        verify(exactly = 1) { regelverkLovendringFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
    }

    @Test
    fun `skal bruke andel generator som passer alderen til de ulike barna dersom toggle er skrudd på`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør())
        val barnFødtFørLovendring2025 = lagPerson(aktør = randomAktør(), fødselsdato = LocalDate.of(2023, 12, 31))
        val barnFødtEtterLovendring2025 = lagPerson(aktør = randomAktør(), fødselsdato = RegelverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025)
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()

        every { personopplysningGrunnlag.søker } returns søker
        every { personopplysningGrunnlag.barna } returns listOf(barnFødtFørLovendring2025, barnFødtEtterLovendring2025)

        every { unleashService.isEnabled(FeatureToggleConfig.BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER, false) } returns true
        every { regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) } returns emptyList()
        every { regelverkLovendringFebruar2025AndelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) } returns emptyList()

        // Act
        beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, mockk<Vilkårsvurdering>(), mockk<TilkjentYtelse>())

        // Assert
        verify(exactly = 1) { regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(søker, barnFødtFørLovendring2025, any(), any()) }
        verify(exactly = 1) { regelverkLovendringFebruar2025AndelGenerator.beregnAndelerForBarn(søker, barnFødtEtterLovendring2025, any(), any()) }
    }
}
