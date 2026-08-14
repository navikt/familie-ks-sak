package no.nav.familie.ks.sak.kjerne.fagsaklåsing

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class FagsakLåsingRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakLåsingRepository: FagsakLåsingRepository

    @Nested
    inner class FinnAktivLåsForFagsak {
        @Test
        fun `skal returnere aktiv lås for fagsaken`() {
            // Arrange
            val søker = opprettOgLagreSøker()
            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søker, status = FagsakStatus.LÅST))
            val låstTidspunkt = LocalDateTime.of(2025, 6, 15, 10, 30, 0)

            fagsakLåsingRepository.save(
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = låstTidspunkt,
                    hendelse = FagsakLåsHendelse.LÅST,
                    begrunnelse = "Låst etter 1 år uten utbetaling eller vedtak.",
                    aktiv = true,
                ),
            )

            // Act
            val gjeldendeLås = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id)

            // Assert
            assertThat(gjeldendeLås).isNotNull
            assertThat(gjeldendeLås!!.tidspunkt).isEqualTo(låstTidspunkt)
            assertThat(gjeldendeLås.hendelse).isEqualTo(FagsakLåsHendelse.LÅST)
            assertThat(gjeldendeLås.aktiv).isTrue()
        }

        @Test
        fun `skal returnere null når fagsaken ikke har noen aktiv lås`() {
            // Arrange
            val søker = opprettOgLagreSøker()
            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søker, status = FagsakStatus.OPPRETTET))

            // Act
            val gjeldendeLås = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id)

            // Assert
            assertThat(gjeldendeLås).isNull()
        }

        @Test
        fun `skal ikke returnere inaktiv lås`() {
            // Arrange
            val søker = opprettOgLagreSøker()
            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søker, status = FagsakStatus.AVSLUTTET))

            fagsakLåsingRepository.save(
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = LocalDateTime.of(2025, 6, 15, 10, 30, 0),
                    hendelse = FagsakLåsHendelse.LÅST,
                    begrunnelse = "Låst etter 1 år uten utbetaling eller vedtak.",
                    aktiv = false,
                ),
            )

            // Act
            val gjeldendeLås = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id)

            // Assert
            assertThat(gjeldendeLås).isNull()
        }
    }
}
