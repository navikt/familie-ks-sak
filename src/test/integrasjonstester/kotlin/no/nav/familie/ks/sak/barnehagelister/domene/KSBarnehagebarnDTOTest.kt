package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister.domene

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.KSBarnehagebarnDTO
import no.nav.familie.ks.sak.config.KafkaConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KSBarnehagebarnDTOTest {
    @Test
    fun `tilBarnehagebarn skal mappe KSBarnehagebarnDTO til Barnehagebarn`() {
        // Arrange
        val ksBarnehagebarnDTO =
            KSBarnehagebarnDTO(
                id = UUID.randomUUID(),
                ident = "12345678910",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(3),
                antallTimerIBarnehage = 35.0,
                kommuneNavn = "OSLO",
                kommuneNr = "1234",
                barnehagelisteId = UUID.randomUUID().toString(),
                organisasjonsnummer = "123456789",
            )

        // Act
        val barnehagebarn = ksBarnehagebarnDTO.tilBarnehagebarn()

        // Assert
        assertThat(barnehagebarn).isEqualTo(
            Barnehagebarn(
                id = ksBarnehagebarnDTO.id,
                ident = ksBarnehagebarnDTO.ident,
                fom = ksBarnehagebarnDTO.fom,
                tom = ksBarnehagebarnDTO.tom,
                antallTimerIBarnehage = ksBarnehagebarnDTO.antallTimerIBarnehage,
                kommuneNavn = ksBarnehagebarnDTO.kommuneNavn,
                kommuneNr = ksBarnehagebarnDTO.kommuneNr,
                arkivReferanse = ksBarnehagebarnDTO.barnehagelisteId,
                kildeTopic = KafkaConfig.BARNEHAGELISTE_TOPIC,
                barnehageOrgNummer = "123456789",
            ),
        )
    }

    @Test
    fun `KSBarnehagebarnDTO skal kunne leses fra melding uten organisasjonsnummer`() {
        // Arrange
        val melding =
            """
            {
              "id": "${UUID.randomUUID()}",
              "ident": "12345678910",
              "fom": "2026-01-01",
              "tom": null,
              "antallTimerIBarnehage": 35.0,
              "kommuneNavn": "OSLO",
              "kommuneNr": "0301",
              "barnehagelisteId": "${UUID.randomUUID()}"
            }
            """.trimIndent()

        // Act
        val barnehagebarn = jsonMapper.readValue(melding, KSBarnehagebarnDTO::class.java).tilBarnehagebarn()

        // Assert
        assertThat(barnehagebarn.barnehageOrgNummer).isNull()
    }
}
