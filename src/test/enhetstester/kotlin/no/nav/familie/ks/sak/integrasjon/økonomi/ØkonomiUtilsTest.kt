package no.nav.familie.ks.sak.integrasjon.økonomi

import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.data.fnrTilAktør
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.data.årMåned
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.AndelTilkjentYtelseForIverksetting
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class ØkonomiUtilsTest {

    @Test
    fun `kjedeinndelteAndeler skal returnere siste før første berørte andel i kjede`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())
        val aktør2 = fnrTilAktør(randomFnr())

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2022-01"), årMåned("2023-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør2, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør2, årMåned("2022-01"), årMåned("2023-01"))
            ).forIverksetting()
        )

        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2022-01"), årMåned("2022-10")),
                lagAndelTilkjentYtelse(null, behandling, aktør2, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør2, årMåned("2022-01"), årMåned("2023-01"))
            ).forIverksetting()
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)

        assertThat(sisteBeståendePerKjede[aktør.aktivFødselsnummer()]?.stønadFom, Is(årMåned("2019-04")))
        assertThat(sisteBeståendePerKjede[aktør2.aktivFødselsnummer()]?.stønadFom, Is(årMåned("2022-01")))
    }

    @Test
    fun `kjedeinndelteAndeler skal sette null som siste bestående for person med endring i første kjede`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2022-01"), årMåned("2023-01"))
            ).forIverksetting()
        )

        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2018-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2022-01"), årMåned("2023-01"))
            ).forIverksetting()
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)

        assertThat(sisteBeståendePerKjede[aktør.aktørId], Is(nullValue()))
    }

    @Test
    fun `kjedeinndelteAndeler skal sette null som siste bestående for ny person`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())

        val kjederBehandling = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2023-10"), årMåned("2025-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2027-10"), årMåned("2028-01"))
            ).forIverksetting()
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = emptyMap(), oppdaterteKjeder = kjederBehandling)

        assertThat(sisteBeståendePerKjede[aktør.aktørId]?.stønadFom, Is(nullValue()))
    }

    @Test
    fun `andelerTilOpphørMedDato skal velge rette perioder til opphør fra endring`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())

        val datoSomSkalOppdateres = "2022-01"
        val datoSomErOppdatert = "2021-01"

        val kjederBehandling = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned(datoSomSkalOppdateres), årMåned("2023-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2025-04"), årMåned("2026-01"))
            ).forIverksetting()
        )

        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned(datoSomErOppdatert), årMåned("2023-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2025-04"), årMåned("2026-01"))
            ).forIverksetting()
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling, oppdaterteKjeder = kjederBehandling2)

        val andelerTilOpphørMedDato =
            andelerTilOpphørMedDato(
                forrigeKjeder = kjederBehandling,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )

        assertThat(andelerTilOpphørMedDato.first().second, Is(årMåned(datoSomSkalOppdateres)))
    }

    @Test
    fun `andelerTilOpprettelse skal velge rette perioder til oppbygging fra endring`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())

        val datoSomSkalOppdateres = "2022-01"
        val datoSomErOppdatert = "2021-01"

        val kjederBehandling = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned(datoSomSkalOppdateres), årMåned("2023-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2025-04"), årMåned("2026-01"))
            ).forIverksetting()
        )

        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned(datoSomErOppdatert), årMåned("2023-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2025-04"), årMåned("2026-01"))
            ).forIverksetting()
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling, oppdaterteKjeder = kjederBehandling2)

        val andelerTilOpprettelse =
            andelerTilOpprettelse(
                oppdaterteKjeder = kjederBehandling,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )

        assertThat(andelerTilOpprettelse.size, Is(1))
        assertThat(andelerTilOpprettelse.first().size, Is(2))
    }

    @Test
    fun `oppdaterBeståendeAndelerMedOffset skal oppdatere offset på bestående andeler i oppdaterte kjeder`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val aktør = fnrTilAktør(randomFnr())
        val aktør2 = fnrTilAktør(randomFnr())

        val kjederBehandling = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    null,
                    behandling,
                    aktør,
                    årMåned("2019-04"),
                    årMåned("2020-01"),
                    periodeOffset = 1,
                    forrigePeriodeOffset = 0
                ),
                lagAndelTilkjentYtelse(
                    null,
                    behandling,
                    aktør2,
                    årMåned("2019-04"),
                    årMåned("2020-01"),
                    periodeOffset = 3,
                    forrigePeriodeOffset = 2
                )
            ).forIverksetting()
        )

        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(null, behandling, aktør, årMåned("2019-04"), årMåned("2020-01")),
                lagAndelTilkjentYtelse(null, behandling, aktør2, årMåned("2019-12"), årMåned("2020-01"))
            ).forIverksetting()
        )

        val oppdaterteBeståendeAndeler =
            oppdaterBeståendeAndelerMedOffset(
                forrigeKjeder = kjederBehandling,
                oppdaterteKjeder = kjederBehandling2
            )

        assertThat(oppdaterteBeståendeAndeler.getValue(aktør.aktivFødselsnummer()).first().periodeOffset, Is(1))
        assertThat(oppdaterteBeståendeAndeler.getValue(aktør.aktivFødselsnummer()).first().forrigePeriodeOffset, Is(0))
        assertThat(
            oppdaterteBeståendeAndeler.getValue(aktør2.aktivFødselsnummer()).first().periodeOffset,
            Is(nullValue())
        )
        assertThat(
            oppdaterteBeståendeAndeler.getValue(aktør2.aktivFødselsnummer()).first().forrigePeriodeOffset,
            Is(nullValue())
        )
    }

    private fun Collection<AndelTilkjentYtelse>.forIverksetting() =
        AndelTilkjentYtelseForIverksetting.Factory.pakkInnForUtbetaling(this)
}
