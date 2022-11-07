package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class StandardbegrunnelseUtilsKtTest {

    @Test
    fun `dødeBarnForrigePeriode() skal returnere barn som døde i forrige periode og som er tilknyttet ytelsen`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val barn1Fnr = "12345678910"
        val barn2Fnr = "12345678911"

        // Barn1 dør før Barn2.
        val dødsfallDatoBarn1 = LocalDate.of(2022, 5, 12)
        val dødsfallDatoBarn2 = LocalDate.of(2022, 7, 2)

        val barnIBehandling = listOf(
            BrevPerson(
                dødsfallsdato = dødsfallDatoBarn1,
                aktivPersonIdent = barn1Fnr,
                type = PersonType.BARN,
                aktørId = barn1Fnr,
                fødselsdato = LocalDate.now().minusYears(2)
            ),
            BrevPerson(
                dødsfallsdato = dødsfallDatoBarn2,
                aktivPersonIdent = barn2Fnr,
                type = PersonType.BARN,
                aktørId = barn2Fnr,
                fødselsdato = LocalDate.now().minusYears(2)
            )
        )

        var ytelserForrigePeriode =
            listOf(
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = YearMonth.of(
                            dødsfallDatoBarn1.minusMonths(1).year, dødsfallDatoBarn1.minusMonths(1).month
                        ),
                        stønadTom = YearMonth.of(dødsfallDatoBarn1.year, dødsfallDatoBarn1.month),
                        aktør = Aktør(barn1Fnr + "00").also { it.personidenter.add(Personident(barn1Fnr, it)) }
                    ),
                    emptyList()
                )
            )

        var dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(1, dødeBarnForrigePeriode.size)
        assertEquals(barn1Fnr, dødeBarnForrigePeriode[0])

        ytelserForrigePeriode =
            listOf(
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = YearMonth.of(
                            dødsfallDatoBarn1.minusMonths(1).year, dødsfallDatoBarn1.minusMonths(1).month
                        ),
                        stønadTom = YearMonth.of(dødsfallDatoBarn2.year, dødsfallDatoBarn2.month),
                        aktør = Aktør(barn2Fnr + "00").also { it.personidenter.add(Personident(barn2Fnr, it)) }
                    ),
                    emptyList()
                )
            )

        dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(1, dødeBarnForrigePeriode.size)
        assertEquals(barn2Fnr, dødeBarnForrigePeriode[0])
    }
}
