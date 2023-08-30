package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagDødsfall
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.dødeBarnForrigePeriode
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class BegrunnelseUtilsKtTest {

    @Test
    fun `dødeBarnForrigePeriode() skal returnere barn som døde i forrige periode og som er tilknyttet ytelsen`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val barn1Fnr = "12345678910"
        val barn2Fnr = "12345678911"

        // Barn1 dør før Barn2.
        val dødsfallDatoBarn1 = LocalDate.of(2022, 5, 12)
        val dødsfallDatoBarn2 = LocalDate.of(2022, 7, 2)

        val barn1 = lagPerson(
            aktør = randomAktør(barn1Fnr),
            personType = PersonType.BARN,
            fødselsdato = LocalDate.now().minusYears(2),
        ).let { it.copy(dødsfall = lagDødsfall(dødsfallDato = dødsfallDatoBarn1, person = it)) }

        val barn2 = lagPerson(
            aktør = randomAktør(barn2Fnr),
            personType = PersonType.BARN,
            fødselsdato = LocalDate.now().minusYears(2),
        ).let { it.copy(dødsfall = lagDødsfall(dødsfallDato = dødsfallDatoBarn2, person = it)) }

        val barnIBehandling = listOf(
            barn1,
            barn2,
        )

        var ytelserForrigePeriode =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.of(
                        dødsfallDatoBarn1.minusMonths(1).year,
                        dødsfallDatoBarn1.minusMonths(1).month,
                    ),
                    stønadTom = YearMonth.of(dødsfallDatoBarn1.year, dødsfallDatoBarn1.month),
                    aktør = barn1.aktør,
                ),
            )

        var dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(1, dødeBarnForrigePeriode.size)
        assertEquals(barn1, dødeBarnForrigePeriode[0])

        ytelserForrigePeriode =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.of(
                        dødsfallDatoBarn1.minusMonths(1).year,
                        dødsfallDatoBarn1.minusMonths(1).month,
                    ),
                    stønadTom = YearMonth.of(dødsfallDatoBarn2.year, dødsfallDatoBarn2.month),
                    aktør = barn2.aktør,
                ),
            )

        dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(1, dødeBarnForrigePeriode.size)
        assertEquals(barn2, dødeBarnForrigePeriode[0])
    }
}
