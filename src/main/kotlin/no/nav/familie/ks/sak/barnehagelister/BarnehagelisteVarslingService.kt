package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.kontrakter.felles.kodeverk.Fylke
import no.nav.familie.kontrakter.felles.kodeverk.HierarkiGeografiInnlandDto
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BarnehagelisteVarslingService(
    val barnehageBarnRepository: BarnehagebarnRepository,
    val epostService: EpostService,
    val integrasjonClient: IntegrasjonClient,
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
        val enheterSomSkalVarslesTilKommuner = kommunerSendtForFørsteGangSisteDøgn.groupBy { finnEnhetForKommuneEllerBydel(it.kode) }

        if (enheterSomSkalVarslesTilKommuner.isNotEmpty()) {
            logger.info("Sender epost for nye kommuner i barnehagelister: $kommunerSendtForFørsteGangSisteDøgn")

            enheterSomSkalVarslesTilKommuner
                .map { (enhetsNr, kommuneNrTilhørendeEnhet) ->
                    finnKontaktEpostForEnhet(enhetsNr) to
                        kommuneNrTilhørendeEnhet
                            .map {
                                it.navn
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
    private fun finnKommunerSendtInnSisteDøgn(): Set<KommuneEllerBydel> {
        val barnSendtInnSisteDøgn =
            barnehageBarnRepository
                .findAll()
                .filter { it.endretTidspunkt >= LocalDateTime.now().minusDays(1) }
        return barnSendtInnSisteDøgn.map { KommuneEllerBydel(it.kommuneNr, it.kommuneNavn) }.toSet()
    }

    private fun hentFylkerOgKommuner(): List<Fylke> {
        val dto: HierarkiGeografiInnlandDto = integrasjonClient.hentFylkerOgKommuner()

        return dto.norgeNode.fylker
    }

    private fun hentAnsvarligEnhetForFylkeMap(): Map<String, Set<String>> {
        val objectMapper = jacksonObjectMapper()

        val enhetTilFylkeFil = Thread.currentThread().contextClassLoader.getResource(PATH_ENHETSNR_TIL_FYLKENR_OVERSIKT)

        return objectMapper.readValue(enhetTilFylkeFil, object : TypeReference<Map<String, Set<String>>>() {})
    }

    private fun hentAnsvarligEnhetForKommuneEllerBydelMap(): Map<String, Set<KommuneEllerBydel>> {
        val enhetTilFylkeMap: Map<String, Set<String>> = hentAnsvarligEnhetForFylkeMap() // enhet -> fylkeskoder
        val fylker: List<Fylke> = hentFylkerOgKommuner()
        val fylkeByKode: Map<String, Fylke> = fylker.associateBy(Fylke::kode)

        return enhetTilFylkeMap.mapValues { (_, fylkeKoder) ->
            buildSet {
                fylkeKoder.forEach { fk ->
                    val fylke =
                        fylkeByKode[fk]
                            ?: throw Feil("Finner ikke fylke med kode $fk i geografihierarki hentet fra kodeverk.")
                    fylke.kommuner.forEach { kommune ->
                        val bydeler = kommune.bydeler
                        if (bydeler.isNotEmpty()) {
                            // Hvis kommunen har bydeler: legg inn KUN bydelene
                            bydeler.forEach { bydel ->
                                add(KommuneEllerBydel(kode = bydel.kode, navn = bydel.navn))
                            }
                        } else {
                            // Ellers: legg inn kommunen
                            add(KommuneEllerBydel(kode = kommune.kode, navn = kommune.navn))
                        }
                    }
                }
            }
        }
    }

    private fun finnEnhetForKommuneEllerBydel(KommuneEllerBydelNr: String): String {
        val enhetTilKommuneEllerBydel: Map<String, Set<KommuneEllerBydel>> = hentAnsvarligEnhetForKommuneEllerBydelMap()

        val enhet =
            enhetTilKommuneEllerBydel
                .filter { (_, kommuneEllerBydel) ->
                    kommuneEllerBydel.map { it.kode }.contains(KommuneEllerBydelNr)
                }.keys

        if (enhet.size > 1) throw Feil("Flere enheter er ansvarlige for kommunen/bydelen $KommuneEllerBydelNr")
        if (enhet.isEmpty()) throw Feil("Ingen enheter har ansvar for kommunen/bydelen $KommuneEllerBydelNr")

        return enhet.single()
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
