package no.nav.familie.ks.sak.kjerne.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.familie.ks.sak.kjerne.logg.domene.LoggRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

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

    fun opprettRegistrertSøknadLogg(behandlingId: Long, aktivSøknadGrunnlagFinnesFraFør: Boolean) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.SØKNAD_REGISTRERT,
                tittel = if (!aktivSøknadGrunnlagFinnesFraFør) "Søknaden ble registrert" else "Søknaden ble endret",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                )
            )
        )
    }

    fun opprettMottattDokumentLogg(behandling: Behandling, tekst: String = "", mottattDato: LocalDateTime) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.DOKUMENT_MOTTATT,
                tittel = "Dokument mottatt ${mottattDato.toLocalDate().tilKortString()}",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    fun opprettSettPåVentLogg(behandling: Behandling, årsak: String) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLIG_SATT_PÅ_VENT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = "Årsak: $årsak"
            )
        )
    }

    fun opprettOppdaterVentingLogg(behandling: Behandling, endretÅrsak: String?, endretFrist: LocalDate?) {
        val tekst = when {
            endretFrist != null && endretÅrsak != null ->
                "Frist og årsak er endret til $endretÅrsak og ${endretFrist.tilKortString()}"

            endretFrist != null ->
                "Frist er endret til ${endretFrist.tilKortString()}"

            endretÅrsak != null ->
                "Årsak er endret til $endretÅrsak"

            else -> {
                logger.info("Ingen endringer tilknyttet frist eller årsak på ventende behandling. Oppretter ikke logginnslag.")
                return
            }
        }
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.VENTENDE_BEHANDLING_ENDRET,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    fun opprettBehandlingGjenopptattLogg(behandling: Behandling) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLIG_GJENOPPTATT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                )
            )
        )
    }

    fun opprettVilkårsvurderingLogg(
        behandling: Behandling,
        behandlingsForrigeResultat: Behandlingsresultat,
        behandlingsNyResultat: Behandlingsresultat
    ) {
        val tekst = when {
            behandlingsForrigeResultat == Behandlingsresultat.IKKE_VURDERT -> {
                "Resultat ble ${behandlingsNyResultat.displayName.lowercase()}"
            }

            behandlingsForrigeResultat != behandlingsNyResultat -> {
                "Resultat gikk fra ${behandlingsForrigeResultat.displayName.lowercase()} til ${behandlingsNyResultat.displayName.lowercase()}"
            }

            else -> {
                logger.info("Logg kan ikke lagres når $behandlingsForrigeResultat er samme som $behandlingsNyResultat")
                return
            }
        }
        val tittel = when {
            behandlingsForrigeResultat != Behandlingsresultat.IKKE_VURDERT -> "Vilkårsvurdering endret"
            else -> "Vilkårsvurdering gjennomført"
        }

        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.VILKÅRSVURDERING,
                tittel = tittel,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LoggService::class.java)
    }
}
