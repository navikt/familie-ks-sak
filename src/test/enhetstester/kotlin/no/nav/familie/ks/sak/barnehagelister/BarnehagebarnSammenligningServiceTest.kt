package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnehagebarnSammenligningServiceTest {

    private val mockBarnehagebarnRepository = mockk<BarnehagebarnRepository>()
    private val mockInfotrygdReplikaClient = mockk<InfotrygdReplikaClient>()

    private val barnehagebarnService =
        BarnehagebarnService(
            mockInfotrygdReplikaClient,
            mockBarnehagebarnRepository,
        )


    @Test
    fun `skal returnere true hvis barnehagebarn er like`() {
        //Arrange
        val barnehagebarn = Barnehagebarn(
            ident = "1234",
            fom = LocalDate.now(),
            tom = LocalDate.now().plusMonths(2),
            antallTimerIBarnehage = 20.0,
            endringstype = "",
            kommuneNavn = "Oslo",
            kommuneNr = "0000",
            arkivReferanse = "1234"
        )

        every {
            mockBarnehagebarnRepository.getByIdent("1234")
        } returns barnehagebarn

        //Act
        val barnehagebarnErLik = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarn)

        //Assert
        assertThat(barnehagebarnErLik).isTrue()

    }

}