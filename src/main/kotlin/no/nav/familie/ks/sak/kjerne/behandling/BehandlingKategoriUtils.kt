package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak

fun bestemKategoriVedOpprettelse(
    overstyrtKategori: BehandlingKategori?,
    behandlingType: BehandlingType,
    behandlingÅrsak: BehandlingÅrsak,
    // siste iverksatt behandling som har løpende utbetaling. Hvis løpende utbetaling ikke finnes, settes det til NASJONAL
    kategoriFraLøpendeBehandling: BehandlingKategori,
): BehandlingKategori =
    when {
        behandlingType in listOf(BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING) &&
            behandlingÅrsak == BehandlingÅrsak.SØKNAD -> {
            overstyrtKategori
                ?: throw FunksjonellFeil(
                    "Behandling med type ${behandlingType.visningsnavn} " +
                        "og årsak ${behandlingÅrsak.visningsnavn} krever behandlingskategori",
                )
        }

        else -> {
            kategoriFraLøpendeBehandling
        }
    }
