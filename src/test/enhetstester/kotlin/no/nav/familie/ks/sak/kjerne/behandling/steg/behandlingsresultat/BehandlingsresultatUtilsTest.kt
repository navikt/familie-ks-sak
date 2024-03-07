package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

internal class BehandlingsresultatUtilsTest {
    val søker = tilfeldigPerson()

    @Test
    fun `validerUtledetBehandlingsresultat skal kaste feil når førtegangsbehandling får behandlingsresultat ENDRET_UTBETALING`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, type = BehandlingType.FØRSTEGANGSBEHANDLING)
        val exception =
            assertThrows<FunksjonellFeil> {
                BehandlingsresultatUtils.validerUtledetBehandlingsresultat(behandling, Behandlingsresultat.ENDRET_UTBETALING)
            }
        val feilmelding =
            "Behandlingsresultatet ${Behandlingsresultat.ENDRET_UTBETALING.displayName.lowercase()} " +
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
        val exception =
            assertThrows<FunksjonellFeil> {
                BehandlingsresultatUtils.validerUtledetBehandlingsresultat(behandling, Behandlingsresultat.AVSLÅTT)
            }
        val feilmelding =
            "Behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn.lowercase()} " +
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
            val ytelsePersonerMedResulater =
                YtelsePersonUtils.utledYtelsePersonerMedResultat(
                    behandlingsresultatPersoner = testData.personer,
                    uregistrerteBarn = testData.uregistrerteBarn,
                )
            val ytelsePersonResultater =
                YtelsePersonUtils.oppdaterYtelsePersonResultaterVedOpphør(ytelsePersonerMedResulater)
            val behandlingsresultat =
                BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersonResulater(
                    ytelsePersonResultater,
                    false,
                )
            assertEquals(forventetResultat, behandlingsresultat)
        }
    }

    @ParameterizedTest(name = "Søknadsresultat {0}, Endringsresultat {1} og Opphørsresultat {2} skal kombineres til behandlingsresultat {3}")
    @MethodSource("hentKombinasjonerOgBehandlingsResultat")
    internal fun `Kombiner resultater - skal kombinere til riktig behandlingsresultat gitt forskjellige kombinasjoner av resultater`(
        søknadsresultat: Søknadsresultat?,
        endringsresultat: Endringsresultat,
        opphørsresultat: Opphørsresultat,
        behandlingsresultat: Behandlingsresultat,
    ) {
        val kombinertResultat =
            BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(
                søknadsresultat,
                endringsresultat,
                opphørsresultat,
            )

        assertEquals(kombinertResultat, behandlingsresultat)
    }

    @ParameterizedTest(name = "Søknadsresultat {0}, Endringsresultat {1} og Opphørsresultat {2} skal kaste feil")
    @MethodSource("hentUgyldigeKombinasjoner")
    internal fun `Kombiner resultater - skal kaste feil ved ugyldige kombinasjoner av resultat`(
        søknadsresultat: Søknadsresultat?,
        endringsresultat: Endringsresultat,
        opphørsresultat: Opphørsresultat,
    ) {
        assertThrows<FunksjonellFeil> {
            BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(
                søknadsresultat,
                endringsresultat,
                opphørsresultat,
            )
        }
    }

    companion object {
        @JvmStatic
        fun hentKombinasjonerOgBehandlingsResultat() =
            Stream.of(
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.FORTSATT_INNVILGET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.INNVILGET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.INNVILGET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.INNVILGET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_ENDRET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_ENDRET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.AVSLÅTT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.AVSLÅTT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.ENDRET_OG_OPPHØRT),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.ENDRET_UTBETALING),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.ENDRET_UTBETALING),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.OPPHØRT),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.FORTSATT_OPPHØRT),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.FORTSATT_INNVILGET),
            )

        @JvmStatic
        fun hentUgyldigeKombinasjoner() =
            Stream.of(
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT),
            )
    }
}

data class BehandlingsresulatTestData(
    val beskrivelse: String,
    val kommentar: String? = "",
    val personer: List<BehandlingsresultatPerson>,
    val uregistrerteBarn: List<String> = emptyList(),
    val forventetResultat: Behandlingsresultat,
)
