package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregnAndelTilkjentYtelseServiceTest {
    private val andelGeneratorLookup: AndelGenerator.Lookup = mockk()
    private val adopsjonService: AdopsjonService = mockk()
    private val beregnAndelTilkjentYtelseService =
        BeregnAndelTilkjentYtelseService(
            andelGeneratorLookup = andelGeneratorLookup,
            adopsjonService = adopsjonService,
        )

    @BeforeEach
    fun setup() {
        every { adopsjonService.finnAdopsjonForAktørIBehandling(any(), any()) } returns null
    }

    @Test
    fun `skal hente andel generator og beregne andeler dersom toggle er skrudd på`() {
        // Arrange
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()
        val andelGenerator = mockk<AndelGenerator>()
        val vilkårsvurdering = mockk<Vilkårsvurdering>()

        every { personopplysningGrunnlag.søker } returns lagPerson(aktør = randomAktør())
        every { personopplysningGrunnlag.barna } returns listOf(lagPerson(aktør = randomAktør(), fødselsdato = LocalDate.of(2023, 12, 31)))

        every { andelGeneratorLookup.hentGeneratorForLovverk(any()) } returns andelGenerator
        every { andelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) } returns emptyList()

        every { vilkårsvurdering.behandling.behandlingId } returns BehandlingId(1)

        // Act
        beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, vilkårsvurdering, mockk<TilkjentYtelse>())

        // Assert
        verify(exactly = 1) { andelGeneratorLookup.hentGeneratorForLovverk(any()) }
        verify(exactly = 1) { andelGenerator.beregnAndelerForBarn(any(), any(), any(), any()) }
    }
}
