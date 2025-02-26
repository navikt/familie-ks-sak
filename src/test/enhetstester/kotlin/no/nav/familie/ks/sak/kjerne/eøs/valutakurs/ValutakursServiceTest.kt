package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medBehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.mockEøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.jan
import no.nav.familie.ks.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ks.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class ValutakursServiceTest {
    private val valutakursRepository: EøsSkjemaRepository<Valutakurs> = mockEøsSkjemaRepository()
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository = mockk()

    private val valutakursService =
        ValutakursService(
            valutakursRepository,
            emptyList(),
        )

    private val tilpassValutakurserTilUtenlandskePeriodebeløpService =
        TilpassValutakurserTilUtenlandskePeriodebeløpService(
            valutakursRepository,
            utenlandskPeriodebeløpRepository,
            emptyList(),
        )

    @BeforeEach
    fun init() {
        valutakursRepository.deleteAll()
    }

    @Test
    fun `skal tilpasse utenlandsk periodebeløp til endrede kompetanser`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())
        val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).førsteDagIInneværendeMåned())

        ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("4444   555 666", "EUR", barn1, barn2, barn3)
            .lagreTil(valutakursRepository)

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
                .medBeløp("  777777777", "EUR", "N", barn1)
                .bygg()

        every { utenlandskPeriodebeløpRepository.findByBehandlingId(behandlingId.id) } returns utenlandskePeriodebeløp

        tilpassValutakurserTilUtenlandskePeriodebeløpService.tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId)

        val faktiskeValutakurser = valutakursService.hentValutakurser(behandlingId)

        val forventedeValutakurser =
            ValutakursBuilder(jan(2020), behandlingId)
                .medKurs("  44$$$555$", "EUR", barn1)
                .bygg()

        assertThat(faktiskeValutakurser)
            .containsAll(forventedeValutakurser)
            .hasSize(forventedeValutakurser.size)
    }

    @Test
    fun `slette et valutakurs-skjema skal resultere i et skjema uten innhold, men som fortsatt har valutakoden`() {
        val behandlingId = BehandlingId(10L)

        val lagretValutakurs =
            valutakursRepository
                .saveAll(
                    listOf(
                        Valutakurs(
                            fom = YearMonth.now(),
                            tom = YearMonth.now(),
                            barnAktører = setOf(tilfeldigPerson().aktør),
                            valutakursdato = LocalDate.now(),
                            valutakode = "EUR",
                            kurs = BigDecimal.TEN,
                        ),
                    ).medBehandlingId(behandlingId),
                ).single()

        valutakursService.slettValutakurs(lagretValutakurs.id)

        val faktiskValutakurs = valutakursService.hentValutakurser(behandlingId).single()

        assertEquals("EUR", faktiskValutakurs.valutakode)
        assertNull(faktiskValutakurs.valutakursdato)
        assertNull(faktiskValutakurs.kurs)

        assertEquals(lagretValutakurs.fom, faktiskValutakurs.fom)
        assertEquals(lagretValutakurs.tom, faktiskValutakurs.tom)
        assertEquals(lagretValutakurs.barnAktører, faktiskValutakurs.barnAktører)
    }

    @Test
    fun `skal kunne lukke åpen valutakurs ved å sende inn identisk skjema med til-og-med-dato`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        // Åpen (til-og-med er null) valutakurs for ett barn
        ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("4>", "EUR", barn1)
            .lagreTil(valutakursRepository)

        // Endrer kun til-og-med dato fra uendelig (null) til en gitt dato
        val oppdatertKompetanse = valutakurs(jan(2020), "444", "EUR", barn1)
        valutakursService.oppdaterValutakurs(behandlingId, oppdatertKompetanse)

        // Forventer skjema uten innhold (MEN MED VALUTAKODE) fra oppdatert dato og fremover
        val forventedeValutakurser =
            ValutakursBuilder(jan(2020), behandlingId)
                .medKurs("444$>", "EUR", barn1)
                .bygg()

        val faktiskeValutakurser = valutakursService.hentValutakurser(behandlingId)

        assertThat(faktiskeValutakurser)
            .containsAll(forventedeValutakurser)
            .hasSize(forventedeValutakurser.size)
    }
}

fun valutakurs(
    tidspunkt: YearMonth,
    s: String,
    valutakode: String,
    vararg barn: Person,
) = ValutakursBuilder(tidspunkt).medKurs(s, valutakode, *barn).bygg().first()
