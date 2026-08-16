package no.nav.familie.ks.sak.integrasjon.tilgangsmaskin

import io.micrometer.core.instrument.Metrics
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinException
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * Skyggekjøring av Tilgangsmaskinen (NAV-27897): sammenligner dagens tilgangsbeslutning fra
 * familie-integrasjoner med hva Tilgangsmaskinen ville svart, og logger divergenser.
 * Påvirker aldri selve tilgangsbeslutningen.
 */
@Service
class TilgangsmaskinSkyggeService(
    private val tilgangsmaskinKlient: TilgangsmaskinKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val sammenlignetTeller = Metrics.counter("familie.ks.sak.tilgangsmaskin.skygge.sammenlignet")
    private val manglendeSvarTeller = Metrics.counter("familie.ks.sak.tilgangsmaskin.skygge.manglende.svar")

    fun skyggeSjekkTilgangTilPersoner(
        personIdenter: List<String>,
        tilgangerFraIntegrasjoner: List<Tilgang>,
    ) {
        try {
            // Bulk-endepunktet krever OBO-token, og systemkontekst har uansett blanke tilganger i dag.
            if (SikkerhetContext.erSystemKontekst()) return
            if (!featureToggleService.isEnabled(FeatureToggle.SKAL_SKYGGEKJØRE_TILGANGSMASKINEN)) return

            val (manglendeSvar, resultaterFraTilgangsmaskinen) =
                tilgangsmaskinKlient
                    .sjekkTilgangTilPersoner(personIdenter.toSet(), Regeltype.KJERNE_REGELTYPE)
                    .partition { it.erManglendeSvar() }
            if (manglendeSvar.isNotEmpty()) {
                manglendeSvarTeller.increment(manglendeSvar.size.toDouble())
                logger.warn(
                    "Tilgangsmaskin-skygge: fikk ikke svar for ${manglendeSvar.size} av " +
                        "${manglendeSvar.size + resultaterFraTilgangsmaskinen.size} identer, disse sammenlignes ikke.",
                )
            }
            sammenlignetTeller.increment(resultaterFraTilgangsmaskinen.size.toDouble())

            val tilgangerPerIdent = tilgangerFraIntegrasjoner.associateBy { it.personIdent }
            val divergenser =
                resultaterFraTilgangsmaskinen.mapNotNull { nyttResultat ->
                    val gammelTilgang = tilgangerPerIdent[nyttResultat.personIdent] ?: return@mapNotNull null
                    if (gammelTilgang.harTilgang != nyttResultat.harTilgang) gammelTilgang to nyttResultat else null
                }
            if (divergenser.isEmpty()) {
                logger.info("Tilgangsmaskin-skygge: sammenlignet ${resultaterFraTilgangsmaskinen.size} identer, ingen divergens.")
                return
            }

            divergenser.forEach { (gammelTilgang, nyttResultat) -> loggDivergens(gammelTilgang, nyttResultat) }

            val avvisningskoder = divergenser.mapNotNull { (_, nyttResultat) -> nyttResultat.avvisningskode }.groupingBy { it }.eachCount()
            val traceIder = divergenser.mapNotNull { (_, nyttResultat) -> nyttResultat.traceId }

            logger.warn(
                "Tilgangsmaskin-skygge: ${divergenser.size} av ${resultaterFraTilgangsmaskinen.size} identer divergerte. " +
                    "Avvisningskoder=$avvisningskoder, traceIder=$traceIder. Se securelogs for detaljer.",
            )
        } catch (exception: Exception) {
            // Skyggingen skal aldri påvirke den gjeldende tilgangskontrollen.
            val httpStatus = (exception as? TilgangsmaskinException)?.httpStatus
            Metrics
                .counter(
                    "familie.ks.sak.tilgangsmaskin.skygge.feilet",
                    "feiltype",
                    exception.javaClass.simpleName,
                    "httpStatus",
                    httpStatus?.toString() ?: "INGEN",
                ).increment()
            logger.warn("Tilgangsmaskin-skygge feilet: ${exception.javaClass.simpleName}${httpStatus?.let { " (HTTP $it)" } ?: ""}")
            secureLogger.warn("Tilgangsmaskin-skygge feilet", exception)
        }
    }

    private fun TilgangsmaskinResultat.erManglendeSvar(): Boolean =
        !harTilgang &&
            httpStatus == HttpStatus.INTERNAL_SERVER_ERROR.value() &&
            avvisningskode == Avvisningskode.UKJENT

    private fun loggDivergens(
        gammelTilgang: Tilgang,
        nyttResultat: TilgangsmaskinResultat,
    ) {
        val retning = if (nyttResultat.harTilgang) Divergensretning.NY_MILDERE else Divergensretning.NY_STRENGERE
        Metrics
            .counter(
                "familie.ks.sak.tilgangsmaskin.skygge.divergens",
                "retning",
                retning.tag,
                "avvisningskode",
                nyttResultat.avvisningskode?.name ?: "INGEN",
            ).increment()
        secureLogger.warn(
            "Tilgangsmaskin-skygge divergens (${retning.tag}) for ident ${nyttResultat.personIdent}: " +
                "integrasjoner harTilgang=${gammelTilgang.harTilgang} (begrunnelse=${gammelTilgang.begrunnelse}), " +
                "tilgangsmaskinen harTilgang=${nyttResultat.harTilgang} (avvisningskode=${nyttResultat.avvisningskode}, " +
                "begrunnelse=${nyttResultat.begrunnelse}, kanOverstyres=${nyttResultat.kanOverstyres}, " +
                "httpStatus=${nyttResultat.httpStatus}, traceId=${nyttResultat.traceId})",
        )
    }

    private enum class Divergensretning(
        val tag: String,
    ) {
        NY_MILDERE("ny-mildere"),
        NY_STRENGERE("ny-strengere"),
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TilgangsmaskinSkyggeService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
