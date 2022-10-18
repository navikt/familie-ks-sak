package no.nav.familie.ks.sak.kjerne.settpåvent

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.settpåvent.domene.SettPåVent
import java.time.LocalDate

fun validerBehandlingKanSettesPåVent(
    gammelSettPåVent: SettPåVent?,
    frist: LocalDate,
    behandling: Behandling
) {
    if (gammelSettPåVent != null) {
        throw FunksjonellFeil(
            melding = "Behandling ${behandling.id} er allerede satt på vent.",
            frontendFeilmelding = "Behandlingen er allerede satt på vent."
        )
    }

    if (frist.isBefore(LocalDate.now())) {
        throw FunksjonellFeil(
            melding = "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
            frontendFeilmelding = "Fristen er satt før dagens dato."
        )
    }

    if (behandling.status == BehandlingStatus.AVSLUTTET) {
        throw FunksjonellFeil(
            melding = "Behandling ${behandling.id} er avsluttet og kan ikke settes på vent.",
            frontendFeilmelding = "Kan ikke sette en avsluttet behandling på vent."
        )
    }

    if (!behandling.aktiv) {
        throw Feil(
            "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent."
        )
    }
}
