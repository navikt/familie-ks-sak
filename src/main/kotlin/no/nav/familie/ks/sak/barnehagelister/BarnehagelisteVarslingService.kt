package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.familie.kontrakter.felles.objectMapper
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
    val geografiService: GeografiHierarkiService,
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

        val enhetTilFylkeMap = hentEnhetTilFylkeMap()
        val kommunerSendtForFørsteGangSisteDøgn = finnKommunerSendtInnSisteDøgn()
        val enheterSomSkalVarslesTilKommuner =
            kommunerSendtForFørsteGangSisteDøgn.groupBy { finnEnhetForKommuneEllerBydel(it.kode, enhetTilFylkeMap) }

        if (enheterSomSkalVarslesTilKommuner.isNotEmpty()) {
            logger.info("Sender epost for nye kommuner i barnehagelister: $kommunerSendtForFørsteGangSisteDøgn")

            enheterSomSkalVarslesTilKommuner.entries
                .map { (enhetsNr, kommuneEllerBydelListe) ->
                    val epost = finnKontaktEpostForEnhet(enhetsNr)
                    val navnSet = kommuneEllerBydelListe.map { it.navn }.toSet()
                    epost to navnSet
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
    private fun finnKommunerSendtInnSisteDøgn(): Set<KommuneEllerBydel> {
        val barnSendtInnSisteDøgn =
            barnehageBarnRepository
                .findAll()
                .filter { it.endretTidspunkt >= LocalDateTime.now().minusDays(1) }
        return barnSendtInnSisteDøgn.map { KommuneEllerBydel(it.kommuneNr, it.kommuneNavn) }.toSet()
    }

    private fun hentEnhetTilFylkeMap(): Map<String, Set<String>> {
        val enhetTilFylkeFil = Thread.currentThread().contextClassLoader.getResource(PATH_ENHETSNR_TIL_FYLKENR_OVERSIKT)

        return objectMapper.readValue(enhetTilFylkeFil, object : TypeReference<Map<String, Set<String>>>() {})
    }

    private fun hentAnsvarligEnhetForKommuneEllerBydelMap(enhetTilFylkeMap: Map<String, Set<String>>): Map<String, Set<KommuneEllerBydel>> =
        enhetTilFylkeMap.mapValues { (_, fylkeKoder) ->
            buildSet {
                fylkeKoder.forEach { fk ->
                    val kodeTilNavnIFylke = geografiService.hentBydelEllerKommuneKodeTilNavnFraFylkeNr(fk)
                    kodeTilNavnIFylke.forEach { (kode, navn) ->
                        add(KommuneEllerBydel(kode = kode, navn = navn))
                    }
                }
            }
        }

    private fun finnEnhetForKommuneEllerBydel(
        kode: String,
        enhetTilFylkeMap: Map<String, Set<String>>,
    ): String {
        val ansvarligMap = hentAnsvarligEnhetForKommuneEllerBydelMap(enhetTilFylkeMap)
        val kodeTilEnhet = ansvarligMap.flatMap { (enhet, set) -> set.map { it.kode to enhet } }.toMap()

        return kodeTilEnhet[kode]
            ?: throw Feil("Ingen enheter har ansvar for kommunen/bydelen $kode")
    }

    companion object {
        val `KONTAKT_E-POST_VADSØ` = "nav.familie-.og.pensjonsytelser.vadso.kontantstotte@nav.no"
        val `KONTAKT_E-POST_BERGEN` = "kontantstotte@nav.no"
        val PATH_ENHETSNR_TIL_FYLKENR_OVERSIKT =
            "barnehagelister/barnehagelister-enhet-til-fylkeNr.json"

        private val logger = LoggerFactory.getLogger(BarnehagelisteVarslingService::class.java)
    }
}

data class KommuneEllerBydel(
    val kode: String,
    val navn: String,
)

private fun finnKontaktEpostForEnhet(enhetsNr: String): String =
    when (enhetsNr) {
        "4820" -> BarnehagelisteVarslingService.`KONTAKT_E-POST_VADSØ`
        "4812" -> BarnehagelisteVarslingService.`KONTAKT_E-POST_BERGEN`
        else -> {
            throw Feil("Det er ikke lagret noen epost for å kontakte enhet $enhetsNr.")
        }
    }
