package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val rolleConfig: RolleConfig,
    private val integrasjonService: IntegrasjonService,
    private val personidentService: PersonidentService,
    private val cacheManager: CacheManager,
    private val auditLogger: AuditLogger,
) {

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre en gitt handling.
     *
     * @param minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @param handling kort beskrivelse for handlingen. Eksempel: 'endre vilkår', 'oppprette behandling'.
     * Handlingen kommer til saksbehandler så det er viktig at denne gir mening.
     */
    fun validerTilgangTilHandling(minimumBehandlerRolle: BehandlerRolle, handling: String) {
        // Hvis minimumBehandlerRolle er forvalter, må innlogget bruker ha FORVALTER rolle
        if (minimumBehandlerRolle == BehandlerRolle.FORVALTER &&
            !SikkerhetContext.harInnloggetBrukerForvalterRolle(rolleConfig)
        ) {
            throw RolleTilgangskontrollFeil(
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} " +
                    "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle",
            )
        }
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)
        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw RolleTilgangskontrollFeil(
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang " +
                    "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
            )
        }
    }

    /**
     * Sjekk om saksbehandler har tilgang til å behandle bestemte personidenter
     * @param personIdenter liste over person identer man skal sjekke tilgang til
     * @param event operasjon som skal gjøres med identene
     * @param minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @param handling kort beskrivelse for handlingen.
     */
    fun validerTilgangTilHandlingOgPersoner(
        personIdenter: List<String>,
        event: AuditLoggerEvent,
        minimumBehandlerRolle: BehandlerRolle,
        handling: String,
    ) {
        validerTilgangTilHandling(minimumBehandlerRolle, handling)
        loggPersonoppslag(personIdenter, event)
        if (!harTilgangTilPersoner(personIdenter)) {
            throw RolleTilgangskontrollFeil(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til å behandle $personIdenter",
            )
        }
    }

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre bestemt handling og om saksbehandler kan behandle behandling
     * @param behandlingId id til behandling det skal sjekkes tilgang til
     * @param event operasjon som skal gjøres med identene
     * @param minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @param handling kort beskrivelse for handlingen.
     */
    fun validerTilgangTilHandlingOgFagsakForBehandling(
        behandlingId: Long,
        event: AuditLoggerEvent,
        minimumBehandlerRolle: BehandlerRolle,
        handling: String,
    ) {
        val fagsakId = behandlingRepository.hentBehandling(behandlingId).fagsak.id
        validerTilgangTilHandlingOgFagsak(fagsakId, event, minimumBehandlerRolle, handling)
    }

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre bestemt handling og om saksbehandler kan behandle fagsak
     * @param fagsakId id til fagsak det skal sjekkes tilgang til
     * @param event operasjon som skal gjøres med identene
     * @param minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @param handling kort beskrivelse for handlingen.
     */
    fun validerTilgangTilHandlingOgFagsak(
        fagsakId: Long,
        event: AuditLoggerEvent,
        minimumBehandlerRolle: BehandlerRolle,
        handling: String,
    ) {
        validerTilgangTilHandling(minimumBehandlerRolle, handling)
        val harTilgang = harSaksbehandlerTilgang("validerTilgangTilFagsak", fagsakId) {
            val aktør = fagsakService.hentFagsak(fagsakId).aktør
            val behandlinger = behandlingRepository.finnBehandlinger(fagsakId)
            val personIdenterIFagsak = behandlinger.flatMap { behandling ->
                val personopplysningGrunnlag =
                    personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                personopplysningGrunnlag?.personer?.map { person -> person.aktør.aktivFødselsnummer() } ?: emptyList()
            }.distinct().ifEmpty { listOf(aktør.aktivFødselsnummer()) }
            loggPersonoppslag(personIdenterIFagsak, event, CustomKeyValue("fagsak", fagsakId.toString()))
            harTilgangTilPersoner(personIdenterIFagsak)
        }
        if (!harTilgang) {
            throw RolleTilgangskontrollFeil(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til fagsak=$fagsakId.",
            )
        }
    }

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre bestemt handling og om saksbehandler kan behandle fagsak
     * @param personIdent id til person vi skal sjekkes fagsak tilgang til
     * @param event operasjon som skal gjøres med identene
     * @param minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @param handling kort beskrivelse for handlingen.
     */
    fun validerTilgangTilHandlingOgFagsakForPerson(
        personIdent: String,
        event: AuditLoggerEvent,
        minimumBehandlerRolle: BehandlerRolle,
        handling: String,
    ) {
        val aktør = personidentService.hentOgLagreAktør(personIdent, true)
        val fagsakId = fagsakService.hentFagsakForPerson(aktør).id
        validerTilgangTilHandlingOgFagsak(fagsakId, event, minimumBehandlerRolle, handling)
    }

    private fun harTilgangTilPersoner(
        personIdenter: List<String>,
    ): Boolean {
        return harSaksbehandlerTilgang("validerTilgangTilPersoner", personIdenter) {
            integrasjonService.sjekkTilgangTilPersoner(personIdenter).harTilgang
        }
    }

    /**
     * Logger at informasjon tilknyttet personidenter er forsøkt hentet
     * @param personIdenter liste over personidenter det er gjort oppslag på
     * @param event operasjon som skal gjøres med identene
     * @param customKeyValue tilleggsinformasjon. Eks: fagsakId, behandlingId.
     */
    private fun loggPersonoppslag(
        personIdenter: List<String>,
        event: AuditLoggerEvent,
        customKeyValue: CustomKeyValue? = null,
    ) {
        personIdenter.forEach { auditLogger.log(Sporingsdata(event, it, customKeyValue)) }
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
