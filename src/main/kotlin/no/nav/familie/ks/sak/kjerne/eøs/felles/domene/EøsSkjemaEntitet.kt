package no.nav.familie.ks.sak.kjerne.eøs.felles.domene

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.entitet.BaseEntitet

// Felles entitet klasse for alle EØS skjemaer.
// Her T kan være entitet klasse Kompetanse, Utenlandskbeløp eller Valutakurs
abstract class EøsSkjemaEntitet<T : EøsSkjemaEntitet<T>> :
    BaseEntitet(),
    EøsSkjema<T> {
    abstract var id: Long
    abstract var behandlingId: Long
}

fun <T : EøsSkjemaEntitet<T>> List<T>.medBehandlingId(behandlingId: BehandlingId): List<T> {
    this.forEach { it.behandlingId = behandlingId.id }
    return this
}
