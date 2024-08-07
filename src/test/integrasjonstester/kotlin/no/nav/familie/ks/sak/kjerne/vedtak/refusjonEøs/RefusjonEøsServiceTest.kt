package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.vedtak.refusjonEøs

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.RefusjonEøsDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class RefusjonEøsServiceTest(
    @Autowired val refusjonEøsService: RefusjonEøsService,
    @Autowired val behandlingService: BehandlingService,
) : OppslagSpringRunnerTest() {
    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun kanLagreEndreOgSlette() {
        val refusjonEøs =
            RefusjonEøsDto(
                id = 0,
                fom = LocalDate.of(2020, Month.JANUARY, 1),
                tom = LocalDate.of(2021, Month.MAY, 31),
                refusjonsbeløp = 1234,
                land = "SE",
                refusjonAvklart = true,
            )

        val id = refusjonEøsService.leggTilRefusjonEøsPeriode(refusjonEøs = refusjonEøs, behandlingId = behandling.id)

        refusjonEøsService
            .hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it[0].id).isEqualTo(id) }
            .also { Assertions.assertThat(it[0].fom).isEqualTo("2020-01-01") }
            .also { Assertions.assertThat(it[0].tom).isEqualTo("2021-05-31") }

        refusjonEøsService.oppdaterRefusjonEøsPeriode(
            refusjonEøs =
                RefusjonEøsDto(
                    id = id,
                    fom = LocalDate.of(2020, Month.JANUARY, 1),
                    tom = LocalDate.of(2020, Month.MAY, 31),
                    refusjonsbeløp = 1,
                    land = "NL",
                    refusjonAvklart = false,
                ),
            id = id,
        )

        refusjonEøsService
            .hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it[0].id).isEqualTo(id) }
            .also { Assertions.assertThat(it[0].tom).isEqualTo("2020-05-31") }
            .also { Assertions.assertThat(it[0].refusjonsbeløp).isEqualTo(1) }
            .also { Assertions.assertThat(it[0].land).isEqualTo("NL") }
            .also { Assertions.assertThat(it[0].refusjonAvklart).isEqualTo(false) }

        val refusjonEøs2 =
            RefusjonEøsDto(
                id = 0,
                fom = LocalDate.of(2019, Month.DECEMBER, 1),
                tom = LocalDate.of(2019, Month.DECEMBER, 31),
                refusjonsbeløp = 100,
                land = "DK",
                refusjonAvklart = false,
            )

        val id2 = refusjonEøsService.leggTilRefusjonEøsPeriode(refusjonEøs = refusjonEøs2, behandlingId = behandling.id)

        refusjonEøsService
            .hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(2) }
            .also { Assertions.assertThat(it[0].id).isEqualTo(id2) }

        refusjonEøsService.fjernRefusjonEøsPeriode(id = id, behandlingId = behandling.id)

        refusjonEøsService
            .hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(1) }
            .also { Assertions.assertThat(it[0].id).isEqualTo(id2) }
    }
}
