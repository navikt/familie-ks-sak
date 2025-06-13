package no.nav.familie.ks.sak.kjerne.beregning.domene

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.YearMonth

class EndretUtbetalingAndelTest {
    val søker = randomAktør()
    private val barn1 = randomAktør()

    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer()),
        )
    val person = lagPerson(personopplysningGrunnlag, søker, PersonType.SØKER)

    @Test
    fun `validerUtfyltEndring skal ikke kaste feil når EndretUtbetalingAndel er riktig fylt ut`() {
        assertDoesNotThrow {
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(person),
                prosent = BigDecimal(50),
            ).validerUtfyltEndring()
        }
    }

    @Test
    fun `validerUtfyltEndring skal kaste feil når EndretUtbetalingAndel ikke har prosent fylt ut`() {
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(person),
            )
        val exception =
            assertThrows<RuntimeException> {
                endretUtbetalingAndel.validerUtfyltEndring()
            }
        assertEquals(
            "Person, prosent, fom, tom, årsak, begrunnese og søknadstidspunkt skal være utfylt: $endretUtbetalingAndel",
            exception.message,
        )
    }

    @Test
    fun `validerUtfyltEndring skal kaste feil når EndretUtbetalingAndel har feil fom, tom`() {
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(person),
                prosent = BigDecimal(50),
                periodeFom = YearMonth.now(),
                periodeTom = YearMonth.now().minusYears(1),
            )
        val exception =
            assertThrows<FunksjonellFeil> {
                endretUtbetalingAndel.validerUtfyltEndring()
            }
        assertEquals("fom må være lik eller komme før tom", exception.message)
        assertEquals("Du kan ikke sette en f.o.m. dato som er etter t.o.m. dato", exception.frontendFeilmelding)
    }
}
