package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import io.mockk.junit5.MockKExtension
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.dato
import no.nav.familie.ks.sak.data.fnrTilAktør
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.data.årMåned
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class UtbetalingsoppdragGeneratorTest {

    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator()

    @Test
    fun `lagTilkjentYtelseMedUtbetalingsoppdrag skal opprette et nytt utbetalingsoppdrag med felles løpende periodeId og separat kjeding på to personer`() {
        val tidNå = LocalDate.now()

        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())
        val aktør2 = fnrTilAktør(randomFnr())
        val vedtak = Vedtak(behandling = behandling)
        val tilkjentYtelse = TilkjentYtelse(behandling = behandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør, årMåned("2019-04"), årMåned("2023-03")),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør, årMåned("2026-05"), årMåned("2027-06")),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør2, årMåned("2019-03"), årMåned("2037-02")),
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør2, årMåned("2037-05"), årMåned("2050-02"))
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelTilkjentYtelser)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksetting.Factory
        )

        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse.utbetalingsoppdrag)

        assertThat(utbetalingsoppdrag.kodeEndring, Is(Utbetalingsoppdrag.KodeEndring.NY))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(4))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == behandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true)
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
    fun `lagTilkjentYtelseMedUtbetalingsoppdrag skal opprette et fullstendig opphør for to personer, hvor opphørsdatoer blir første dato i hver kjede`() {
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
                    periodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    behandling = behandling,
                    aktør = aktør,
                    stønadFom = årMåned("2026-05"),
                    stønadTom = årMåned("2027-06"),
                    periodeOffset = 1
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    behandling = behandling,
                    aktør = aktør2,
                    stønadFom = førsteDatoKjede2,
                    stønadTom = årMåned("2037-02"),
                    periodeOffset = 2
                )
            )

        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(andelTilkjentYtelser)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = TilkjentYtelse(behandling = behandling, opprettetDato = tidNå, endretDato = tidNå),
                sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                    ØkonomiUtils.kjedeinndelteAndeler(
                        andelTilkjentYtelser.forIverksetting()
                    )
                )
            ),
            AndelTilkjentYtelseForIverksetting.Factory,
            forrigeTilkjentYtelseMedAndeler = forrigeTilkjentYtelse
        )

        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse.utbetalingsoppdrag)

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == behandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true)
        )

        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoFom, Is(dato("2026-05-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoTom, Is(dato("2027-06-30")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].periodeId, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].forrigePeriodeId, Is(nullValue()))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode[0].opphør?.opphørDatoFom,
            Is(førsteDatoKjede1.førsteDagIInneværendeMåned())
        )

        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoFom, Is(dato("2019-03-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoTom, Is(dato("2037-02-28")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].periodeId, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId, Is(nullValue()))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode[1].opphør?.opphørDatoFom,
            Is(førsteDatoKjede2.førsteDagIInneværendeMåned())
        )
    }

    @Test
    fun `lagTilkjentYtelseMedUtbetalingsoppdrag skal opprette revurdering med endring på eksisterende periode`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val førsteBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak).also {
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
                    periodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = fomDatoSomEndres,
                    stønadTom = årMåned("2034-12"),
                    periodeOffset = 1
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2037-01"),
                    stønadTom = årMåned("2039-12"),
                    periodeOffset = 2
                )
            )

        tilkjentYtelseIFørsteBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIFørsteBehandling)

        val oppdatertTilkjentYtelseIFørsteBehandling =
            utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                lagVedtakMedTilkjentYtelse(
                    vedtak = vedtak,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling
                ),
                AndelTilkjentYtelseForIverksetting.Factory
            )

        val andreBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)
        val andreVedtak = Vedtak(behandling = andreBehandling)

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
                    periodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2034-01"),
                    stønadTom = årMåned("2034-12"),
                    periodeOffset = 3
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2037-01"),
                    stønadTom = årMåned("2039-12"),
                    periodeOffset = 4
                )
            )

        tilkjentYtelseIAndreBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIAndreBehandling)
        val sisteOffsetPåFagsak = 2

        val oppdatertTilkjentYtelseIAndreBehandling =
            utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                lagVedtakMedTilkjentYtelse(
                    vedtak = andreVedtak,
                    sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                        ØkonomiUtils.kjedeinndelteAndeler(
                            andelTilkjentYtelserIFørsteBehandling.forIverksetting()
                        )
                    ),
                    sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling
                ),
                AndelTilkjentYtelseForIverksetting.Factory,
                forrigeTilkjentYtelseMedAndeler = oppdatertTilkjentYtelseIFørsteBehandling
            )

        val utbetalingsoppdrag =
            konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelseIAndreBehandling.utbetalingsoppdrag)

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == førsteBehandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true)
        )
        assertThat(utbetalingsoppdrag.kodeEndring, Is(Utbetalingsoppdrag.KodeEndring.ENDR))
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

        val førsteBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak).also {
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
                    periodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2033-01"),
                    stønadTom = årMåned("2034-12"),
                    periodeOffset = 1
                )
            )

        tilkjentYtelseIFørsteBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIFørsteBehandling)
        tilkjentYtelseIFørsteBehandling.utbetalingsoppdrag = "Oppdrag"

        val oppdatertTilkjentYtelseIFørsteBehandling =
            utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                lagVedtakMedTilkjentYtelse(
                    vedtak = vedtak,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling
                ),
                AndelTilkjentYtelseForIverksetting.Factory
            )

        val andreBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)
        val andreVedtak = Vedtak(behandling = andreBehandling)
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
                    periodeOffset = 2
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = andreBehandling,
                    aktør = nyAktør,
                    stønadFom = årMåned("2037-01"),
                    stønadTom = årMåned("2039-12"),
                    periodeOffset = 3
                )
            )

        tilkjentYtelseIAndreBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIAndreBehandling)
        val sisteOffsetPåFagsak = 1

        val oppdatertTilkjentYtelseIAndreBehandling =
            utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                lagVedtakMedTilkjentYtelse(
                    vedtak = andreVedtak,
                    sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                        ØkonomiUtils.kjedeinndelteAndeler(
                            andelTilkjentYtelserIFørsteBehandling.forIverksetting()
                        )
                    ),
                    sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling
                ),
                AndelTilkjentYtelseForIverksetting.Factory,
                forrigeTilkjentYtelseMedAndeler = oppdatertTilkjentYtelseIFørsteBehandling
            )

        val utbetalingsoppdrag =
            konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelseIAndreBehandling.utbetalingsoppdrag)

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == førsteBehandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true)
        )
        assertThat(utbetalingsoppdrag.kodeEndring, Is(Utbetalingsoppdrag.KodeEndring.ENDR))
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
    fun `lagTilkjentYtelseMedUtbetalingsoppdrag skal generere komplett utbetalingsoppdrag selvom ingen endringer har blitt gjort`() {
        val tidNå = LocalDate.now()

        val aktør = fnrTilAktør(randomFnr())
        val fagsak = lagFagsak(aktør)

        val førsteBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak).also {
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
                    periodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2030-01"),
                    stønadTom = årMåned("2034-12"),
                    periodeOffset = 1
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2035-01"),
                    stønadTom = årMåned("2039-12"),
                    periodeOffset = 2
                )
            )

        tilkjentYtelseIFørsteBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIFørsteBehandling)

        val oppdatertTilkjentYtelseIFørsteBehandling =
            utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                lagVedtakMedTilkjentYtelse(
                    vedtak = vedtak,
                    tilkjentYtelse = tilkjentYtelseIFørsteBehandling
                ),
                AndelTilkjentYtelseForIverksetting.Factory
            )

        val andreBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)
        val andreVedtak = Vedtak(behandling = andreBehandling)

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
                    periodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2030-01"),
                    stønadTom = årMåned("2034-12"),
                    periodeOffset = 3
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    behandling = førsteBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2035-01"),
                    stønadTom = årMåned("2039-12"),
                    periodeOffset = 4
                )
            )

        tilkjentYtelseIAndreBehandling.andelerTilkjentYtelse.addAll(andelTilkjentYtelserIAndreBehandling)
        val sisteOffsetPåFagsak = 2

        val oppdatertTilkjentYtelseIAndreBehandling =
            utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                lagVedtakMedTilkjentYtelse(
                    vedtak = andreVedtak,
                    sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                        ØkonomiUtils.kjedeinndelteAndeler(
                            andelTilkjentYtelserIFørsteBehandling.forIverksetting()
                        )
                    ),
                    sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                    tilkjentYtelse = tilkjentYtelseIAndreBehandling,
                    erSimulering = true
                ),
                AndelTilkjentYtelseForIverksetting.Factory,
                forrigeTilkjentYtelseMedAndeler = oppdatertTilkjentYtelseIFørsteBehandling
            )

        val utbetalingsoppdrag =
            konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelseIAndreBehandling.utbetalingsoppdrag)

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(4))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == førsteBehandling.fagsak.aktør.aktivFødselsnummer() },
            Is(true)
        )
        assertThat(utbetalingsoppdrag.kodeEndring, Is(Utbetalingsoppdrag.KodeEndring.ENDR))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.count { it.opphør != null }, Is(1))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.count { it.opphør == null }, Is(3))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoFom, Is(dato("2035-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].vedtakdatoTom, Is(dato("2039-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].periodeId, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[0].forrigePeriodeId, Is(1))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoFom, Is(dato("2020-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].vedtakdatoTom, Is(dato("2029-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].periodeId, Is(3))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId, Is(2))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoFom, Is(dato("2030-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].vedtakdatoTom, Is(dato("2034-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].periodeId, Is(4))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[2].forrigePeriodeId, Is(3))

        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].vedtakdatoFom, Is(dato("2035-01-01")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].vedtakdatoTom, Is(dato("2039-12-31")))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].periodeId, Is(5))
        assertThat(utbetalingsoppdrag.utbetalingsperiode[3].forrigePeriodeId, Is(4))
    }

    private fun lagVedtakMedTilkjentYtelse(
        vedtak: Vedtak,
        tilkjentYtelse: TilkjentYtelse,
        sisteOffsetPerIdent: Map<String, Int> = emptyMap(),
        sisteOffsetPåFagsak: Int? = null,
        erSimulering: Boolean = false
    ) = VedtakMedTilkjentYtelse(
        tilkjentYtelse = tilkjentYtelse,
        vedtak = vedtak,
        saksbehandlerId = "saksbehandler",
        sisteOffsetPerIdent = sisteOffsetPerIdent,
        sisteOffsetPåFagsak = sisteOffsetPåFagsak,
        erSimulering = erSimulering
    )

    private fun konvertTilUtbetalingsoppdrag(utbetalingsoppdragIString: String?) =
        objectMapper.readValue(utbetalingsoppdragIString, Utbetalingsoppdrag::class.java)
}

fun Collection<AndelTilkjentYtelse>.forIverksetting() =
    AndelTilkjentYtelseForIverksetting.Factory.pakkInnForUtbetaling(this)
