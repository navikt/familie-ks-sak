package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_ENDRET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTBETALING
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_ENDRET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak

object BehandlingsresultatUtils {
    internal fun skalUtledeSøknadsresultatForBehandling(behandling: Behandling): Boolean = behandling.opprettetÅrsak in listOf(BehandlingÅrsak.SØKNAD, BehandlingÅrsak.KLAGE)

    internal fun kombinerResultaterTilBehandlingsresultat(
        // Søknadsresultat er null hvis det ikke er en søknad/fødselshendelse/manuell migrering
        søknadsresultat: Søknadsresultat?,
        endringsresultat: Endringsresultat,
        opphørsresultat: Opphørsresultat,
    ): Behandlingsresultat {
        fun sjekkResultat(
            ønsketSøknadsresultat: Søknadsresultat?,
            ønsketEndringsresultat: Endringsresultat,
            ønsketOpphørsresultat: Opphørsresultat,
        ): Boolean = søknadsresultat == ønsketSøknadsresultat && endringsresultat == ønsketEndringsresultat && opphørsresultat == ønsketOpphørsresultat

        fun ugyldigBehandlingsresultatFeil(behandlingsresultatString: String) =
            FunksjonellFeil(
                frontendFeilmelding = "Du har fått behandlingsresultatet $behandlingsresultatString, men behandlingen er registrert med årsak søknad. Du må enten innvilge eller avslå noe for å kunne fortsette. Om du er uenig i resultatet ta kontakt med Superbruker.",
                melding = "Kombinasjonen av (søknadsresultat=$søknadsresultat, endringsresultat=$endringsresultat, opphørsresultat=$opphørsresultat) er ikke støttet i løsningen.",
            )

        return when {
            // Søknad/fødselshendelse/manuell migrering
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> throw ugyldigBehandlingsresultatFeil("Endret og opphørt")
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> throw ugyldigBehandlingsresultatFeil("Endret og fortsatt opphørt")
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> throw ugyldigBehandlingsresultatFeil("Opphørt")
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> FORTSATT_OPPHØRT
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> FORTSATT_INNVILGET

            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> INNVILGET_ENDRET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> INNVILGET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> INNVILGET
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> INNVILGET

            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> AVSLÅTT_ENDRET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> AVSLÅTT_OG_ENDRET
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> AVSLÅTT_OG_ENDRET
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> AVSLÅTT_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> AVSLÅTT
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> AVSLÅTT

            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> DELVIS_INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> DELVIS_INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> DELVIS_INNVILGET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> DELVIS_INNVILGET
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> DELVIS_INNVILGET

            // Ikke søknad/fødselshendelse/manuell migrering
            sjekkResultat(null, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> ENDRET_OG_OPPHØRT
            sjekkResultat(null, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> ENDRET_UTBETALING
            sjekkResultat(null, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> ENDRET_UTBETALING
            sjekkResultat(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> OPPHØRT
            sjekkResultat(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> FORTSATT_OPPHØRT
            sjekkResultat(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> FORTSATT_INNVILGET

            // Skal egentlig aldri kunne komme hit, alle kombinasjoner skal være skrevet ut
            else -> throw Feil(
                frontendFeilmelding = "Du har fått et behandlingsresultat vi ikke støtter. Meld sak i Porten om du er uenig i resultatet.",
                message = "Klarer ikke utlede behandlingsresultat fra (søknadsresultat=$søknadsresultat, endringsresultat=$endringsresultat, opphørsresultat=$opphørsresultat)",
            )
        }
    }
}
