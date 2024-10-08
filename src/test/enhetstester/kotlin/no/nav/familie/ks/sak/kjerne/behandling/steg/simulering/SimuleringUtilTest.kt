package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import io.mockk.mockk
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.ks.sak.api.mapper.SimuleringMapper.tilSimuleringDto
import no.nav.familie.ks.sak.api.mapper.SimuleringMapper.tilSimuleringsPerioder
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringPostering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SimuleringUtilTest {
    private fun mockØkonomiSimuleringMottaker(
        id: Long = 0,
        mottakerNummer: String? = randomFnr(),
        mottakerType: MottakerType = MottakerType.BRUKER,
        behandling: Behandling = mockk(relaxed = true),
        økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = listOf(mockVedtakSimuleringPostering()),
    ) = ØkonomiSimuleringMottaker(id, mottakerNummer, mottakerType, behandling, økonomiSimuleringPostering)

    private fun mockVedtakSimuleringPostering(
        økonomiSimuleringMottaker: ØkonomiSimuleringMottaker = mockk(relaxed = true),
        beløp: Int = 0,
        fagOmrådeKode: FagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
        fom: LocalDate = LocalDate.now().minusMonths(1),
        tom: LocalDate = LocalDate.now().minusMonths(1),
        betalingType: BetalingType = BetalingType.DEBIT,
        posteringType: PosteringType = PosteringType.YTELSE,
        forfallsdato: LocalDate = LocalDate.now().minusMonths(1),
        utenInntrekk: Boolean = false,
    ) = ØkonomiSimuleringPostering(
        økonomiSimuleringMottaker = økonomiSimuleringMottaker,
        fagOmrådeKode = fagOmrådeKode,
        fom = fom,
        tom = tom,
        betalingType = betalingType,
        beløp = beløp.toBigDecimal(),
        posteringType = posteringType,
        forfallsdato = forfallsdato,
        utenInntrekk = utenInntrekk,
    )

    fun mockVedtakSimuleringPosteringer(
        måned: YearMonth = YearMonth.of(2021, 1),
        antallMåneder: Int = 1,
        beløp: Int = 5000,
        posteringstype: PosteringType = PosteringType.YTELSE,
        betalingstype: BetalingType = if (beløp >= 0) BetalingType.DEBIT else BetalingType.KREDIT,
    ): List<ØkonomiSimuleringPostering> =
        MutableList(antallMåneder) { index ->
            ØkonomiSimuleringPostering(
                økonomiSimuleringMottaker = mockk(relaxed = true),
                fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
                fom = måned.plusMonths(index.toLong()).atDay(1),
                tom = måned.plusMonths(index.toLong()).atEndOfMonth(),
                betalingType = betalingstype,
                beløp = beløp.toBigDecimal(),
                posteringType = posteringstype,
                forfallsdato = måned.plusMonths(index.toLong()).atEndOfMonth(),
                utenInntrekk = false,
            )
        }

    @Test
    fun `Test 'hentNyttBeløpIPeriode()', 'hentTidligereUtbetaltIPeriode()' og 'hentResultatIPeriode()' for simuleringsperiode uten feilutbetaling`() {
        val vedtaksimuleringPosteringer =
            listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
            )

        Assertions.assertEquals(BigDecimal.valueOf(200), hentNyttBeløpIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(198), hentTidligereUtbetaltIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(2), hentResultatIPeriode(vedtaksimuleringPosteringer))
    }

    @Test
    fun `Test 'hentNyttBeløpIPeriode()', 'hentTidligereUtbetaltIPeriode()' og 'hentResultatIPeriode()' for simuleringsperiode med feilutbetaling`() {
        val økonomiSimuleringPosteringer =
            listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
            )

        Assertions.assertEquals(BigDecimal.valueOf(4), hentNyttBeløpIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(198), hentTidligereUtbetaltIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(-196), hentResultatIPeriode(økonomiSimuleringPosteringer))
    }

    @Test
    fun `Test 'hentNyttBeløpIPeriode()', 'hentTidligereUtbetaltIPeriode()' og 'hentResultatIPeriode()' for simuleringsperiode med reduksjon i feilutbetaling`() {
        val økonomiSimuleringPosteringer =
            listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.FEILUTBETALING),
            )

        Assertions.assertEquals(BigDecimal.valueOf(200), hentNyttBeløpIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(197), hentTidligereUtbetaltIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(3), hentResultatIPeriode(økonomiSimuleringPosteringer))
    }

    private val økonomiSimuleringPosteringerMedNegativFeilutbetaling =
        listOf(
            mockVedtakSimuleringPostering(beløp = -500, posteringType = PosteringType.FEILUTBETALING),
            mockVedtakSimuleringPostering(beløp = -2000, posteringType = PosteringType.YTELSE),
            mockVedtakSimuleringPostering(beløp = 3000, posteringType = PosteringType.YTELSE),
            mockVedtakSimuleringPostering(beløp = -500, posteringType = PosteringType.YTELSE),
        )

    @Test
    fun `tilSimulertingDto - Total etterbetaling skal bli summen av ytelsene i periode med negativ feilutbetaling`() {
        val økonomiSimuleringMottaker =
            mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedNegativFeilutbetaling)
        val simuleringDto = listOf(økonomiSimuleringMottaker).tilSimuleringDto()

        Assertions.assertEquals(BigDecimal.valueOf(500), simuleringDto.etterbetaling)
    }

    @Test
    fun `tilSimuleringDto - Total feilutbetaling skal bli 0 i periode med negativ feilutbetaling`() {
        val økonomiSimuleringMottaker =
            mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedNegativFeilutbetaling)
        val simuleringDto = listOf(økonomiSimuleringMottaker).tilSimuleringDto()

        Assertions.assertEquals(BigDecimal.valueOf(0), simuleringDto.feilutbetaling)
    }

    @Test
    fun `tilSimuleringDto - Skal gi 0 etterbetaling og sum feilutbetaling ved positiv feilutbetaling`() {
        val økonomiSimuleringPosteringerMedPositivFeilutbetaling =
            listOf(
                mockVedtakSimuleringPostering(beløp = 500, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = -2000, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 3000, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -500, posteringType = PosteringType.YTELSE),
            )

        val økonomiSimuleringMottaker =
            mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedPositivFeilutbetaling)
        val simuleringDto = listOf(økonomiSimuleringMottaker).tilSimuleringDto()

        Assertions.assertEquals(BigDecimal.valueOf(0), simuleringDto.etterbetaling)
        Assertions.assertEquals(BigDecimal.valueOf(500), simuleringDto.feilutbetaling)
    }

    @Test
    fun `hentEtterbetalingIPeriode - Test at bare perioder med passert forfalldato blir inludert i summeringen av etterbetaling`() {
        val vedtaksimuleringPosteringer =
            listOf(
                mockVedtakSimuleringPostering(
                    beløp = 100,
                    posteringType = PosteringType.YTELSE,
                    forfallsdato = LocalDate.now().plusDays(1),
                ),
                mockVedtakSimuleringPostering(
                    beløp = 200,
                    posteringType = PosteringType.YTELSE,
                    forfallsdato = LocalDate.now().minusDays(1),
                ),
            )

        Assertions.assertEquals(
            BigDecimal.valueOf(200),
            hentEtterbetalingIPeriode(
                vedtaksimuleringPosteringer,
                LocalDate.now(),
            ),
        )
    }

    /*
    De neste testene antar at brukeren går gjennom følgende for ÉN periode:
    - Førstegangsbehandling gir ytelse på kr 10 000
    - Revurdering reduserer ytelse fra kr 10 000 til kr 2 000, dvs kr 8 000 feilutbetalt
    - Revurdering øker ytelse fra kr 2 000 til kr 3 000, dvs feilutbetaling reduseres
    - Revurdering øker ytelse fra kr 3 000 tik kr 12 000, dvs feilutbetaling nulles ut, og etterbetaling skjer
     */
    @Test
    fun `ytelse på 10000 korrigert til 2000`() {
        val redusertYtelseTil2000 =
            listOf(
                mockVedtakSimuleringPostering(
                    beløp = -10000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
                // Forrige
                mockVedtakSimuleringPostering(
                    beløp = 2000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
                // Ny
                mockVedtakSimuleringPostering(
                    beløp = 8000,
                    posteringType = PosteringType.FEILUTBETALING,
                    betalingType = BetalingType.DEBIT,
                ),
                // Feilutbetaling
                mockVedtakSimuleringPostering(
                    beløp = -8000,
                    posteringType = PosteringType.MOTP,
                    betalingType = BetalingType.KREDIT,
                ),
                // "Nuller ut" Feilutbetalingen
                mockVedtakSimuleringPostering(
                    beløp = 8000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
                // "Nuller ut" forrige og ny
            )

        val økonomiSimuleringMottakere =
            listOf(mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = redusertYtelseTil2000))
        val simuleringsperioder = økonomiSimuleringMottakere.tilSimuleringsPerioder()
        val oppsummering = økonomiSimuleringMottakere.tilSimuleringDto()

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(10000.toBigDecimal())
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(2000.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(-8000.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(8000.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(0.toBigDecimal())
    }

    @Test
    fun `ytelse på 2000 korrigert til 3000`() {
        val øktYtelseFra2000Til3000 =
            listOf(
                mockVedtakSimuleringPostering(
                    beløp = -2000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
                mockVedtakSimuleringPostering(
                    beløp = 3000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
                mockVedtakSimuleringPostering(
                    beløp = -1000,
                    posteringType = PosteringType.FEILUTBETALING,
                    betalingType = BetalingType.KREDIT,
                ),
                // Reduser feilutbetaling
                mockVedtakSimuleringPostering(
                    beløp = 1000,
                    posteringType = PosteringType.MOTP,
                    betalingType = BetalingType.DEBIT,
                ),
                mockVedtakSimuleringPostering(
                    beløp = -1000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = øktYtelseFra2000Til3000))
        val simuleringsperioder = økonomiSimuleringMottakere.tilSimuleringsPerioder()
        val oppsummering = økonomiSimuleringMottakere.tilSimuleringDto()

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(2000.toBigDecimal())
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(3000.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(1000.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(0.toBigDecimal())
    }

    @Test
    fun `ytelse på 3000 korrigert til 12000`() {
        val øktYtelseFra3000Til12000 =
            listOf(
                mockVedtakSimuleringPostering(
                    beløp = -3000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
                mockVedtakSimuleringPostering(
                    beløp = 12000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
                mockVedtakSimuleringPostering(
                    beløp = -7000,
                    posteringType = PosteringType.FEILUTBETALING,
                    betalingType = BetalingType.KREDIT,
                ),
                // Reduser feilutb
                mockVedtakSimuleringPostering(
                    beløp = 7000,
                    posteringType = PosteringType.MOTP,
                    betalingType = BetalingType.DEBIT,
                ),
                mockVedtakSimuleringPostering(
                    beløp = -7000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = øktYtelseFra3000Til12000))
        val simuleringsperioder = økonomiSimuleringMottakere.tilSimuleringsPerioder()
        val oppsummering = økonomiSimuleringMottakere.tilSimuleringDto()

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(3000.toBigDecimal())
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(12000.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(9000.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(2000.toBigDecimal())
    }

    /*
    De neste testene antar at brukeren går gjennom følgende førstegangsbehandling og revurderinger i november 2021:
    2021	Feb	    Mar	    Apr	    Mai	    Jun	    Jul	    Aug	    Sep	    Okt	    Nov
    18/11	17153	17153	17153	18195	18195	18195	18195	18195	18195
    22/11	17153	17153   17153   17257	17257	17257	17257   18195   18195
    23/11	17341	17341	17341	18382	18382	18382	18382	18382	18382	18382
     */

    @Test
    fun `førstegangsbehandling 18 nov`() {
        val førstegangsbehandling18nov =
            mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, 17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 6, 18_195, PosteringType.YTELSE)

        val økonomiSimuleringMottakere =
            listOf(mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = førstegangsbehandling18nov))
        val oppsummering = økonomiSimuleringMottakere.tilSimuleringDto()

        assertThat(oppsummering.feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(160_629.toBigDecimal())
    }

    @Test
    fun `revurdering 22 nov`() {
        val revurering22nov =
            // Forrige ytelse
            mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, -17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 6, -18_195, PosteringType.YTELSE) +
                // Ny ytelse
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, 17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 17_257, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 9), 2, 18_195, PosteringType.YTELSE) +
                // Feilutbetaling
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 938, PosteringType.FEILUTBETALING) +
                // Motpost feilutbetaling
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -938, PosteringType.MOTP) +
                // Teknisk postering
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 938, PosteringType.YTELSE)

        val økonomiSimuleringMottakere =
            listOf(mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = revurering22nov))
        val oppsummering = økonomiSimuleringMottakere.tilSimuleringDto()

        assertThat(oppsummering.feilutbetaling).isEqualTo(3_752.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(0.toBigDecimal())
    }

    @Test
    fun `revurdering 23 nov`() {
        val revurdering23nov =
            // Forrige ytelse
            mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, -17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -17_257, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 9), 2, -18_195, PosteringType.YTELSE) +
                // Ny ytelse
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, 17_341, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 7, 18_382, PosteringType.YTELSE) +
                // Teknisk postering
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -938, PosteringType.YTELSE) +
                // Reduser feilutbetaling til null
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -938, PosteringType.FEILUTBETALING) +
                // Motpost feilutbetaling
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 938, PosteringType.MOTP)

        val økonomiSimuleringMottakere =
            listOf(mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = revurdering23nov))
        val simuleringsperioder = økonomiSimuleringMottakere.tilSimuleringsPerioder()
        val oppsummering = økonomiSimuleringMottakere.tilSimuleringDto()

        (3..6).forEach {
            assertThat(simuleringsperioder[it].tidligereUtbetalt).isEqualTo(17_257.toBigDecimal())
            assertThat(simuleringsperioder[it].resultat).isEqualTo(1_125.toBigDecimal())
        }

        assertThat(oppsummering.feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(20_068.toBigDecimal()) // 1 686 hvis revurderingen ble gjort nov 2021, ikke "i dag"
    }

    @Test
    fun `hentManuellPosteringIPeriode skal returnere sum av manuelle posteringer`() {
        val økonomiSimuleringPosteringer =
            listOf(
                mockVedtakSimuleringPostering(beløp = 2000, posteringType = PosteringType.YTELSE, fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE),
                mockVedtakSimuleringPostering(beløp = 500, posteringType = PosteringType.YTELSE, fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE_INFOTRYGD_MANUELT),
                mockVedtakSimuleringPostering(beløp = 500, posteringType = PosteringType.YTELSE, fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE_INFOTRYGD_MANUELT),
                mockVedtakSimuleringPostering(beløp = 500, posteringType = PosteringType.YTELSE, fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE_MANUELT),
                mockVedtakSimuleringPostering(beløp = 200, posteringType = PosteringType.FEILUTBETALING, fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE_INFOTRYGD_MANUELT),
                mockVedtakSimuleringPostering(beløp = 200, posteringType = PosteringType.FEILUTBETALING, fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE_INFOTRYGD_MANUELT),
                mockVedtakSimuleringPostering(beløp = 200, posteringType = PosteringType.FEILUTBETALING, fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE_MANUELT),
            )

        Assertions.assertEquals(BigDecimal.valueOf(900), hentManuellPosteringIPeriode(økonomiSimuleringPosteringer))
    }
}
