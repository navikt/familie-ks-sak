package no.nav.familie.ks.sak.integrasjon.økonomi.internkonsistensavstemming

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.integrasjoner.økonomi.internkonsistensavstemming.erForskjellMellomAndelerOgOppdrag
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InternKonsistensavstemmingUtilTest {

    @Test
    fun `Skal ignorere forskjeller før første utbetalingsoppdragsperiode`() {
        val andelerSisteVedtatteBehandling = listOf(
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2021-12"),
                stønadTom = YearMonth.parse("2021-12"),
                sats = 1654
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2022-01"),
                stønadTom = YearMonth.parse("2023-02"),
                sats = 1676
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2023-03"),
                stønadTom = YearMonth.parse("2027-10"),
                sats = 1723
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2027-11"),
                stønadTom = YearMonth.parse("2039-10"),
                sats = 1083
            )
        )
        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(mockUtbetalingsoppdrag)

        Assertions.assertFalse(
            erForskjellMellomAndelerOgOppdrag(
                andelerSisteVedtatteBehandling,
                utbetalingsoppdrag,
                0L
            )
        )
    }

    @Test
    fun `skal se at vi mangler andel for oppdragsperiode`() {
        val andelerSisteVedtatteBehandling = listOf(
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2021-12"),
                stønadTom = YearMonth.parse("2021-12"),
                sats = 1654
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2022-01"),
                stønadTom = YearMonth.parse("2023-02"),
                sats = 1676
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2023-03"),
                stønadTom = YearMonth.parse("2027-10"),
                sats = 1723
            )
        )
        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(mockUtbetalingsoppdrag)

        Assertions.assertTrue(erForskjellMellomAndelerOgOppdrag(andelerSisteVedtatteBehandling, utbetalingsoppdrag, 0L))
    }

    @Test
    fun `skal se at andel og oppdragsperiode har forskjellig beløp`() {
        val andelerSisteVedtatteBehandling = listOf(
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2021-12"),
                stønadTom = YearMonth.parse("2021-12"),
                sats = 1654
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2022-01"),
                stønadTom = YearMonth.parse("2023-02"),
                sats = 1676
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2023-03"),
                stønadTom = YearMonth.parse("2027-10"),
                sats = 1723
            ),
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse("2027-11"),
                stønadTom = YearMonth.parse("2039-10"),
                sats = 9999
            )
        )
        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(mockUtbetalingsoppdrag)

        Assertions.assertTrue(erForskjellMellomAndelerOgOppdrag(andelerSisteVedtatteBehandling, utbetalingsoppdrag, 0L))
    }

    @Test
    fun `skal ikke si det er forskjell ved riktig utbetalingsoppdrag når det er flere kjeder`() {
        val andelStringer = listOf(
            "2021-05,2021-08,1354",
            "2021-09,2021-12,1654",

            "2022-01,2023-02,1676",
            "2023-03,2024-11,1723",
            "2024-12,2036-11,1083",

            "2021-05,2023-02,1054,UTVIDET_BARNETRYGD",
            "2023-03,2036-11,2489,UTVIDET_BARNETRYGD"
        )

        val andelerSisteVedtatteBehandling = andelStringer.map { it.split(",") }.map {
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse(it[0]),
                stønadTom = YearMonth.parse(it[1]),
                sats = it[2].toInt(),
            )
        }

        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(utbetalingsoppdragMockMedUtvidet)

        Assertions.assertFalse(
            erForskjellMellomAndelerOgOppdrag(
                andelerSisteVedtatteBehandling,
                utbetalingsoppdrag,
                0L
            )
        )
    }

    @Test
    fun `skal ikke si det er forskjell ved riktig utbetalingsoppdrag når kun ett barn ble endret i siste behandling som iverksatte`() {
        val andelStringer = listOf(
            "2022-05,2022-06,1676,2554733867704", // barn 1, ble laget i siste behandling som iverksatte
            "2022-07,2028-03,838,2554733867704", // barn 1, ble laget i siste behandling som iverksatte
            "2028-04,2040-03,527,2554733867704", // barn 1, ble laget i siste behandling som iverksatte

            "2022-07,2028-05,1676,2909658383415", // barn 2, ble laget før siste behandling som iverksatte
            "2028-06,2040-05,1054,2909658383415" // barn 2, ble laget før siste behandling som iverksatte
        )

        val andelerSisteVedtatteBehandling = andelStringer.map { it.split(",") }.map {
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse(it[0]),
                stønadTom = YearMonth.parse(it[1]),
                sats = it[2].toInt(),
                aktør = Aktør(it[3])
            )
        }

        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(utbetalingsoppdragMockEndringKunEttBarn)

        Assertions.assertFalse(
            erForskjellMellomAndelerOgOppdrag(
                andelerSisteVedtatteBehandling,
                utbetalingsoppdrag,
                0L
            )
        )
    }

    @Test
    fun `skal ikke si det er forskjell ved opphør`() {
        val andelStringer = listOf(
            "2016-02,2019-02,970,2193974415300",
            "2019-03,2020-08,1054,2193974415300",
            "2020-09,2021-08,1354,2193974415300",
            "2021-09,2021-12,1654,2193974415300",
            "2022-01,2022-02,1054,2193974415300",
            "2012-06,2019-02,970,2094407059820",
            "2019-03,2022-01,1054,2094407059820"
        )

        val andelerSisteVedtatteBehandling = andelStringer.map { it.split(",") }.map {
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse(it[0]),
                stønadTom = YearMonth.parse(it[1]),
                sats = it[2].toInt(),
                aktør = Aktør(it[3])
            )
        }

        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(utbetalingsoppdragMockOpphør)

        Assertions.assertFalse(
            erForskjellMellomAndelerOgOppdrag(
                andelerSisteVedtatteBehandling,
                utbetalingsoppdrag,
                0L
            )
        )
    }

    @Test
    fun `skal si andelene og utbetalingene er like dersom andel blir splittet opp, men betaler ut det samme i periodene`() {
        val andelStringer = listOf(
            "2021-01,2022-03,1054",
            "2022-04,2022-06,1054"
        )

        val aktør = randomAktør()
        val behandling = lagBehandling()

        val andelerSisteVedtatteBehandling = andelStringer.map { it.split(",") }.map {
            lagAndelTilkjentYtelse(
                stønadFom = YearMonth.parse(it[0]),
                stønadTom = YearMonth.parse(it[1]),
                sats = it[2].toInt(),
                aktør = aktør,
                behandling = behandling
            )
        }

        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(utbetalingsoppdragMockEnPeriode)

        Assertions.assertFalse(
            erForskjellMellomAndelerOgOppdrag(
                andelerSisteVedtatteBehandling,
                utbetalingsoppdrag,
                0L
            )
        )
    }
}

private val mockUtbetalingsoppdrag = """
    {
      "kodeEndring": "ENDR",
      "fagSystem": "KS",
      "saksnummer": "1",
      "aktoer": "1",
      "saksbehandlerId": "VL",
      "avstemmingTidspunkt": "2023-02-08T16:12:56.200284803",
      "utbetalingsperiode": [
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2022-01-01"
          },
          "periodeId": 2,
          "forrigePeriodeId": 1,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2027-11-01",
          "vedtakdatoTom": "2039-10-31",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 3,
          "forrigePeriodeId": 2,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2022-01-01",
          "vedtakdatoTom": "2023-02-28",
          "sats": 1676,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 4,
          "forrigePeriodeId": 3,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2023-03-01",
          "vedtakdatoTom": "2027-10-31",
          "sats": 1723,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 5,
          "forrigePeriodeId": 4,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2027-11-01",
          "vedtakdatoTom": "2039-10-31",
          "sats": 1083,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        }
      ],
      "gOmregning": false
    }
""".trimIndent()

private val utbetalingsoppdragMockMedUtvidet = """
    {
      "kodeEndring": "ENDR",
      "fagSystem": "KS",
      "saksnummer": "200028561",
      "aktoer": "02416938515",
      "saksbehandlerId": "VL",
      "avstemmingTidspunkt": "2023-02-08T15:57:38.341011606",
      "utbetalingsperiode": [
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2022-01-01"
          },
          "periodeId": 3,
          "forrigePeriodeId": 2,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2024-12-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2021-05-01"
          },
          "periodeId": 4,
          "forrigePeriodeId": null,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2021-05-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 5,
          "forrigePeriodeId": 3,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2022-01-01",
          "vedtakdatoTom": "2023-02-28",
          "sats": 1676,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 6,
          "forrigePeriodeId": 5,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2023-03-01",
          "vedtakdatoTom": "2024-11-30",
          "sats": 1723,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 7,
          "forrigePeriodeId": 6,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2024-12-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 1083,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 8,
          "forrigePeriodeId": 4,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2021-05-01",
          "vedtakdatoTom": "2023-02-28",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 9,
          "forrigePeriodeId": 8,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "KS",
          "vedtakdatoFom": "2023-03-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 2489,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        }
      ],
      "gOmregning": false
    }
""".trimIndent()

val utbetalingsoppdragMockEndringKunEttBarn = """
    {
      "kodeEndring": "ENDR",
      "fagSystem": "KS",
      "saksnummer": "200002102",
      "aktoer": "07118905215",
      "saksbehandlerId": "Z994623",
      "avstemmingTidspunkt": "2022-08-16T08:49:13.565620862",
      "utbetalingsperiode": [
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2022-05-01"
          },
          "periodeId": 3,
          "forrigePeriodeId": 2,
          "datoForVedtak": "2022-08-16",
          "klassifisering": "KS",
          "vedtakdatoFom": "2028-04-01",
          "vedtakdatoTom": "2040-03-31",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "07118905215",
          "behandlingId": 100098303,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 4,
          "forrigePeriodeId": 3,
          "datoForVedtak": "2022-08-16",
          "klassifisering": "KS",
          "vedtakdatoFom": "2022-05-01",
          "vedtakdatoTom": "2022-06-30",
          "sats": 1676,
          "satsType": "MND",
          "utbetalesTil": "07118905215",
          "behandlingId": 100098303,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 5,
          "forrigePeriodeId": 4,
          "datoForVedtak": "2022-08-16",
          "klassifisering": "KS",
          "vedtakdatoFom": "2022-07-01",
          "vedtakdatoTom": "2028-03-31",
          "sats": 838,
          "satsType": "MND",
          "utbetalesTil": "07118905215",
          "behandlingId": 100098303,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 6,
          "forrigePeriodeId": 5,
          "datoForVedtak": "2022-08-16",
          "klassifisering": "KS",
          "vedtakdatoFom": "2028-04-01",
          "vedtakdatoTom": "2040-03-31",
          "sats": 527,
          "satsType": "MND",
          "utbetalesTil": "07118905215",
          "behandlingId": 100098303,
          "utbetalingsgrad": null
        }
      ],
      "gOmregning": false
    }
""".trimIndent()

val utbetalingsoppdragMockOpphør = """
    {
      "kodeEndring": "ENDR",
      "fagSystem": "KS",
      "saksnummer": "200001701",
      "aktoer": "25118604604",
      "saksbehandlerId": "Z991771",
      "avstemmingTidspunkt": "2022-08-09T14:42:14.977345689",
      "utbetalingsperiode": [
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2022-08-01"
          },
          "periodeId": 10,
          "forrigePeriodeId": 9,
          "datoForVedtak": "2022-08-09",
          "klassifisering": "KS",
          "vedtakdatoFom": "2025-02-01",
          "vedtakdatoTom": "2037-01-31",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "25118604604",
          "behandlingId": 100096955,
          "utbetalingsgrad": null
        }
      ],
      "gOmregning": false
    }
""".trimIndent()

val utbetalingsoppdragMockEnPeriode = """
    {
      "kodeEndring": "NY",
      "fagSystem": "KONT",
      "saksnummer": "1",
      "aktoer": "1",
      "saksbehandlerId": "",
      "avstemmingTidspunkt": "2021-12-23T08:11:33.333476714",
      "utbetalingsperiode": [
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 0,
          "forrigePeriodeId": null,
          "datoForVedtak": "2021-12-23",
          "klassifisering": "KS",
          "vedtakdatoFom": "2021-01-01",
          "vedtakdatoTom": "2022-06-30",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        }
      ]
    }
""".trimIndent()
