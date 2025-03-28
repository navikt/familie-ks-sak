package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.common.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDateTime

@Service
class BarnehagelisteVarslingService(
    val barnehageBarnRepository: BarnehagebarnRepository,
    val epostService: EpostService,
) {
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    fun sendVarslingOmNyBarnehagelisteTilEnhet() {
        val kommunerSendtForFørsteGangSisteDøgn = finnNyeKommunerSendtInnSisteDøgn()
        val enheterSomSkalVarslesTilKommuner = kommunerSendtForFørsteGangSisteDøgn.groupBy { finnEnhetForKommune(it) }

        if (enheterSomSkalVarslesTilKommuner.isNotEmpty()) {
            logger.info("Sender epost for nye kommuner i barnehagelister: $kommunerSendtForFørsteGangSisteDøgn")
        }
        enheterSomSkalVarslesTilKommuner
            .mapKeys { (enhetsNr, _) -> finnKontaktEpostForEnhet(enhetsNr) }
            .forEach { (epostAdresse, kommunerTilhørendeEnhet) ->
                epostService.sendEpostVarslingBarnehagelister(epostAdresse, kommunerTilhørendeEnhet.toSet())
            }
    }

    // Bruker kommuneNr i stedet for kommunenavn siden det er mindre mulighet for feilskriving
    @Transactional
    fun finnNyeKommunerSendtInnSisteDøgn(): Set<String> {
        val barnSendtInnTidligereEnnSisteDøgn =
            barnehageBarnRepository
                .findAll()
                .filter { it.endretTidspunkt <= LocalDateTime.now().minusDays(1) }
        val barnSendtInnSisteDøgn =
            barnehageBarnRepository
                .findAll()
                .filter { it.endretTidspunkt >= LocalDateTime.now().minusDays(1) }
        return barnSendtInnSisteDøgn.map { it.kommuneNr }.toSet() - barnSendtInnTidligereEnnSisteDøgn.map { it.kommuneNr }.toSet()
    }

    private fun finnEnhetForKommune(kommuneNr: String): String {
        val enhetTilKommune: Map<String, Set<String>> = hentAnsvarligEnhetForKommune()

        val enhet = enhetTilKommune.filter { (_, kNummer) -> kNummer.contains(kommuneNr) }.keys

        if (enhet.size > 1) throw Feil("Flere enheter er ansvarlige for knr $kommuneNr")
        if (enhet.isEmpty()) throw Feil("Ingen enheter har ansvar for knr $kommuneNr")

        return enhet.single()
    }

    private fun hentAnsvarligEnhetForKommune(): Map<String, Set<String>> {
        val objectMapper = jacksonObjectMapper()
        val enhetTilKommuneFil = File(PATH_ENHETSNR_TIL_KOMMUNENR_OVERSIKT)

        return objectMapper.readValue(enhetTilKommuneFil, object : TypeReference<Map<String, Set<String>>>() {})
    }

    private fun finnKontaktEpostForEnhet(enhetsNr: String): String =
        when (enhetsNr) {
            "4820" -> `KONTAKT_E-POST_VADSØ`
            "4812" -> `KONTAKT_E-POST_BERGEN`
            else -> {
                throw Feil("Det er ikke lagret noen epost for å kontakte enhet $enhetsNr.")
            }
        }

    companion object {
        private val `KONTAKT_E-POST_VADSØ` = "nav.familie-.og.pensjonsytelser.vadso.kontantstotte@nav.no"
        private val `KONTAKT_E-POST_BERGEN` = "kontantstotte@nav.no"

        val PATH_ENHETSNR_TIL_KOMMUNENR_OVERSIKT = "src/main/resources/barnehagelister/barnehagelister-enhet-til-kommune.json"

        private val logger = LoggerFactory.getLogger(BarnehagelisteVarslingService::class.java)
    }
}
