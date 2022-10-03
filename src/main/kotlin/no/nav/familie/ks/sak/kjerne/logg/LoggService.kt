package no.nav.familie.ks.sak.kjerne.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.familie.ks.sak.kjerne.logg.domene.LoggRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service

@Service
class LoggService(
    private val loggRepository: LoggRepository,
    private val rolleConfig: RolleConfig
) {

    private val metrikkPerLoggType: Map<LoggType, Counter> = LoggType.values().associateWith {
        Metrics.counter(
            "behandling.logg",
            "type",
            it.name,
            "beskrivelse",
            it.visningsnavn
        )
    }

    fun opprettAutovedtakTilManuellBehandling(behandling: Behandling, tekst: String) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.AUTOVEDTAK_TIL_MANUELL_BEHANDLING,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    private fun lagreLogg(logg: Logg): Logg {
        metrikkPerLoggType[logg.type]?.increment()

        return loggRepository.save(logg)
    }

    fun hentLoggForBehandling(behandlingId: Long): List<Logg> = loggRepository.hentLoggForBehandling(behandlingId)

    fun opprettBehandlendeEnhetEndret(
        behandling: Behandling,
        fraEnhet: ArbeidsfordelingPåBehandling,
        tilEnhet: ArbeidsfordelingPåBehandling,
        manuellOppdatering: Boolean,
        begrunnelse: String
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLENDE_ENHET_ENDRET,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = "Behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} endret " +
                    "fra ${fraEnhet.behandlendeEnhetId} ${fraEnhet.behandlendeEnhetNavn} " +
                    "til ${tilEnhet.behandlendeEnhetId} ${tilEnhet.behandlendeEnhetNavn}." +
                    if (begrunnelse.isNotBlank()) "\n\n$begrunnelse" else ""
            )
        )
    }

    fun opprettBehandlingLogg(behandling: Behandling) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "${behandling.type.visningsnavn} opprettet",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER)
            )
        )
    }

    fun opprettRegistrertSøknadLogg(behandlingId: Long, søknadGrunnlagFinnesFraFør: Boolean) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.SØKNAD_REGISTRERT,
                tittel = if (!søknadGrunnlagFinnesFraFør) "Søknaden ble registrert" else "Søknaden ble endret",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                )
            )
        )
    }
}
