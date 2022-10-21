package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

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
}
