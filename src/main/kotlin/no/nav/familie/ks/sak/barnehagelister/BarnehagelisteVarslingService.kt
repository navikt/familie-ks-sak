package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.common.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDateTime

@Profile("prod")
@Service
class BarnehagelisteVarslingService(
    val barnehageBarnRepository: BarnehagebarnRepository,
    val epostService: EpostService,
) {
    private val logger = LoggerFactory.getLogger(BarnehagelisteVarslingService::class.java)

    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    fun sendVarslingOmNyBarnehagelisteTilEnhet() {
        val kommunerSomHarFåttOppdatertBarnehagebarnSisteDøgn = finnKommunerSendtInnSisteDøgn()
        val enheterSomSkalVarslesTilKommuner = kommunerSomHarFåttOppdatertBarnehagebarnSisteDøgn.groupBy { finnEnhetForKommune(it) }
        enheterSomSkalVarslesTilKommuner
            .mapKeys { (enhetsNr, _) -> finnEpostForEnhet(enhetsNr) }
            .forEach { (epostAdresse, kommunerTilhørendeEnhet) ->
                epostService.sendEpostVarslingBarnehagelister(epostAdresse, kommunerTilhørendeEnhet)
            }
    }

    @Transactional
    fun finnKommunerSendtInnSisteDøgn(): List<String> = barnehageBarnRepository.findKommuneNavnEndretSidenTidspunkt(tidspunkt = LocalDateTime.now().minusDays(1))

    fun finnEnhetForKommune(kommuneNr: String): String {
        val enhetTilKommune: Map<String, List<String>> = hentEnhetTilKommune()

        val enhet = enhetTilKommune.filter { (_, kNummer) -> kNummer.contains(kommuneNr) }.keys

        if (enhet.size > 1) throw Feil("Flere enheter er ansvarlige for kommune $kommuneNr")
        if (enhet.isEmpty()) throw Feil("Ingen enheter har ansvar for kommune $kommuneNr")

        return enhet.single()
    }

    private fun hentEnhetTilKommune(): Map<String, List<String>> {
        val objectMapper = jacksonObjectMapper()
        val enhetTilKommuneJson = File("src/main/resources/barnehagelister-enhet-til-kommune.json")

        val enhetTilKommune = objectMapper.readValue(enhetTilKommuneJson, object : TypeReference<Map<String, List<String>>>() {})
        return enhetTilKommune
    }

    private fun finnEpostForEnhet(enhetsNr: String): String =
        when (enhetsNr) {
            "4820" -> "nav.familie-.og.pensjonsytelser.vadso.kontantstotte@nav.no" // Vadsø
            "4812" -> "kontantstotte@nav.no" // Bergen
            else -> {
                throw Feil("Enhet $enhetsNr er ikke implementert som en enhet for barnehagelister.")
            }
        }
}
