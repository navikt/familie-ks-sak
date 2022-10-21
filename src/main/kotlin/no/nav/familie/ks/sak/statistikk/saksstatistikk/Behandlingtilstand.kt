package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.OffsetDateTime

data class Behandlingtilstand(
    val funksjoneltTidspunkt: OffsetDateTime, // Tidspunkt for siste endring på behandlingen.
    val tekniskTidspunkt: OffsetDateTime? = null, // Tidspunktet da fagsystemet legger hendelsen på grensesnittet/topicen
    val mottattTid: OffsetDateTime? = null, // Tidspunktet da behandlingen oppstår (eks søknadstidspunkt, inntektsmelding, etc). Det er ønskelig å måle brukers opplevde ventetid. Ved elektronisk kontakt regner vi med at denne er lik registrertTid
    val saksnummer: String,
    val ytelseType: Ytelsestype,
    val behandlingID: Long,
    val behandlingType: BehandlingType,
    val behandlingStatus: BehandlingStatus,
    val behandlingResultat: String? = null, // Behandlingresultatstype,// TODO legg til når vi er ferdig med reusltat
    val behandlingErManueltOpprettet: Boolean,
    val sattPaaVent: SattPåVent? = null,
    val ansvarligEnhet: String,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String?,
    val forrigeBehandling: Long?,
    val revurderingOpprettetÅrsak: BehandlingÅrsak?
)

data class SattPåVent(
    val frist: OffsetDateTime,
    val tidSattPaaVent: OffsetDateTime,
    val aarsak: String
)
