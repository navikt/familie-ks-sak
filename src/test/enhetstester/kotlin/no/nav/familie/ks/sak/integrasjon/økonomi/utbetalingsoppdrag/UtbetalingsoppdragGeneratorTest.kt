package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.data.dato
import no.nav.familie.ks.sak.data.fnrTilAktør
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.data.årMåned
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.filtrerAndelerSomSkalSendesTilOppdrag
import no.nav.familie.ks.sak.kjerne.beregning.domene.totalKalkulertUtbetalingsbeløpForPeriode
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class UtbetalingsoppdragGeneratorTest {
    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator()

    @Test
    fun `lagUtbetalingsoppdrag skal opprette et nytt utbetalingsoppdrag med felles løpende periodeId og separat kjeding på to personer`() {
        val tidNå = LocalDate.now()

        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())
        val aktør2 = fnrTilAktør(randomFnr())
        val vedtak = Vedtak(behandling = behandling)
        val tilkjentYtelse = TilkjentYtelse(behandling = behandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør, årMåned("2019-04"), årMåned("2023-03"), id = 0),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør, årMåned("2026-05"), årMåned("2027-06"), id = 1),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør2, årMåned("2019-03"), årMåned("2037-02"), id = 2),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør2, årMåned("2037-05"), årMåned("2050-02"), id = 3),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelTilkjentYtelser)

        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = null,
                nyTilkjentYtelse = tilkjentYtelse,
                sisteAndelPerKjede = emptyMap(),
                erSimulering = false,
            )

        val utbetalingsoppdrag = beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.kodeEndring, Is(no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring.NY))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(4))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == behandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true),
        )

        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoFom, Is(dato("2019-04-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoTom, Is(dato("2023-03-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].periodeId, Is(0))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].forrigePeriodeId, Is(nullValue()))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoFom, Is(dato("2026-05-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoTom, Is(dato("2027-06-30")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].periodeId, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId, Is(0))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoFom, Is(dato("2019-03-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoTom, Is(dato("2037-02-28")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].periodeId, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].forrigePeriodeId, Is(nullValue()))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].vedtakdatoFom, Is(dato("2037-05-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].vedtakdatoTom, Is(dato("2050-02-28")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].periodeId, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].forrigePeriodeId, Is(2))
    }

    @Test
    fun `lagUtbetalingsoppdrag skal opprette et fullstendig opphør for to personer, hvor opphørsdatoer blir første dato i hver kjede`() {
        val tidNå = LocalDate.now()

        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val aktør = fnrTilAktør(randomFnr())
        val aktør2 = fnrTilAktør(randomFnr())
        val vedtak = Vedtak(behandling = behandling)

        val førsteDatoKjede1 = årMåned("2019-04")
        val førsteDatoKjede2 = årMåned("2019-03")

        val forrigeTilkjentYtelse = TilkjentYtelse(behandling = behandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    behandling = behandling,
                    aktør = aktør,
                    stønadFom = førsteDatoKjede1,
                    stønadTom = årMåned("2023-03"),
                    periodeOffset = 0,
                    id = 0,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    behandling = behandling,
                    aktør = aktør,
                    stønadFom = årMåned("2026-05"),
                    stønadTom = årMåned("2027-06"),
                    periodeOffset = 1,
                    id = 1,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    behandling = behandling,
                    aktør = aktør2,
                    stønadFom = førsteDatoKjede2,
                    stønadTom = årMåned("2037-02"),
                    periodeOffset = 2,
                    id = 2,
                ),
            )

        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(andelTilkjentYtelser)

        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                nyTilkjentYtelse = TilkjentYtelse(behandling = behandling, opprettetDato = tidNå, endretDato = tidNå),
                sisteAndelPerKjede = andelTilkjentYtelser.groupBy { andelTilkjentYtelse -> IdentOgType(ident = andelTilkjentYtelse.aktør.aktivFødselsnummer(), type = YtelsetypeKS.ORDINÆR_KONTANTSTØTTE) }.filterValues { andelTilkjentYtelse -> andelTilkjentYtelse.any { it.periodeOffset != null } }.mapValues { it.value.maxBy { andelTilkjentYtelse -> andelTilkjentYtelse.periodeOffset!! } },
                erSimulering = false,
            )

        val utbetalingsoppdrag = beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == behandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true),
        )

        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoFom, Is(dato("2026-05-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoTom, Is(dato("2027-06-30")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].periodeId, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].forrigePeriodeId, Is(nullValue()))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode[0].opphør?.opphørDatoFom,
            Is(førsteDatoKjede1.førsteDagIInneværendeMåned()),
        )

        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoFom, Is(dato("2019-03-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoTom, Is(dato("2037-02-28")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].periodeId, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId, Is(nullValue()))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode[1].opphør?.opphørDatoFom,
            Is(førsteDatoKjede2.førsteDagIInneværendeMåned()),
        )
    }

    @Test
    fun `lagTilkjentYtelseMedUtbetalingsoppdrag skal opprette revurdering med endring på eksisterende periode`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val førsteBehandling =
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak).also {
                it.behandlingStegTilstand.forEach { behandlingSteg ->
                    behandlingSteg.behandlingStegStatus = BehandlingStegStatus.UTFØRT
                }
            }

        val vedtak = Vedtak(behandling = førsteBehandling)

        val fomDatoSomEndres = årMåned("2033-01")

        val tilkjentYtelseIFørsteBehandling =
            TilkjentYtelse(behandling = førsteBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIFørsteBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2020-01"),
                    stønadTom = årMåned("2029-12"),
                    id = 0,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = fomDatoSomEndres,
                    stønadTom = årMåned("2034-12"),
                    id = 1,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2037-01"),
                    stønadTom = årMåned("2039-12"),
                    id = 2,
                ),
            )

        tilkjentYtelseIFørsteBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIFørsteBehandling)

        val beregnetUtbetalingsoppdragLongId1 =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = null,
                nyTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                sisteAndelPerKjede = emptyMap(),
                erSimulering = false,
            )

        val oppdaterteAndelerTilkjentYtelseFørsteBehandling = tilkjentYtelseIFørsteBehandling.tilAndelerMedOppdatertOffset(beregnetUtbetalingsoppdragLongId1.andeler)

        val andreBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)

        val tilkjentYtelseIAndreBehandling =
            TilkjentYtelse(behandling = andreBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIAndreBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2020-01"),
                    stønadTom = årMåned("2029-12"),
                    id = 3,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2034-01"),
                    stønadTom = årMåned("2034-12"),
                    id = 4,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2037-01"),
                    stønadTom = årMåned("2039-12"),
                    id = 5,
                ),
            )

        tilkjentYtelseIAndreBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIAndreBehandling)

        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                nyTilkjentYtelse = tilkjentYtelseIAndreBehandling,
                sisteAndelPerKjede = oppdaterteAndelerTilkjentYtelseFørsteBehandling.groupBy { andelTilkjentYtelse -> IdentOgType(ident = andelTilkjentYtelse.aktør.aktivFødselsnummer(), type = YtelsetypeKS.ORDINÆR_KONTANTSTØTTE) }.filterValues { andelTilkjentYtelse -> andelTilkjentYtelse.any { it.periodeOffset != null } }.mapValues { it.value.maxBy { andelTilkjentYtelse -> andelTilkjentYtelse.periodeOffset!! } },
                erSimulering = false,
            )

        val utbetalingsoppdrag = beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == førsteBehandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true),
        )
        assertThat(utbetalingsoppdrag.kodeEndring, Is(no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring.ENDR))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.count { it.opphør != null }, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.count { it.opphør == null }, Is(2))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoFom, Is(dato("2037-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoTom, Is(dato("2039-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].periodeId, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].forrigePeriodeId, Is(1))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoFom, Is(dato("2034-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoTom, Is(dato("2034-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].periodeId, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId, Is(2))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoFom, Is(dato("2037-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoTom, Is(dato("2039-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].periodeId, Is(4))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].forrigePeriodeId, Is(3))
    }

    @Test
    fun `lagTilkjentYtelseMedUtbetalingsoppdrag skal opprette revurdering med nytt barn`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val førsteBehandling =
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak).also {
                it.behandlingStegTilstand.forEach { behandlingSteg ->
                    behandlingSteg.behandlingStegStatus = BehandlingStegStatus.UTFØRT
                }
            }

        val vedtak = Vedtak(behandling = førsteBehandling)

        val tilkjentYtelseIFørsteBehandling =
            TilkjentYtelse(behandling = førsteBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIFørsteBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2020-01"),
                    stønadTom = årMåned("2029-12"),
                    id = 0,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2033-01"),
                    stønadTom = årMåned("2034-12"),
                    id = 1,
                ),
            )

        tilkjentYtelseIFørsteBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIFørsteBehandling)
        tilkjentYtelseIFørsteBehandling.utbetalingsoppdrag = "Oppdrag"

        val oppdatertTilkjentYtelseIFørsteBehandling =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = null,
                nyTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                sisteAndelPerKjede = emptyMap(),
                erSimulering = false,
            )

        val oppdaterteAndelerTilkjentYtelseFørsteBehandling = tilkjentYtelseIFørsteBehandling.tilAndelerMedOppdatertOffset(oppdatertTilkjentYtelseIFørsteBehandling.andeler)

        val andreBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)
        val nyAktør = fnrTilAktør(randomFnr())

        val tilkjentYtelseIAndreBehandling =
            TilkjentYtelse(behandling = andreBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIAndreBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = nyAktør,
                    stønadFom = årMåned("2022-01"),
                    stønadTom = årMåned("2034-12"),
                    id = 2,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = nyAktør,
                    stønadFom = årMåned("2037-01"),
                    stønadTom = årMåned("2039-12"),
                    id = 3,
                ),
            )

        tilkjentYtelseIAndreBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIAndreBehandling)

        val oppdatertTilkjentYtelseIAndreBehandling =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                nyTilkjentYtelse = tilkjentYtelseIAndreBehandling,
                sisteAndelPerKjede = oppdaterteAndelerTilkjentYtelseFørsteBehandling.groupBy { andelTilkjentYtelse -> IdentOgType(ident = andelTilkjentYtelse.aktør.aktivFødselsnummer(), type = YtelsetypeKS.ORDINÆR_KONTANTSTØTTE) }.filterValues { andelTilkjentYtelse -> andelTilkjentYtelse.any { it.periodeOffset != null } }.mapValues { it.value.maxBy { andelTilkjentYtelse -> andelTilkjentYtelse.periodeOffset!! } },
                erSimulering = false,
            )

        val utbetalingsoppdrag = oppdatertTilkjentYtelseIAndreBehandling.utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == førsteBehandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true),
        )
        assertThat(utbetalingsoppdrag.kodeEndring, Is(no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring.ENDR))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.count { it.opphør != null }, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.count { it.opphør == null }, Is(2))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoFom, Is(dato("2033-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoTom, Is(dato("2034-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].periodeId, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].forrigePeriodeId, Is(0))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoFom, Is(dato("2022-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoTom, Is(dato("2034-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].periodeId, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId, Is(nullValue()))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoFom, Is(dato("2037-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoTom, Is(dato("2039-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].periodeId, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].forrigePeriodeId, Is(2))
    }

    @Test
    fun `lagUtbetalingsoppdrag skal ikke generere utbetalingsperioder for andeler som er uendret`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val førsteBehandling =
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak).also {
                it.behandlingStegTilstand.forEach { behandlingSteg ->
                    behandlingSteg.behandlingStegStatus = BehandlingStegStatus.UTFØRT
                }
            }

        val vedtak = Vedtak(behandling = førsteBehandling)

        val tilkjentYtelseIFørsteBehandling =
            TilkjentYtelse(behandling = førsteBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIFørsteBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2020-01"),
                    stønadTom = årMåned("2029-12"),
                    id = 0,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2030-01"),
                    stønadTom = årMåned("2034-12"),
                    id = 1,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2035-01"),
                    stønadTom = årMåned("2039-12"),
                    id = 2,
                ),
            )

        tilkjentYtelseIFørsteBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIFørsteBehandling)

        val oppdatertTilkjentYtelseIFørsteBehandling =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = null,
                nyTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                sisteAndelPerKjede = emptyMap(),
                erSimulering = false,
            )

        val oppdaterteAndelerTilkjentYtelseFørsteBehandling = tilkjentYtelseIFørsteBehandling.tilAndelerMedOppdatertOffset(oppdatertTilkjentYtelseIFørsteBehandling.andeler)

        val andreBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)

        val tilkjentYtelseIAndreBehandling =
            TilkjentYtelse(behandling = andreBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIAndreBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2020-01"),
                    stønadTom = årMåned("2029-12"),
                    id = 3,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2030-01"),
                    stønadTom = årMåned("2034-12"),
                    id = 4,
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2035-01"),
                    stønadTom = årMåned("2039-12"),
                    id = 5,
                ),
            )

        tilkjentYtelseIAndreBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIAndreBehandling)

        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                nyTilkjentYtelse = tilkjentYtelseIAndreBehandling,
                sisteAndelPerKjede = oppdaterteAndelerTilkjentYtelseFørsteBehandling.groupBy { andelTilkjentYtelse -> IdentOgType(ident = andelTilkjentYtelse.aktør.aktivFødselsnummer(), type = YtelsetypeKS.ORDINÆR_KONTANTSTØTTE) }.filterValues { andelTilkjentYtelse -> andelTilkjentYtelse.any { it.periodeOffset != null } }.mapValues { it.value.maxBy { andelTilkjentYtelse -> andelTilkjentYtelse.periodeOffset!! } },
                erSimulering = false,
            )

        val utbetalingsoppdrag = beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(0))
    }

    @Test
    fun `lagUtbetalingsoppdrag skal ikke generere utbetalingsperioder for overgangsordningAndeler som har uendret sum`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val førsteBehandling =
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak).also {
                it.behandlingStegTilstand.forEach { behandlingSteg ->
                    behandlingSteg.behandlingStegStatus = BehandlingStegStatus.UTFØRT
                }
            }

        val vedtak = Vedtak(behandling = førsteBehandling)

        val tilkjentYtelseIFørsteBehandling =
            TilkjentYtelse(behandling = førsteBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIFørsteBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    id = 0,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2020-01"),
                    stønadTom = årMåned("2024-07"),
                ),
                lagAndelTilkjentYtelse(
                    id = 1,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2024-10"),
                    stønadTom = årMåned("2024-11"),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2025-01"),
                    stønadTom = årMåned("2025-02"),
                    kalkulertUtbetalingsbeløp = 2000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        tilkjentYtelseIFørsteBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIFørsteBehandling)

        val oppdatertTilkjentYtelseIFørsteBehandling =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = null,
                nyTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                sisteAndelPerKjede = emptyMap(),
                erSimulering = false,
            )

        val oppdaterteAndelerTilkjentYtelseFørsteBehandling = tilkjentYtelseIFørsteBehandling.tilAndelerMedOppdatertOffset(oppdatertTilkjentYtelseIFørsteBehandling.andeler)

        val andreBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)

        val tilkjentYtelseIAndreBehandling =
            TilkjentYtelse(behandling = andreBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelserIAndreBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    id = 3,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2020-01"),
                    stønadTom = årMåned("2024-07"),
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2024-08"),
                    stønadTom = årMåned("2024-08"),
                    kalkulertUtbetalingsbeløp = 4000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                lagAndelTilkjentYtelse(
                    id = 5,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2025-01"),
                    stønadTom = årMåned("2025-02"),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        tilkjentYtelseIAndreBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIAndreBehandling)

        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                nyTilkjentYtelse = tilkjentYtelseIAndreBehandling,
                sisteAndelPerKjede =
                    oppdaterteAndelerTilkjentYtelseFørsteBehandling
                        .groupBy { andelTilkjentYtelse -> IdentOgType(ident = andelTilkjentYtelse.aktør.aktivFødselsnummer(), type = YtelsetypeKS.ORDINÆR_KONTANTSTØTTE) }
                        .filterValues { andelTilkjentYtelse -> andelTilkjentYtelse.any { it.periodeOffset != null } }
                        .mapValues { it.value.maxBy { andelTilkjentYtelse -> andelTilkjentYtelse.periodeOffset!! } },
                erSimulering = false,
            )

        val utbetalingsoppdrag = beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag

        assertThat("Det er uventede utbetalingsperioder: ${utbetalingsoppdrag.utbetalingsperiode}", utbetalingsoppdrag.utbetalingsperiode.size, Is(0))
    }

    @Test
    fun `lagUtbetalingsoppdrag skal generere én utbetalingsperiode for overgangsordning per barn `() {
        val tidNå = LocalDate.now()

        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())
        val aktør2 = fnrTilAktør(randomFnr())
        val vedtak = Vedtak(behandling = behandling)
        val tilkjentYtelse = TilkjentYtelse(behandling = behandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør, årMåned("2024-05"), årMåned("2024-08"), id = 0),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør, årMåned("2024-09"), årMåned("2024-10"), id = 1, ytelseType = YtelseType.OVERGANGSORDNING),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør, årMåned("2024-12"), årMåned("2024-12"), id = 2, ytelseType = YtelseType.OVERGANGSORDNING),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør2, årMåned("2024-11"), årMåned("2024-12"), id = 3, ytelseType = YtelseType.OVERGANGSORDNING, kalkulertUtbetalingsbeløp = 1000),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør2, årMåned("2025-01"), årMåned("2025-02"), id = 4, ytelseType = YtelseType.OVERGANGSORDNING),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelTilkjentYtelser)

        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = "123abc",
                vedtak = vedtak,
                forrigeTilkjentYtelse = null,
                nyTilkjentYtelse = tilkjentYtelse,
                sisteAndelPerKjede = emptyMap(),
                erSimulering = false,
            )

        val utbetalingsoppdrag = beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.kodeEndring, Is(no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring.NY))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(3))
        assertThat(
            "Sats på utbetalingsperiode var feil: ${utbetalingsoppdrag.utbetalingsperiode.map { it.sats }}",
            utbetalingsoppdrag.utbetalingsperiode.map { it.sats.toInt() },
            Is(listOf(7500, 22500, 17000)),
        )
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == behandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true),
        )

        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoFom, Is(dato("2024-05-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoTom, Is(dato("2024-08-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].periodeId, Is(0))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].forrigePeriodeId, Is(nullValue()))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoFom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().førsteDagIInneværendeMåned()))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoTom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().sisteDagIMåned()))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].periodeId, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId, Is(0))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoFom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().førsteDagIInneværendeMåned()))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoTom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().sisteDagIMåned()))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].periodeId, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].forrigePeriodeId, Is(nullValue()))
    }

    @Test
    fun `lagUtbetalingsoppdrag skal summere overgangsordning og ordinær andel for utbetalingsmåned i overgangsordningandelen per barn`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val gammelBehandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val tilkjentYtelseGammelBehandling =
            TilkjentYtelse(behandling = gammelBehandling, opprettetDato = tidNå, endretDato = tidNå)
                .apply {
                    andelerTilkjentYtelse.add(
                        lagAndelTilkjentYtelse(this, gammelBehandling, aktør, årMåned("2024-04"), årMåned("2024-10"), id = 0, periodeOffset = 0),
                    )
                }
        val sisteAndelPerKjede =
            mapOf(
                IdentOgType(ident = aktør.aktivFødselsnummer(), type = YtelsetypeKS.ORDINÆR_KONTANTSTØTTE) to
                    tilkjentYtelseGammelBehandling.andelerTilkjentYtelse.first(),
            )

        val nyBehandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.OVERGANGSORDNING_2024)
        val vedtakNyBehandling = Vedtak(behandling = nyBehandling)
        val tilkjentYtelsenyBehandling =
            TilkjentYtelse(behandling = nyBehandling, opprettetDato = tidNå, endretDato = tidNå)
                .apply {
                    andelerTilkjentYtelse.addAll(
                        listOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = this,
                                behandling = nyBehandling,
                                aktør = aktør,
                                stønadFom = OVERGANGSORDNING_UTBETALINGSMÅNED.minusMonths(6),
                                stønadTom = OVERGANGSORDNING_UTBETALINGSMÅNED,
                                id = 1,
                            ),
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = this,
                                behandling = nyBehandling,
                                aktør = aktør,
                                stønadFom = OVERGANGSORDNING_UTBETALINGSMÅNED.plusMonths(1),
                                stønadTom = OVERGANGSORDNING_UTBETALINGSMÅNED.plusMonths(4),
                                ytelseType = YtelseType.OVERGANGSORDNING,
                                id = 2,
                            ),
                        ),
                    )
                }

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator
                .lagUtbetalingsoppdrag(
                    saksbehandlerId = "123abc",
                    vedtak = vedtakNyBehandling,
                    forrigeTilkjentYtelse = tilkjentYtelseGammelBehandling,
                    nyTilkjentYtelse = tilkjentYtelsenyBehandling,
                    sisteAndelPerKjede = sisteAndelPerKjede,
                    erSimulering = false,
                ).utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.kodeEndring, Is(Utbetalingsoppdrag.KodeEndring.ENDR))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(3))

        val forventetUtbetalingsbeløp =
            tilkjentYtelsenyBehandling.andelerTilkjentYtelse.let { andeler ->
                andeler.first { it.type == YtelseType.ORDINÆR_KONTANTSTØTTE }.kalkulertUtbetalingsbeløp +
                    andeler.first { it.type == YtelseType.OVERGANGSORDNING }.totalKalkulertUtbetalingsbeløpForPeriode()
            }

        assertThat(
            "Sats på utbetalingsperiode var feil: ${utbetalingsoppdrag.utbetalingsperiode.map { it.sats }}",
            utbetalingsoppdrag.utbetalingsperiode.any {
                it.sats.toInt() == forventetUtbetalingsbeløp
            },
            Is(true),
        )
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == aktør.aktivFødselsnummer() },
            Is(true),
        )

        val utbetalingOvergangsordning = utbetalingsoppdrag.utbetalingsperiode.first { it.sats.toInt() == forventetUtbetalingsbeløp }
        assertThat(utbetalingOvergangsordning.vedtakdatoFom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().førsteDagIInneværendeMåned()))
        assertThat(utbetalingOvergangsordning.vedtakdatoTom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().sisteDagIMåned()))
    }

    @Test
    fun `lagUtbetalingsoppdrag skal ta hensyn til overgangsordningsandeler dersom den siste andelen i kjeden er overgangsordning andel`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val førsteOvergangsordningBehandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.OVERGANGSORDNING_2024)
        val tilkjentYtelseFørsteOvergangsordningBehandling =
            TilkjentYtelse(behandling = førsteOvergangsordningBehandling, opprettetDato = tidNå, endretDato = tidNå)
                .apply {
                    andelerTilkjentYtelse.addAll(
                        listOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = this,
                                behandling = førsteOvergangsordningBehandling,
                                aktør = aktør,
                                stønadFom = OVERGANGSORDNING_UTBETALINGSMÅNED.minusMonths(6),
                                stønadTom = OVERGANGSORDNING_UTBETALINGSMÅNED,
                                id = 0,
                                periodeOffset = 0,
                                ytelseType = YtelseType.OVERGANGSORDNING,
                            ),
                        ),
                    )
                }

        val sisteAndelPerKjede =
            mapOf(
                IdentOgType(ident = aktør.aktivFødselsnummer(), type = YtelsetypeKS.ORDINÆR_KONTANTSTØTTE) to
                    tilkjentYtelseFørsteOvergangsordningBehandling.andelerTilkjentYtelse.first(),
            )

        val andreOvergangsordningBehandlingMed1MånedMindre = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.OVERGANGSORDNING_2024)

        val vedtakNyBehandling = Vedtak(behandling = andreOvergangsordningBehandlingMed1MånedMindre)

        val tilkjentYtelseAndreOvergangsordningBehandlingMed1MånedMindre =
            TilkjentYtelse(behandling = andreOvergangsordningBehandlingMed1MånedMindre, opprettetDato = tidNå, endretDato = tidNå)
                .apply {
                    andelerTilkjentYtelse.addAll(
                        listOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = this,
                                behandling = førsteOvergangsordningBehandling,
                                aktør = aktør,
                                stønadFom = OVERGANGSORDNING_UTBETALINGSMÅNED.minusMonths(6),
                                stønadTom = OVERGANGSORDNING_UTBETALINGSMÅNED.minusMonths(1),
                                ytelseType = YtelseType.OVERGANGSORDNING,
                                id = 1,
                            ),
                        ),
                    )
                }

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator
                .lagUtbetalingsoppdrag(
                    saksbehandlerId = "123abc",
                    vedtak = vedtakNyBehandling,
                    forrigeTilkjentYtelse = tilkjentYtelseFørsteOvergangsordningBehandling,
                    nyTilkjentYtelse = tilkjentYtelseAndreOvergangsordningBehandlingMed1MånedMindre,
                    sisteAndelPerKjede = sisteAndelPerKjede,
                    erSimulering = false,
                ).utbetalingsoppdrag

        assertThat(utbetalingsoppdrag.kodeEndring, Is(Utbetalingsoppdrag.KodeEndring.ENDR))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(1))

        val forventetUtbetalingsbeløp =
            tilkjentYtelseAndreOvergangsordningBehandlingMed1MånedMindre.andelerTilkjentYtelse.let { andeler ->
                andeler.first { it.type == YtelseType.OVERGANGSORDNING }.totalKalkulertUtbetalingsbeløpForPeriode()
            }

        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == aktør.aktivFødselsnummer() },
            Is(true),
        )

        val utbetalingOvergangsordning = utbetalingsoppdrag.utbetalingsperiode.first { it.sats.toInt() == forventetUtbetalingsbeløp }
        assertThat(utbetalingOvergangsordning.vedtakdatoFom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().førsteDagIInneværendeMåned()))
        assertThat(utbetalingOvergangsordning.vedtakdatoTom, Is(OVERGANGSORDNING_UTBETALINGSMÅNED.toLocalDate().sisteDagIMåned()))
    }

    private fun TilkjentYtelse.tilAndelerMedOppdatertOffset(
        andelerMedPeriodeId: List<AndelMedPeriodeIdLongId>,
    ): MutableSet<AndelTilkjentYtelse> =
        andelerTilkjentYtelse
            .filtrerAndelerSomSkalSendesTilOppdrag()
            .onEach { andel ->
                val andelMedOffset = andelerMedPeriodeId.first { it.id == andel.id }
                andel.periodeOffset = andelMedOffset.periodeId
                andel.forrigePeriodeOffset = andelMedOffset.forrigePeriodeId
                andel.kildeBehandlingId = andelMedOffset.kildeBehandlingId
            }.toMutableSet()
}
