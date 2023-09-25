package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnDto
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnMedBehandlingRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattArkiv
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattArkivRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattRepository
import no.nav.familie.ks.sak.barnehagelister.domene.Melding
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
    val barnehagelisteMottattArkivRepository: BarnehagelisteMottattArkivRepository,
    val barnehagebarnMedBehandlingRepository: BarnehagebarnMedBehandlingRepository,
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
    fun lagreBarnehagelisteMottatt(barnehagelisteMottatt: BarnehagelisteMottatt) {
        barnehagelisteMottattRepository.save(barnehagelisteMottatt)
    }

    fun erListenMottattTidligere(meldingId: String): Boolean {
        return barnehagelisteMottattRepository.existsByMeldingId(meldingId) || barnehagelisteMottattArkivRepository.existsByMeldingId(meldingId)
    }

    @Transactional
    fun lesOgArkiver(uuid: UUID) {
        var barnehagelisteMottatt = barnehagelisteMottattRepository.findByIdOrNull(uuid)

        if (barnehagelisteMottatt != null) {
            val barnehagelisteMelding = lesBarnehagelisteMottattMeldingXml(barnehagelisteMottatt?.melding)
            val barnehagelister = barnehagelisteMelding.skjema.barnInfolinjer.map { it ->
                it.tilBarnehagelisteEntitet(kommuneNavn = barnehagelisteMelding.skjema.listeopplysninger.kommuneNavn, kommuneNr = barnehagelisteMelding.skjema.listeopplysninger.kommuneNr)
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

    fun hentUarkvierteBarnehagelisteUuider(): List<String> {
        return barnehagelisteMottattRepository.findAllIds()
    }

    fun hentAlleBarnehagebarn(): List<Barnehagebarn> {
        return barnehagebarnRepository.findAll()
    }

    // fun hentAlleBarnehagebarnPage(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<Barnehagebarn> {
    fun hentAlleBarnehagebarnPage(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnDto> {
        /* var sort = Sort.by("endretTidspunkt").descending()
        if (barnehagebarnRequestParams.sortBy != null) {
            if (barnehagebarnRequestParams.sortAsc) {
                sort = Sort.by(barnehagebarnRequestParams.sortBy).ascending()
            } else {
                sort = Sort.by(barnehagebarnRequestParams.sortBy).descending()
            }
        } */
        val pageable: Pageable = PageRequest.of(barnehagebarnRequestParams.offset ?: 0, barnehagebarnRequestParams.limit ?: 50) // sort

        // return barnehagebarnRepository.findAll(pageable)
        return barnehagebarnMedBehandlingRepository.hentData(barnehagebarnRequestParams.toSql(), barnehagebarnRequestParams.toCountSql(), pageable)
    }
}
