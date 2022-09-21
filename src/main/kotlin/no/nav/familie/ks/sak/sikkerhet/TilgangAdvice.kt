package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.cache.CacheManager
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.reflect.full.declaredMemberProperties

@Aspect
@Component
class TilgangAdvice(
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val rolleConfig: RolleConfig,
    private val integrasjonService: IntegrasjonService,
    private val cacheManager: CacheManager,
    private val auditLogger: AuditLogger
) {

    @Before("@annotation(sjekkTilgang) ")
    fun sjekkTilgang(joinPoint: JoinPoint, sjekkTilgang: SjekkTilgang) {
        when (sjekkTilgang.tilgang) {
            Tilgang.FAGSAK -> {
                sjekkTilgangTilHandling(sjekkTilgang.minimumBehandlerRolle, sjekkTilgang.handling)
                val fagsakId =
                    lesFeltFraKontrollerMetodeParameter(joinPoint, listOf("fagsakId"), sjekkTilgang.index) as Long
                sjekkTilgangTilFagsak(fagsakId, sjekkTilgang.auditLoggerEvent)
            }

            Tilgang.BEHANDLING -> {
                sjekkTilgangTilHandling(sjekkTilgang.minimumBehandlerRolle, sjekkTilgang.handling)
                val behandlingId =
                    lesFeltFraKontrollerMetodeParameter(joinPoint, listOf("behandling"), sjekkTilgang.index) as Long
                sjekkTilgangTilBehandling(behandlingId, sjekkTilgang.auditLoggerEvent)
            }

            Tilgang.PERSON -> {
                sjekkTilgangTilHandling(sjekkTilgang.minimumBehandlerRolle, sjekkTilgang.handling)
                val personIdenter = lesFeltFraKontrollerMetodeParameter(
                    joinPoint,
                    listOf("personIdent", "søkersIdent", "ident"),
                    sjekkTilgang.index
                ) as String
                sjekkTilgangTilPersoner(listOf(personIdenter), sjekkTilgang.auditLoggerEvent)
            }

            Tilgang.HANDLING -> {
                sjekkTilgangTilHandling(sjekkTilgang.minimumBehandlerRolle, sjekkTilgang.handling)
            }
        }
    }

    private fun lesFeltFraKontrollerMetodeParameter(
        kontrollerMetode: JoinPoint,
        feltAliaser: List<String> = emptyList(),
        index: Int = 0
    ): Any {
        val kontrollerMetodeParameter = kontrollerMetode.args[index]
        if (erGetEllerDelete()) {
            return kontrollerMetodeParameter
        }
        val felter = kontrollerMetodeParameter::class.declaredMemberProperties
        val felt = felter.first { feltAliaser.contains(it.name) }.getter.call(kontrollerMetodeParameter)
        return felt!!
    }

    private fun erGetEllerDelete(): Boolean {
        val httpMethod: HttpMethod =
            HttpMethod.valueOf((RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request.method)
        return httpMethod === HttpMethod.GET || httpMethod === HttpMethod.DELETE
    }

    private fun sjekkTilgangTilHandling(minimumBehandlerRolle: BehandlerRolle, handling: String) {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)
        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw RolleTilgangskontrollFeil(
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang " +
                    "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling."
            )
        }
    }

    private fun sjekkTilgangTilFagsak(fagsakId: Long, event: AuditLoggerEvent) {
        val aktør = fagsakService.hentFagsak(fagsakId).aktør
        aktør.personidenter.forEach {
            Sporingsdata(
                event = event,
                personIdent = it.fødselsnummer,
                custom1 = CustomKeyValue("fagsak", fagsakId.toString())
            )
        }
        val behandlinger = behandlingRepository.finnBehandlinger(fagsakId)
        val personIdenterIFagsak = behandlinger.flatMap { behandling ->
            val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            when {
                personopplysningGrunnlag != null -> personopplysningGrunnlag.personer.map { person -> person.aktør.aktivFødselsnummer() }
                else -> emptyList()
            }
        }.distinct().ifEmpty { listOf(aktør.aktivFødselsnummer()) }
        val harTilgang = harTilgangTilPersoner(personIdenterIFagsak)
        if (!harTilgang) {
            throw RolleTilgangskontrollFeil(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til fagsak=$fagsakId.",
                frontendFeilmelding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til fagsak=$fagsakId."
            )
        }
    }

    private fun sjekkTilgangTilPersoner(personIdenter: List<String>, event: AuditLoggerEvent) {
        personIdenter.forEach { auditLogger.log(Sporingsdata(event, it)) }
        if (!harTilgangTilPersoner(personIdenter)) {
            throw RolleTilgangskontrollFeil(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang.",
                frontendFeilmelding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til $personIdenter"
            )
        }
    }

    private fun sjekkTilgangTilBehandling(behandlingId: Long, event: AuditLoggerEvent) {
        val harTilgang = harSaksbehandlerTilgang("validerTilgangTilBehandling", behandlingId) {
            val behandling = behandlingRepository.finnBehandling(behandlingId)
            val personIdenter =
                personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer?.map { it.aktør.aktivFødselsnummer() }
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
                    "har ikke tilgang.",
                "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til behandling=$behandlingId"
            )
        }
    }

    private fun harTilgangTilPersoner(personIdenter: List<String>): Boolean {
        return harSaksbehandlerTilgang("validerTilgangTilPersoner", personIdenter) {
            integrasjonService.sjekkTilgangTilPersoner(personIdenter).harTilgang
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
