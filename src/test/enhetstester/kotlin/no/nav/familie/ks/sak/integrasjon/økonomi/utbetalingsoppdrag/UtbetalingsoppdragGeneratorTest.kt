package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import io.mockk.junit5.MockKExtension
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.data.fnrTilAktør
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.data.årMåned
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.dato
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import org.hamcrest.CoreMatchers.nullValue
import java.math.BigDecimal
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class UtbetalingsoppdragGeneratorTest {

    val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator()

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
                lagAndelTilkjentYtelse(tilkjentYtelse, behandling, aktør2, årMåned("2037-05"), årMåned("2050-02")),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelTilkjentYtelser)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
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
                ),
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
            AndelTilkjentYtelseForIverksettingFactory(),
            forrigeTilkjentYtelse = forrigeTilkjentYtelse
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

        val avsluttetBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).also {
            it.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.UTFØRT }

        }

        val aktør = fnrTilAktør(randomFnr())
        val aktør2 = fnrTilAktør(randomFnr())
        val vedtak = Vedtak(behandling = avsluttetBehandling)

        val førsteDatoKjede1 = årMåned("2019-04")
        val førsteDatoKjede2 = årMåned("2019-03")

        val tilkjentYtelse =
            TilkjentYtelse(behandling = avsluttetBehandling, opprettetDato = tidNå, endretDato = tidNå)

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = avsluttetBehandling,
                    aktør = aktør,
                    stønadFom = førsteDatoKjede1,
                    stønadTom = årMåned("2023-03"),
                    periodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = avsluttetBehandling,
                    aktør = aktør,
                    stønadFom = årMåned("2026-05"),
                    stønadTom = årMåned("2027-06"),
                    periodeOffset = 1
                ),
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = avsluttetBehandling,
                    aktør = aktør2,
                    stønadFom = førsteDatoKjede2,
                    stønadTom = årMåned("2037-02"),
                    periodeOffset = 2
                ),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelTilkjentYtelser)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = TilkjentYtelse(
                    behandling = avsluttetBehandling,
                    opprettetDato = tidNå,
                    endretDato = tidNå
                ),
                sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                    ØkonomiUtils.kjedeinndelteAndeler(
                        andelTilkjentYtelser.forIverksetting()
                    )
                )
            ),
            AndelTilkjentYtelseForIverksettingFactory(),
            forrigeTilkjentYtelse = tilkjentYtelse
        )

        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse.utbetalingsoppdrag)

        assertThat(utbetalingsoppdrag.utbetalingsperiode.size, Is(2))
        assertThat(utbetalingsoppdrag.utbetalingsperiode.all { it.sats == BigDecimal(7500) }, Is(true))
        assertThat(
            utbetalingsoppdrag.utbetalingsperiode.all { it.utbetalesTil == avsluttetBehandling.fagsak.aktør.aktivFødselsnummer() },
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
    AndelTilkjentYtelseForIverksettingFactory().pakkInnForUtbetaling(this)
