package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.File

internal class BehandlingsresultatUtilsTest {

    @Test
    fun `validerUtledetBehandlingsresultat skal kaste feil når førtegangsbehandling får behandlingsresultat ENDRET_UTBETALING`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, type = BehandlingType.FØRSTEGANGSBEHANDLING)
        val exception = assertThrows<FunksjonellFeil> {
            BehandlingsresultatUtils.validerUtledetBehandlingsresultat(behandling, Behandlingsresultat.ENDRET_UTBETALING)
        }
        val feilmelding = "Behandlingsresultatet ${Behandlingsresultat.ENDRET_UTBETALING.displayName.lowercase()} " +
            "er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'."
        assertEquals(feilmelding, exception.message)
        assertEquals(feilmelding, exception.frontendFeilmelding)
    }

    @Test
    fun `validerUtledetBehandlingsresultat skal ikke kaste feil når revurdering får behandlingsresultat ENDRET_UTBETALING`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, type = BehandlingType.REVURDERING)
        assertDoesNotThrow {
            BehandlingsresultatUtils.validerUtledetBehandlingsresultat(behandling, Behandlingsresultat.ENDRET_UTBETALING)
        }
    }

    @Test
    fun `validerUtledetBehandlingsresultat skal kaste feil når klage får behandlingsresultat AVSLÅTT`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.KLAGE, type = BehandlingType.FØRSTEGANGSBEHANDLING)
        val exception = assertThrows<FunksjonellFeil> {
            BehandlingsresultatUtils.validerUtledetBehandlingsresultat(behandling, Behandlingsresultat.AVSLÅTT)
        }
        val feilmelding = "Behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn.lowercase()} " +
            "er ugyldig i kombinasjon med resultat '${Behandlingsresultat.AVSLÅTT.displayName.lowercase()}'."
        assertEquals(feilmelding, exception.message)
        assertEquals(feilmelding, exception.frontendFeilmelding)
    }

    @Test
    fun `utledBehandlingsresultatBasertPåYtelsePersonResulater skal utlede behandlingsresultat og sjekke at det matcher forventet resulat`() {
        val testmappe = File("./src/test/resources/behandlingsresultat/")
        testmappe.listFiles()?.forEach { fil ->
            val testData = objectMapper.readValue(fil.readText(), BehandlingsresulatTestData::class.java)
            val forventetResultat = testData.forventetResultat
            println("Tester: ${testData.beskrivelse}, forventet resultat: $forventetResultat")
            val ytelsePersonerMedResulater = YtelsePersonUtils.utledYtelsePersonerMedResultat(
                behandlingsresultatPersoner = testData.personer,
                uregistrerteBarn = testData.uregistrerteBarn
            )
            val ytelsePersonResultater =
                YtelsePersonUtils.oppdaterYtelsePersonResultaterVedOpphør(ytelsePersonerMedResulater)
            val behandlingsresultat =
                BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersonResulater(
                    ytelsePersonResultater,
                    false
                )
            assertEquals(forventetResultat, behandlingsresultat)
        }
    }
}

data class BehandlingsresulatTestData(
    val beskrivelse: String,
    val kommentar: String? = "",
    val personer: List<BehandlingsresultatPerson>,
    val uregistrerteBarn: List<String> = emptyList(),
    val forventetResultat: Behandlingsresultat
)
