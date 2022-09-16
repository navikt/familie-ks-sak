package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.AuditLogger
import no.nav.familie.ks.sak.config.AuditLoggerEvent
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.CustomKeyValue
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.config.Sporingsdata
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersongrunnlagService
import org.springframework.cache.CacheManager

// Skal ikke brukes foreløpig, det må omskrives.
class TilgangService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val rolleConfig: RolleConfig,
    private val integrasjonClient: IntegrasjonClient,
    private val cacheManager: CacheManager,
    private val auditLogger: AuditLogger
) {

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre en gitt handling.
     *
     * @minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @handling kort beskrivelse for handlingen. Eksempel: 'endre vilkår', 'oppprette behandling'.
     * Handlingen kommer til saksbehandler så det er viktig at denne gir mening.
     */
    fun verifiserHarTilgangTilHandling(minimumBehandlerRolle: BehandlerRolle, handling: String) {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw RolleTilgangskontrollFeil(
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang " +
                    "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling."
            )
        }
    }

    private fun harTilgangTilPersoner(personIdenter: List<String>): Boolean =
        harSaksbehandlerTilgang("validerTilgangTilPersoner", personIdenter) {
            integrasjonClient.sjekkTilgangTilPersoner(personIdenter).harTilgang
        }

    fun validerTilgangTilBehandling(behandlingId: Long, event: AuditLoggerEvent) {
        val harTilgang = harSaksbehandlerTilgang("validerTilgangTilBehandling", behandlingId) {
            val behandling = behandlingHentOgPersisterService.hent(behandlingId)
            val personIdenter =
                persongrunnlagService.hentAktiv(behandlingId = behandlingId)?.personer?.map { it.aktør.aktivFødselsnummer() }
                    ?: listOf(behandling.fagsak.aktør.aktivFødselsnummer())
            personIdenter.forEach {
                auditLogger.log(
                    Sporingsdata(
                        event = event,
                        personIdent = it,
                        custom1 = CustomKeyValue("behandling", behandlingId.toString())
                    )
                )
            }
            harTilgangTilPersoner(personIdenter)
        }
        if (!harTilgang) {
            throw RolleTilgangskontrollFeil(
                "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til behandling=$behandlingId"
            )
        }
    }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(cacheName: String, verdi: T, hentVerdi: () -> Boolean): Boolean {
        if (SikkerhetContext.erSystemKontekst()) return true

        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler())) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }
}
