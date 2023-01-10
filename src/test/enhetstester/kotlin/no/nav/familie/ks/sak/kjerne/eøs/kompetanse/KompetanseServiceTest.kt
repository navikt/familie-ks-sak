package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.ks.sak.api.dto.tilKompetanseDto
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class KompetanseServiceTest {

    private val kompetanseRepository: EøsSkjemaRepository<Kompetanse> = mockPeriodeBarnSkjemaRepository()
    private val personidentService: PersonidentService = mockk()
    private val kompetanseService: KompetanseService = KompetanseService(
        kompetanseRepository = kompetanseRepository,
        kompetanseEndringsAbonnenter = emptyList(),
        personidentService = personidentService
    )

    private val barn1 = randomAktør()
    private val barn2 = randomAktør()
    private val barn3 = randomAktør()
    private val behandlingId = 10001L

    @BeforeEach
    fun init() {
        every { personidentService.hentAktør(barn1.aktivFødselsnummer()) } returns barn1
        every { personidentService.hentAktør(barn2.aktivFødselsnummer()) } returns barn2
        every { personidentService.hentAktør(barn3.aktivFødselsnummer()) } returns barn3
        kompetanseRepository.deleteAll()
    }

    @Test
    fun `oppdaterKompetanse bare reduksjon av periode skal ikke føre til endring i kompetansen`() {
        val eksisterendeKompetanse = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 8),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            barnAktører = setOf(barn1)
        ).lagreTil(kompetanseRepository)

        val oppdateresKompetanse = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 7),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            barnAktører = setOf(barn1)
        )
        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())
        assertThat(listOf(eksisterendeKompetanse)).containsExactlyInAnyOrderElementsOf(
            kompetanseService.hentKompetanser(behandlingId)
        )
    }

    @Test
    fun `oppdaterKompetanse oppdatering som splitter kompetanse fulgt av sletting skal returnere til utgangspunktet`() {
        val eksisterendeKompetanse = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 9),
            barnAktører = setOf(barn1, barn2, barn3)
        ).lagreTil(kompetanseRepository)

        val oppdateresKompetanse = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 4),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            barnAktører = setOf(barn2, barn3)
        )

        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 4)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn1, barn2, barn3),
            hentetKompetanse = kompetanser[0]
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 4),
            barnAktører = setOf(barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            hentetKompetanse = kompetanser[1]
        )
        assertKompetanse( // periode med null resultat for barn1 i denne perioden
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 4),
            barnAktører = setOf(barn1),
            hentetKompetanse = kompetanser[2]
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 5),
            tom = YearMonth.of(2022, 9),
            barnAktører = setOf(barn1, barn2, barn3),
            hentetKompetanse = kompetanser[3]
        )

        val kompetanseSomSkalSlettes = kompetanseService.hentKompetanser(behandlingId)
            .first { it == oppdateresKompetanse }
        kompetanseService.slettKompetanse(kompetanseSomSkalSlettes.id)

        assertThat(listOf(eksisterendeKompetanse)).containsExactlyInAnyOrderElementsOf(
            kompetanseService.hentKompetanser(behandlingId)
        )
    }

    @Test
    fun `oppdatering som endrer deler av en kompetanse, skal resultarere i en splitt`() {
        val eksisterendeKompetanse1 = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 4),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND
        )
        val eksisterendeKompetanse2 = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 9),
            barnAktører = setOf(barn2, barn3)
        )
        val eksisterendeKompetanse3 = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 4),
            tom = YearMonth.of(2022, 7),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND
        )
        listOf(eksisterendeKompetanse1, eksisterendeKompetanse2, eksisterendeKompetanse3).lagreTil(kompetanseRepository)

        val oppdateresKompetanse = lagKompetanse(
            behandlingId = behandlingId,
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 5),
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND
        )
        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 5)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[0]
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn2, barn3),
            hentetKompetanse = kompetanser[1]
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn2, barn3),
            hentetKompetanse = kompetanser[1]
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 5),
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            hentetKompetanse = kompetanser[2]
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 6),
            tom = YearMonth.of(2022, 7),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[3]
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 6),
            tom = YearMonth.of(2022, 9),
            barnAktører = setOf(barn2, barn3),
            hentetKompetanse = kompetanser[4]
        )
    }

    private fun assertKompetanse(
        fom: YearMonth,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
        resultat: KompetanseResultat? = null,
        hentetKompetanse: Kompetanse
    ) {
        assertThat(fom).isEqualTo(hentetKompetanse.fom)
        assertThat(tom).isEqualTo(hentetKompetanse.tom)
        assertThat(barnAktører).containsExactlyInAnyOrderElementsOf(hentetKompetanse.barnAktører)
        assertThat(resultat).isEqualTo(hentetKompetanse.resultat)
    }
}
