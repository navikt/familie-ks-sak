package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnService
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class BarnehagebarnServiceTest() : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var barnehagebarnRepository: BarnehagebarnRepository

    @Autowired
    private lateinit var barnehagebarnService: BarnehagebarnService


    val barnehagebarn = Barnehagebarn(
        ident = "12345678901",
        fom = LocalDate.now(),
        tom = LocalDate.now().plusMonths(3),
        antallTimerIBarnehage = 35.0,
        endringstype = "",
        kommuneNr = "1234",
        kommuneNavn = "OSLO",
        arkivReferanse = UUID.randomUUID().toString(),
    )

    @Test
    fun `erBarnehageBarnMottattTidligere returnerer true hvis fom, tom og arkivReferanse er like`() {
        // Arrange
        val barnehagebarnMedLikFomTomOgArkivReferanse = barnehagebarn.copy(
            antallTimerIBarnehage = 15.0,
            endringstype = "",
            kommuneNavn = "VIKEN",
            kommuneNr = "4321",
        )
        barnehagebarnRepository.save(barnehagebarn)

        //Act
        val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarnMedLikFomTomOgArkivReferanse)

        // Assert
        assertThat(erBarnehagebarnMottattTidligere).isTrue()
    }

    @Test
    fun `erBarnehageBarnMottattTidligere returnerer false hvis det ikke er noe barnehagebarn med samme ident`() {
        //Act
        val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarn)

        // Assert
        assertThat(erBarnehagebarnMottattTidligere).isFalse()
    }

    @Test
    fun `erBarnehageBarnMottattTidligere returnerer false hvis fom er ulik`() {
        // Arrange
        val barnehagebarnMedLikFomTomOgArkivReferanse = barnehagebarn.copy(
            fom = LocalDate.now().minusMonths(1),
        )
        barnehagebarnRepository.save(barnehagebarn)

        //Act
        val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarnMedLikFomTomOgArkivReferanse)

        // Assert
        assertThat(erBarnehagebarnMottattTidligere).isFalse()
    }
}