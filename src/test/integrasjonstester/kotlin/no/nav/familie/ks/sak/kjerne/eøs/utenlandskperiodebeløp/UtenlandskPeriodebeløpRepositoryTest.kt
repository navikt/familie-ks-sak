package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagUtenlandskPeriodebeløp
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth

class UtenlandskPeriodebeløpRepositoryTest(
    @Autowired private val aktørRepository: AktørRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
) : OppslagSpringRunnerTest() {
    @Test
    fun `Skal lagre flere utenlandske periodebeløp med gjenbruk av flere aktører`() {
        val søker = aktørRepository.save(randomAktør())
        val barn1 = aktørRepository.save(randomAktør())
        val barn2 = aktørRepository.save(randomAktør())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val utenlandskPeriodebeløp =
            utenlandskPeriodebeløpRepository.save(
                lagUtenlandskPeriodebeløp(
                    barnAktører = setOf(barn1, barn2),
                ).also { it.behandlingId = behandling.id },
            )

        val utenlandskPeriodebeløp2 =
            utenlandskPeriodebeløpRepository.save(
                lagUtenlandskPeriodebeløp(
                    barnAktører = setOf(barn1, barn2),
                ).also { it.behandlingId = behandling.id },
            )

        assertEquals(utenlandskPeriodebeløp.barnAktører, utenlandskPeriodebeløp2.barnAktører)
    }

    @Test
    fun `Skal lagre skjema-feltene`() {
        val søker = aktørRepository.save(randomAktør())
        val barn1 = aktørRepository.save(randomAktør())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val utenlandskPeriodebeløp =
            utenlandskPeriodebeløpRepository.save(
                lagUtenlandskPeriodebeløp(
                    behandlingId = behandling.id,
                    barnAktører = setOf(barn1),
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2021, 12),
                    beløp = BigDecimal.valueOf(1_234),
                    valutakode = "EUR",
                    intervall = Intervall.UKENTLIG,
                ),
            )

        val hentedeUtenlandskePeriodebeløp =
            utenlandskPeriodebeløpRepository.findByBehandlingId(behandlingId = behandling.id)

        assertEquals(1, hentedeUtenlandskePeriodebeløp.size)
        assertEquals(utenlandskPeriodebeløp, hentedeUtenlandskePeriodebeløp.first())
    }
}
