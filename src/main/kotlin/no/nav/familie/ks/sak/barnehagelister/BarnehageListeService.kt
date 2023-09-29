package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.*
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class BarnehageListeService(
    val barnehagelisteMottattRepository: BarnehagelisteMottattRepository,
    val barnehagebarnRepository: BarnehagebarnRepository,
    val taskService: TaskService,
    val barnehagelisteMottattArkivRepository: BarnehagelisteMottattArkivRepository,
    val infotrygdReplikaClient: InfotrygdReplikaClient,
) {

    private val xmlDeserializer = XmlMapper(
        JacksonXmlModule().apply {
            setDefaultUseWrapper(false)
        },
    ).registerKotlinModule()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())

    @Transactional
    fun lagreBarnehagelisteMottattOgOpprettTaskForLesing(barnehagelisteMottatt: BarnehagelisteMottatt): BarnehagelisteMottatt {
        val barnehagelisteMottatt = barnehagelisteMottattRepository.save(barnehagelisteMottatt)
        taskService.save(
            LesOgArkiverBarnehagelisteTask.opprettTask(
                barnehagelisteId = barnehagelisteMottatt.id,
                arkivreferanse = barnehagelisteMottatt.meldingId,
            ),
        )
        return barnehagelisteMottatt
    }

    fun erListenMottattTidligere(meldingId: String): Boolean {
        return barnehagelisteMottattRepository.existsByMeldingId(meldingId) || barnehagelisteMottattArkivRepository.existsByMeldingId(
            meldingId,
        )
    }

    @Transactional
    fun lesOgArkiver(uuid: UUID) {
        val barnehagelisteMottatt = barnehagelisteMottattRepository.findByIdOrNull(uuid)

        if (barnehagelisteMottatt != null) {
            val barnehagelisteMelding = lesBarnehagelisteMottattMeldingXml(barnehagelisteMottatt.melding)
            val barnehagelister = barnehagelisteMelding.skjema.barnInfolinjer.map { it ->
                it.tilBarnehagelisteEntitet(
                    kommuneNavn = barnehagelisteMelding.skjema.listeopplysninger.kommuneNavn,
                    kommuneNr = barnehagelisteMelding.skjema.listeopplysninger.kommuneNr,
                    arkivReferanse = barnehagelisteMottatt.meldingId,
                )
            }
            barnehagebarnRepository.saveAll(barnehagelister)
            barnehagelisteMottattArkivRepository.save(
                BarnehagelisteMottattArkiv(
                    id = barnehagelisteMottatt.id,
                    melding = barnehagelisteMottatt.melding,
                    mottatTid = barnehagelisteMottatt.mottatTid,
                    meldingId = barnehagelisteMottatt.meldingId,
                ),
            )
            barnehagelisteMottattRepository.deleteById(barnehagelisteMottatt.id)
        }
    }

    fun lesBarnehagelisteMottattMeldingXml(xml: String?): Melding {
        return xmlDeserializer.readValue(xml, Melding::class.java)
    }

    fun hentBarnehagebarn(ident: String): Barnehagebarn {
        return barnehagebarnRepository.findByIdent(ident)
    }

    fun hentUarkiverteBarnehagelisteUuider(): List<String> {
        return barnehagelisteMottattRepository.findAllIds()
    }

    fun hentAlleBarnehagebarnInfotrygd(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnInfotrygdDto> {
        var sort = Sort.by(getCorrectSortBy("kommuneNavn")).descending()

        if (barnehagebarnRequestParams.sortAsc) {
            sort = Sort.by(getCorrectSortBy(barnehagebarnRequestParams.sortBy)).ascending()
        } else {
            sort = Sort.by(getCorrectSortBy(barnehagebarnRequestParams.sortBy)).descending()
        }

        val pageable: Pageable =
            PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, sort)

        val barna = infotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker()
        val barneMap = barna.map { it to it }.toMap()

        if (!barnehagebarnRequestParams.ident.isNullOrEmpty()) {
            if (barnehagebarnRequestParams.kunLøpendeFagsak) {
                return barnehagebarnRepository.findBarnehagebarnByIdentInfotrygd(
                    ident = barnehagebarnRequestParams.ident,
                    barna = barna,
                    pageable = pageable
                ).map { BarnehagebarnInfotrygdDto.fraBarnehageBarinInterfaceTilDto(it, true) }
            } else {
                return barnehagebarnRepository.findBarnehagebarnByIdentInfotrygdUavhengigAvFagsak(
                    ident = barnehagebarnRequestParams.ident,
                    pageable = pageable
                ).map {
                    BarnehagebarnInfotrygdDto.fraBarnehageBarinInterfaceTilDto(
                        it,
                        barneMap.contains(it.getIdent())
                    )
                }
            }
        } else if (!barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty()) {
            if (barnehagebarnRequestParams.kunLøpendeFagsak) {
                return barnehagebarnRepository.findBarnehagebarnByKommuneNavnInfotrygd(
                    kommuneNavn = barnehagebarnRequestParams.kommuneNavn,
                    barna = barna,
                    pageable = pageable
                ).map { BarnehagebarnInfotrygdDto.fraBarnehageBarinInterfaceTilDto(it, true) }
            } else {
                return barnehagebarnRepository.findBarnehagebarnByKommuneNavnInfotrygdUavhengigAvFagsak(
                    kommuneNavn = barnehagebarnRequestParams.kommuneNavn,
                    pageable = pageable
                ).map {
                    BarnehagebarnInfotrygdDto.fraBarnehageBarinInterfaceTilDto(
                        it,
                        barneMap.contains(it.getIdent())
                    )
                }
            }
        } else {
            if (barnehagebarnRequestParams.kunLøpendeFagsak) {
                return barnehagebarnRepository.findBarnehagebarnInfotrygd(barna = barna, pageable = pageable)
                    .map { BarnehagebarnInfotrygdDto.fraBarnehageBarinInterfaceTilDto(it, true) }
            } else {
                return barnehagebarnRepository.findBarnehagebarnInfotrygdUavhengigAvFagsak(pageable = pageable).map {
                    BarnehagebarnInfotrygdDto.fraBarnehageBarinInterfaceTilDto(
                        it,
                        barneMap.contains(it.getIdent())
                    )
                }
            }
        }
    }

    fun hentAlleBarnehagebarnPage(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnDtoInterface> {
        var sort = Sort.by(getCorrectSortBy("kommuneNavn")).descending()
        var fagsakstatuser = listOf("LØPENDE")

        if (barnehagebarnRequestParams.sortAsc) {
            sort = Sort.by(getCorrectSortBy(barnehagebarnRequestParams.sortBy)).ascending()
        } else {
            sort = Sort.by(getCorrectSortBy(barnehagebarnRequestParams.sortBy)).descending()
        }
        val pageable: Pageable =
            PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, sort)

        if (!barnehagebarnRequestParams.ident.isNullOrEmpty()) {
            if (barnehagebarnRequestParams.kunLøpendeFagsak) {
                return barnehagebarnRepository.findBarnehagebarnByIdent(
                    fagsakStatuser = fagsakstatuser,
                    ident = barnehagebarnRequestParams.ident,
                    pageable = pageable,
                )
            } else {
                return barnehagebarnRepository.findBarnehagebarnByIdentUavhengigAvFagsak(
                    ident = barnehagebarnRequestParams.ident,
                    pageable = pageable,
                )
            }
        } else if (!barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty()) {
            if (barnehagebarnRequestParams.kunLøpendeFagsak) {
                return barnehagebarnRepository.findBarnehagebarnByKommuneNavn(
                    fagsakStatuser = fagsakstatuser,
                    barnehagebarnRequestParams.kommuneNavn,
                    pageable,
                )
            } else {
                return barnehagebarnRepository.findBarnehagebarnByKommuneNavnUavhengigAvFagsak(
                    barnehagebarnRequestParams.kommuneNavn,
                    pageable,
                )
            }
        } else {
            if (barnehagebarnRequestParams.kunLøpendeFagsak) {
                return barnehagebarnRepository.findBarnehagebarn(fagsakStatuser = fagsakstatuser, pageable = pageable)
            } else {
                return barnehagebarnRepository.findAlleBarnehagebarnUavhengigAvFagsak(pageable = pageable)
            }
        }
    }

    private fun getCorrectSortBy(sortBy: String): String {
        return when (sortBy.lowercase()) {
            "endrettidspunkt" -> "endret_tid"
            "kommunenavn" -> "kommune_navn"
            "kommunenr" -> "kommune_nr"
            "antalltimeribarnehage" -> "antall_timer_i_barnehage"
            else -> sortBy
        }
    }
}
