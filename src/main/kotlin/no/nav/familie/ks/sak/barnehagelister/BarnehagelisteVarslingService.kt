package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ks.sak.barnehagelister.BarnehagelisteVarslingService.Companion.PATH_ENHETSNR_TIL_KOMMUNENR_OVERSIKT
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.common.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BarnehagelisteVarslingService(
    val barnehageBarnRepository: BarnehagebarnRepository,
    val epostService: EpostService,
) {
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    fun sendVarlingOmBarnehagelisteHverMorgenKl6() {
        sendVarslingOmNyBarnehagelisteTilEnhet()
    }

    fun sendVarslingOmNyBarnehagelisteTilEnhet(
        dryRun: Boolean = false,
        dryRunEpost: String = "",
    ) {
        logger.info("Sjekker om det er kommet inn barnehagelister ila siste døgn.")
        val kommunerSendtForFørsteGangSisteDøgn = finnKommunerSendtInnSisteDøgn()
        val enheterSomSkalVarslesTilKommuner = kommunerSendtForFørsteGangSisteDøgn.groupBy { finnEnhetForKommune(it) }

        if (enheterSomSkalVarslesTilKommuner.isNotEmpty()) {
            logger.info("Sender epost for nye kommuner i barnehagelister: $kommunerSendtForFørsteGangSisteDøgn")

            enheterSomSkalVarslesTilKommuner
                .map { (enhetsNr, kommuneNrTilhørendeEnhet) ->
                    finnKontaktEpostForEnhet(enhetsNr) to
                        kommuneNrTilhørendeEnhet
                            .map {
                                finnKommuneNavnForKnr(
                                    it,
                                )
                            }.toSet()
                }.forEach { (epostAdresse, kommunerTilhørendeEnhet) ->
                    if (dryRun) {
                        epostService.sendEpostVarslingBarnehagelister(dryRunEpost, kommunerTilhørendeEnhet)
                    } else {
                        epostService.sendEpostVarslingBarnehagelister(epostAdresse, kommunerTilhørendeEnhet)
                    }
                }
        }
    }

    // Bruker kommuneNr i stedet for kommunenavn siden det er mindre mulighet for feilskriving
    private fun finnKommunerSendtInnSisteDøgn(): Set<String> {
        val barnSendtInnSisteDøgn =
            barnehageBarnRepository
                .findAll()
                .filter { it.endretTidspunkt >= LocalDateTime.now().minusDays(1) }
        return barnSendtInnSisteDøgn.map { it.kommuneNr }.toSet()
    }

    companion object {
        val `KONTAKT_E-POST_VADSØ` = "nav.familie-.og.pensjonsytelser.vadso.kontantstotte@nav.no"
        val `KONTAKT_E-POST_BERGEN` = "kontantstotte@nav.no"
        val PATH_ENHETSNR_TIL_KOMMUNENR_OVERSIKT =
            "barnehagelister/barnehagelister-enhet-til-kommune.json"

        private val logger = LoggerFactory.getLogger(BarnehagelisteVarslingService::class.java)
    }
}

private fun finnKontaktEpostForEnhet(enhetsNr: String): String =
    when (enhetsNr) {
        "4820" -> BarnehagelisteVarslingService.`KONTAKT_E-POST_VADSØ`
        "4812" -> BarnehagelisteVarslingService.`KONTAKT_E-POST_BERGEN`
        else -> {
            throw Feil("Det er ikke lagret noen epost for å kontakte enhet $enhetsNr.")
        }
    }

private data class Kommune(
    val knr: String,
    val kommuneNavn: String,
)

private fun hentAnsvarligEnhetForKommuneMap(): Map<String, Set<Kommune>> {
    val objectMapper = jacksonObjectMapper()

    val enhetTilKommuneFil = Thread.currentThread().contextClassLoader.getResource(PATH_ENHETSNR_TIL_KOMMUNENR_OVERSIKT)

    return objectMapper.readValue(enhetTilKommuneFil, object : TypeReference<Map<String, Set<Kommune>>>() {})
}

private fun finnEnhetForKommune(kommuneNr: String): String {
    val enhetTilKommune: Map<String, Set<Kommune>> = hentAnsvarligEnhetForKommuneMap()

    val enhet = enhetTilKommune.filter { (_, kommuner) -> kommuner.map { it.knr }.contains(kommuneNr) }.keys

    if (enhet.size > 1) throw Feil("Flere enheter er ansvarlige for knr $kommuneNr")
    if (enhet.isEmpty()) throw Feil("Ingen enheter har ansvar for knr $kommuneNr")

    return enhet.single()
}

private fun finnKommuneNavnForKnr(knr: String): String {
    val kommuner = hentAnsvarligEnhetForKommuneMap().values.flatten()
    return kommuner.find { it.knr == knr }?.kommuneNavn ?: throw Feil("Ikke lagret noe kommunenavn for knr $knr")
}
