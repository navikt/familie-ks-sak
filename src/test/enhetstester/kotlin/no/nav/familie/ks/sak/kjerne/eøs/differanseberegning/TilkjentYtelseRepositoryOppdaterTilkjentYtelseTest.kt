package no.nav.familie.ks.sak.kjerne.eøs.differanseberegning

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.oppdaterTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.eøs.util.TilkjentYtelseBuilder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseRepositoryOppdaterTilkjentYtelseTest {
    fun Int.jan(år: Int) = LocalDate.of(år, 1, this)

    val barnsFødselsdato = 13.jan(2020)
    val startMåned = barnsFødselsdato.toYearMonth()

    val søker = tilfeldigPerson(personType = PersonType.SØKER)
    val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

    private val tilkjentYtelseRepository: TilkjentYtelseRepository = spyk()

    @Test
    fun `skal ikke kaste exception hvis tilkjent ytelse oppdateres med gyldige andeler`() {
        val behandling = lagBehandling()

        val forrigeTilkjentYtelse =
            TilkjentYtelseBuilder(startMåned, behandling)
                .bygg()

        val nyTilkjentYtelse =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(søker)
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$")
                .bygg()

        every { tilkjentYtelseRepository.saveAndFlush(any()) } returns nyTilkjentYtelse

        tilkjentYtelseRepository.oppdaterTilkjentYtelse(
            forrigeTilkjentYtelse,
            nyTilkjentYtelse.andelerTilkjentYtelse,
        )

        verify(exactly = 1) { tilkjentYtelseRepository.saveAndFlush(any()) }
    }
}
