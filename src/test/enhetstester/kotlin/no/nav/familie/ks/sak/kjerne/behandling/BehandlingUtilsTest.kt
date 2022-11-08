package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class BehandlingUtilsTest {

    @Test
    fun `hentSisteBehandlingSomErIverksatt - skal returnere avsluttet behandling med høyest opprettetTidspunkt`() {
        val avsluttetBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).copy(id = 1)
        avsluttetBehandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = avsluttetBehandling,
                behandlingSteg = BehandlingSteg.BEHANDLING_AVSLUTTET
            )
        )
        val avsluttetBehandling2 = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).copy(id = 2)
        avsluttetBehandling2.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = avsluttetBehandling2,
                behandlingSteg = BehandlingSteg.BEHANDLING_AVSLUTTET
            )
        )
        val sisteIverksatteBehandling =
            BehandlingUtils.hentSisteBehandlingSomErIverksatt(listOf(avsluttetBehandling, avsluttetBehandling2))
        assertEquals(2, sisteIverksatteBehandling?.id)
    }

    @Test
    fun `hentSisteBehandlingSomErIverksatt - skal returnere null dersom det ikke finnes noen iverksatte behandlinger`() {
        val sisteIverksatteBehandling =
            BehandlingUtils.hentSisteBehandlingSomErIverksatt(emptyList())
        assertNull(sisteIverksatteBehandling)
    }

    @Test
    fun `hentForrigeIverksatteBehandling - skal hente den siste iverksatte behandlingen før inneværende behandling`() {
        val avsluttetBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).copy(id = 1)
        avsluttetBehandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = avsluttetBehandling,
                behandlingSteg = BehandlingSteg.BEHANDLING_AVSLUTTET
            )
        )
        val avsluttetBehandling2 = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).copy(id = 2)
        avsluttetBehandling2.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = avsluttetBehandling2,
                behandlingSteg = BehandlingSteg.BEHANDLING_AVSLUTTET
            )
        )
        val inneværendeBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).copy(id = 3)

        val forrigeIverksatteBehandling = BehandlingUtils.hentForrigeIverksatteBehandling(
            listOf(avsluttetBehandling, avsluttetBehandling2),
            inneværendeBehandling
        )
        assertEquals(2, forrigeIverksatteBehandling?.id)
    }

    @Test
    fun `hentForrigeIverksatteBehandling - skal returnere null dersom det ikke finnes noen iverksatte behandlinger`() {
        val inneværendeBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).copy(id = 3)

        val forrigeIverksatteBehandling = BehandlingUtils.hentForrigeIverksatteBehandling(
            emptyList(),
            inneværendeBehandling
        )
        assertNull(forrigeIverksatteBehandling)
    }
}
