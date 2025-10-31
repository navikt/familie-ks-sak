package no.nav.familie.ks.sak.barnehagelister

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattArkiv
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattArkivRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattRepository
import no.nav.familie.ks.sak.barnehagelister.domene.Melding
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnehageListeService(
    val barnehagelisteMottattRepository: BarnehagelisteMottattRepository,
    val barnehagebarnRepository: BarnehagebarnRepository,
    val taskService: TaskRepositoryWrapper,
    val barnehagelisteMottattArkivRepository: BarnehagelisteMottattArkivRepository,
) {
    private val xmlDeserializer =
        XmlMapper(
            JacksonXmlModule().apply {
                setDefaultUseWrapper(false)
            },
        ).registerKotlinModule()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(JavaTimeModule())

    fun erBarnehagelisteMottattTidligere(meldingId: String): Boolean = barnehagelisteMottattRepository.existsByMeldingId(meldingId) || barnehagelisteMottattArkivRepository.existsByMeldingId(meldingId)

    fun hentUarkiverteBarnehagelisteUuider(): List<String> = barnehagelisteMottattRepository.findAllIds()

    @Transactional
    fun lesOgArkiverBarnehageliste(uuid: UUID) {
        val barnehagelisteMottatt = barnehagelisteMottattRepository.findByIdOrNull(uuid) ?: return
        val barnehagelisteMelding = lesBarnehagelisteMottattMeldingXml(barnehagelisteMottatt.melding)

        val listeMedBarnehageBarn =
            barnehagelisteMelding.skjema.barnInfolinjer.map {
                it.tilBarnehagelisteEntitet(
                    kommuneNavn = barnehagelisteMelding.skjema.listeopplysninger.kommuneNavn,
                    kommuneNr = barnehagelisteMelding.skjema.listeopplysninger.kommuneNr,
                    arkivReferanse = barnehagelisteMottatt.meldingId,
                )
            }
        barnehagebarnRepository.saveAll(listeMedBarnehageBarn)
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

    private fun lesBarnehagelisteMottattMeldingXml(xml: String?): Melding = xmlDeserializer.readValue(xml, Melding::class.java)
}
