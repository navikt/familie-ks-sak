package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import org.hamcrest.CoreMatchers.`is` as Is

internal class UtbetalingsoppdragValidatorTest {

    @Test
    fun `valider skal kaste feil dersom nasjonalt utbetalingsoppdrag ikke har utbetalingsperiode`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            utbetalingsoppdrag.valider(
                behandlingsresultat = Behandlingsresultat.INNVILGET,
                behandlingskategori = BehandlingKategori.NASJONAL,
                andelerTilkjentYtelse = listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = inneværendeMåned().minusYears(4),
                        stønadTom = inneværendeMåned(),
                        behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD),
                    ),
                ),
            )
        }

        assertThat(
            funksjonellFeil.message,
            Is("Utbetalingsoppdraget inneholder ingen utbetalingsperioder og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. Kontakt teamet for hjelp."),
        )
    }

    @Test
    fun `valider skal kaste feil dersom innvilget EØS-utbetalingsoppdrag hvor Norge er Primærland mangler utbetalingsperiode`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()
        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            utbetalingsoppdrag.valider(
                behandlingsresultat = Behandlingsresultat.INNVILGET,
                behandlingskategori = BehandlingKategori.EØS,
                andelerTilkjentYtelse = listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = inneværendeMåned().minusYears(4),
                        stønadTom = inneværendeMåned(),
                        behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD),
                    ),
                ),
            )
        }

        assertThat(
            funksjonellFeil.message,
            Is("Utbetalingsoppdraget inneholder ingen utbetalingsperioder og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. Kontakt teamet for hjelp."),
        )
    }

    private fun lagUtbetalingsoppdrag(utbetalingsperioder: List<Utbetalingsperiode> = emptyList()) = Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = FAGSYSTEM,
        saksnummer = "",
        aktoer = UUID.randomUUID().toString(),
        saksbehandlerId = "",
        avstemmingTidspunkt = LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperioder,
    )
}
