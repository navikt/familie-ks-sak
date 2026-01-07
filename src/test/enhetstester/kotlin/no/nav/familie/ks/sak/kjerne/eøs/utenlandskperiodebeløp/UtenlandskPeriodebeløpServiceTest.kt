package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.mockEøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService.Companion.BULGARSK_LEV
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.util.KompetanseBuilder
import no.nav.familie.ks.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

internal class UtenlandskPeriodebeløpServiceTest {
    private val utenlandskPeriodebeløpRepository: EøsSkjemaRepository<UtenlandskPeriodebeløp> =
        mockEøsSkjemaRepository()
    private val kompetanseRepository: KompetanseRepository = mockk()

    private val utenlandskPeriodebeløpService =
        UtenlandskPeriodebeløpService(
            utenlandskPeriodebeløpRepository,
            emptyList(),
        )

    private val tilpassUtenlandskePeriodebeløpTilKompetanserService =
        TilpassUtenlandskePeriodebeløpTilKompetanserService(
            utenlandskPeriodebeløpRepository,
            emptyList(),
            kompetanseRepository,
        )

    @BeforeEach
    fun init() {
        utenlandskPeriodebeløpRepository.deleteAll()
    }

    @Test
    fun `skal tilpasse utenlandsk periodebeløp til endrede kompetanser`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())
        val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())

        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("4444   555 666", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        val kompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse("SS   SSSSS", barn1, annenForeldersAktivitetsland = "N")
                .medKompetanse("  PPP", barn1, barn2, barn3, annenForeldersAktivitetsland = "N")
                .medKompetanse("--   ----", barn2, barn3, annenForeldersAktivitetsland = "N")
                .byggKompetanser()

        every { kompetanseRepository.findByBehandlingId(behandlingId.id) } returns kompetanser

        tilpassUtenlandskePeriodebeløpTilKompetanserService
            .tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId)

        val faktiskeUtenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val forventedeUtenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
                .medBeløp("44   --555", "EUR", "N", barn1)
                .bygg()

        assertThat(faktiskeUtenlandskePeriodebeløp).hasSize(forventedeUtenlandskePeriodebeløp.size).containsAll(forventedeUtenlandskePeriodebeløp)
    }

    @Test
    fun `Slette et utenlandskPeriodebeløp-skjema skal resultere i et skjema uten innhold, men som fortsatt har utbetalingsland`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())

        val lagretUtenlandskPeriodebeløp =
            UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
                .medBeløp("44444444", "EUR", "SE", barn1)
                .lagreTil(utenlandskPeriodebeløpRepository)
                .single()

        utenlandskPeriodebeløpService.slettUtenlandskPeriodebeløp(lagretUtenlandskPeriodebeløp.id)

        val faktiskUtenlandskPeriodebeløp =
            utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId).single()

        assertEquals("SE", faktiskUtenlandskPeriodebeløp.utbetalingsland)
        assertNull(faktiskUtenlandskPeriodebeløp.beløp)
        assertNull(faktiskUtenlandskPeriodebeløp.valutakode)
        assertNull(faktiskUtenlandskPeriodebeløp.intervall)
        assertNull(faktiskUtenlandskPeriodebeløp.kalkulertMånedligBeløp)
        assertEquals(lagretUtenlandskPeriodebeløp.fom, faktiskUtenlandskPeriodebeløp.fom)
        assertEquals(lagretUtenlandskPeriodebeløp.tom, faktiskUtenlandskPeriodebeløp.tom)
        assertEquals(lagretUtenlandskPeriodebeløp.barnAktører, faktiskUtenlandskPeriodebeløp.barnAktører)
    }

    @Test
    fun `Skal kunne lukke åpen utenlandskPeriodebeløp-skjema ved å sende inn identisk skjema med satt tom-dato`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())

        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("4>", "EUR", "SE", barn1)
            .medIntervall(Intervall.UKENTLIG)
            .lagreTil(utenlandskPeriodebeløpRepository)
            .single()

        // Oppdaterer UtenlandskPeriodeBeløp med identisk innhold, men med lukket tom for andre mnd.
        val oppdatertUtenlandskPeriodebeløp =
            UtenlandskPeriodebeløpBuilder(jan(2020))
                .medBeløp("44", "EUR", "SE", barn1)
                .medIntervall(Intervall.UKENTLIG)
                .bygg()
                .first()
        utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(behandlingId, oppdatertUtenlandskPeriodebeløp)

        // Forventer en liste på 2 elementer hvor det første dekker 2 mnd og det andre dekker fra mnd 3 og til uendelig (null). Det siste elementet skal ha beløp, valutakode og intervall satt til null, mens utbetalingsland skal være "SE".
        val faktiskUtenlandskPeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        assertNotNull(faktiskUtenlandskPeriodebeløp)

        assertEquals(2, faktiskUtenlandskPeriodebeløp.size)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).beløp)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).valutakode)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).intervall)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).kalkulertMånedligBeløp)
        assertEquals("SE", faktiskUtenlandskPeriodebeløp.elementAt(1).utbetalingsland)
    }

    @Test
    fun `Skal kaste funksjonell feil dersom fom ikke er satt`() {
        val feilmelding =
            assertThrows<FunksjonellFeil> {
                utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(
                    BehandlingId(1),
                    UtenlandskPeriodebeløp(
                        fom = null,
                        tom = null,
                        beløp = 1.0.toBigDecimal(),
                        valutakode = BULGARSK_LEV,
                    ),
                )
            }

        assertThat(feilmelding.message).isEqualTo("Fra og med dato på utenlandskperiode beløp må være satt")
    }

    @Test
    fun `Skal kaste funksjonell feil dersom det forsøkes å settes fom fra og med 1 januar 2026 med valutakode BGN`() {
        val feilmelding =
            assertThrows<FunksjonellFeil> {
                utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(
                    BehandlingId(1),
                    UtenlandskPeriodebeløp(
                        fom = YearMonth.of(2026, 1),
                        tom = null,
                        beløp = 1.0.toBigDecimal(),
                        valutakode = BULGARSK_LEV,
                    ),
                )
            }

        assertThat(feilmelding.message).isEqualTo("Bulgarske lev er ikke lenger gyldig valuta fra 01.01.26")
    }

    @Test
    fun `Skal kaste funksjonell feil dersom det forsøkes å settes tom fra og med 1 januar 2026 med valutakode BGN`() {
        val feilmelding =
            assertThrows<FunksjonellFeil> {
                utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(
                    BehandlingId(1),
                    UtenlandskPeriodebeløp(
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2026, 1),
                        beløp = 1.0.toBigDecimal(),
                        valutakode = BULGARSK_LEV,
                    ),
                )
            }

        assertThat(feilmelding.message).isEqualTo("Bulgarske lev er ikke lenger gyldig valuta fra 01.01.26")
    }
}
