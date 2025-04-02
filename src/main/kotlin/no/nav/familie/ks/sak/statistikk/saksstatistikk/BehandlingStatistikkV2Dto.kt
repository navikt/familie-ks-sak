package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.ZonedDateTime

data class BehandlingStatistikkV2Dto(
    // Tidspunkt for siste endring på behandlingen.
    val funksjoneltTidspunkt: ZonedDateTime,
    // Tidspunktet da fagsystemet legger hendelsen på grensesnittet/topicen
    val tekniskTidspunkt: ZonedDateTime? = null,
    // Tidspunktet da behandlingen oppstår (eks søknadstidspunkt, inntektsmelding, etc). Det er ønskelig å måle brukers opplevde ventetid. Ved elektronisk kontakt regner vi med at denne er lik registrertTid
    val mottattTid: ZonedDateTime? = null,
    // Tidspunkt for når system/saksbehandler oppretter behandlingen
    val registrertTid: ZonedDateTime,
    val saksnummer: Long,
    val behandlingID: Long,
    val behandlingType: BehandlingType,
    val utenlandstilsnitt: String,
    val behandlingStatus: BehandlingStatus,
    val behandlingsResultat: Behandlingsresultat? = null,
    val behandlingErManueltOpprettet: Boolean,
    val sattPaaVent: SattPåVent? = null,
    val ansvarligEnhet: String,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String?,
    val behandlingOpprettetÅrsak: BehandlingÅrsak?,
    val automatiskBehandlet: Boolean,
    val relatertBehandlingId: String? = null,
    val relatertBehandlingFagsystem: String? = null,
)

data class SattPåVent(
    val frist: ZonedDateTime,
    val aarsak: String,
)
