package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.hamcrest.CoreMatchers.`is` as Is

internal class BrevUtilKtTest {

    @Test
    fun `hentBrevmal skal returnere VEDTAK_OPPHØR_DØDSFALL dersom opprettetÅrsak er DØDSFALL`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.DØDSFALL)
        val brevmal = hentBrevmal(behandling)

        assertThat(brevmal, Is(Brevmal.VEDTAK_OPPHØR_DØDSFALL))
    }

    @Test
    fun `hentBrevmal skal returnere VEDTAK_KORREKSJON_VEDTAKSBREV dersom opprettetÅrsak er KORREKSJON_VEDTAKSBREV`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV)
        val brevmal = hentBrevmal(behandling)

        assertThat(brevmal, Is(Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV))
    }

    @Test
    fun `hentVedtaksbrevmal skal kaste feil dersom behandlingresultatet ikke er vurdert enda`() {
        val behandling =
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).apply { resultat = Behandlingsresultat.IKKE_VURDERT }

        val feil = assertThrows<Feil> { hentVedtaksbrevmal(behandling) }

        assertThat(feil.message, Is("Kan ikke opprette brev. Behandlingen er ikke vurdert."))
    }

    @Test
    fun `hentVedtaksbrevtype skal kaste feil dersom brev ikke er støttet for kombinasjonen av behandlingstype og behandlingsresultat`() {
        val feil = assertThrows<FunksjonellFeil> {
            hentVedtaksbrevtype(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT
            )
        }

        assertThat(
            feil.message,
            Is("Brev ikke støttet for behandlingstype=FØRSTEGANGSBEHANDLING og behandlingsresultat=INNVILGET_ENDRET_OG_OPPHØRT")
        )
    }

    @Test
    fun `hentVedtaksbrevtype skal returnere førstegangsvedtak mal for førstegangsbehandlinger med innvilget resultat`() {
        val brevmal = hentVedtaksbrevtype(BehandlingType.FØRSTEGANGSBEHANDLING, Behandlingsresultat.INNVILGET)

        assertThat(
            brevmal,
            Is(Brevmal.VEDTAK_FØRSTEGANGSVEDTAK)
        )
    }
}
