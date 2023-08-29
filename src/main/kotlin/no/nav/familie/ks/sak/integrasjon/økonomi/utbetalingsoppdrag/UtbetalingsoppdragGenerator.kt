package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.ks.sak.integrasjon.økonomi.UtbetalingsperiodeMal
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.sisteAndelPerKjede
import no.nav.familie.ks.sak.integrasjon.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Component
import java.time.YearMonth

const val FAGSYSTEM = "KS"

@Component
class UtbetalingsoppdragGenerator {

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes kun siste utbetalingsperiode (med opphørsdato).
     *
     * @param[vedtakMedTilkjentYtelse] tilpasset objekt som inneholder tilkjentytelse,og andre nødvendige felter som trenges for å lage utbetalingsoppdrag
     * @param[andelTilkjentYtelseForUtbetalingsoppdragFactory] type factory bestemmer om AndelTilkjentYtelse muteres eller ikke. Avhengig om det er AndelTilkjentYtelseForIverksetting eller AndelTilkjentYtelseForSimulerin
     * @param[forrigeTilkjentYtelseMedAndeler] forrige tilkjentYtelse
     * @return oppdatert TilkjentYtelse som inneholder generert utbetalingsoppdrag
     */
    fun lagTilkjentYtelseMedUtbetalingsoppdrag(
        vedtakMedTilkjentYtelse: VedtakMedTilkjentYtelse,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory,
        forrigeTilkjentYtelseMedAndeler: TilkjentYtelse? = null
    ): TilkjentYtelse {
        val tilkjentYtelse = vedtakMedTilkjentYtelse.tilkjentYtelse
        val vedtak = vedtakMedTilkjentYtelse.vedtak
        val erFørsteBehandlingPåFagsakSomSkalIverksettes = forrigeTilkjentYtelseMedAndeler == null

        // Filtrer kun andeler som kan sendes til oppdrag
        val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
            .pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)

        // grupperer andeler basert på personIdent.
        val oppdaterteKjeder = kjedeinndelteAndeler(andelerTilkjentYtelse)

        // Filtrerer og grupperer forrige andeler basert på personIdent.
        val forrigeAndeler =
            forrigeTilkjentYtelseMedAndeler?.andelerTilkjentYtelse?.filter { it.erAndelSomSkalSendesTilOppdrag() }
                ?.pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)
                ?: emptyList()

        val forrigeKjeder = kjedeinndelteAndeler(forrigeAndeler)

        // Generer et komplett nytt eller bare endringer på et eksisterende betalingsoppdrag.
        val sisteBeståendeAndelIHverKjede = if (vedtakMedTilkjentYtelse.erSimulering) {
            // Gjennom å sette andeler til null markeres at alle perioder i kjeden skal opphøres.
            sisteAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        } else {
            // For å kunne behandle alle forlengelser/forkortelser av perioder likt har vi valgt å konsekvent opphøre og erstatte.
            // Det vil si at vi alltid gjenoppbygger kjede fra første endring, selv om vi i realiteten av og til kun endrer datoer
            // på en eksisterende linje (endring på 150 linjenivå).
            sisteBeståendeAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        }

        // Finner ut andeler som er opprettet
        val andelerTilOpprettelse = andelerTilOpprettelse(oppdaterteKjeder, sisteBeståendeAndelIHverKjede)

        // Setter offsettet til andeler som ikke er endret i denne behandlingen til
        // offsettet de hadde i forrige behandling.

        if (andelerTilkjentYtelse.isNotEmpty() && forrigeAndeler.isNotEmpty()) {
            ØkonomiUtils.oppdaterBeståendeAndelerMedOffset(
                oppdaterteKjeder = kjedeinndelteAndeler(andelerTilkjentYtelse),
                forrigeKjeder = kjedeinndelteAndeler(forrigeAndeler)
            )
        }

        // Trenger denne sjekken som slipper å sette offset når det ikke finnes andelerTilOpprettelse,dvs nullutbetaling
        val opprettes = if (andelerTilOpprettelse.isNotEmpty()) {
            // lager utbetalingsperioder og oppdaterer andelerTilkjentYtelse
            val opprettelsePeriodeMedAndeler = lagUtbetalingsperioderForOpprettelse(
                andeler = andelerTilOpprettelse,
                erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsakSomSkalIverksettes,
                vedtak = vedtak,
                sisteOffsetIKjedeOversikt = vedtakMedTilkjentYtelse.sisteOffsetPerIdent,
                sisteOffsetPåFagsak = vedtakMedTilkjentYtelse.sisteOffsetPåFagsak
            )

            opprettelsePeriodeMedAndeler.second
        } else {
            emptyList()
        }

        // Finner ut andeler som er opphørt
        val andelerTilOpphør = andelerTilOpphørMedDato(
            forrigeKjeder,
            sisteBeståendeAndelIHverKjede
        )

        val opphøres = lagUtbetalingsperioderForOpphør(andeler = andelerTilOpphør, vedtak = vedtak)

        val aksjonskodePåOppdragsnivå =
            if (erFørsteBehandlingPåFagsakSomSkalIverksettes) Utbetalingsoppdrag.KodeEndring.NY else Utbetalingsoppdrag.KodeEndring.ENDR
        val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = vedtakMedTilkjentYtelse.saksbehandlerId,
            kodeEndring = aksjonskodePåOppdragsnivå,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
            utbetalingsperiode = listOf(opphøres, opprettes).flatten()
        )

        // valider utbetalingsoppdrag
        val erBehandlingOpphørt = vedtak.behandling.resultat == Behandlingsresultat.OPPHØRT
        if (!vedtakMedTilkjentYtelse.erSimulering && erBehandlingOpphørt) utbetalingsoppdrag.validerOpphørsoppdrag()
        utbetalingsoppdrag.also {
            it.valider(
                behandlingsresultat = vedtak.behandling.resultat,
                behandlingskategori = vedtak.behandling.kategori,
                // her må vi sende alle andeler slik at det valideres for nullutbetalinger også
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList()
            )
        }

        // oppdater tilkjentYtlese med andelerTilkjentYTelser og utbetalingsoppdrag
        return tilkjentYtelse.copy(
            behandling = vedtak.behandling,
            utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
        )
    }

    private fun lagUtbetalingsperioderForOpphør(
        andeler: List<Pair<AndelTilkjentYtelseForUtbetalingsoppdrag, YearMonth>>,
        vedtak: Vedtak
    ): List<Utbetalingsperiode> =
        andeler.map { (sisteAndelIKjede, opphørKjedeFom) ->
            UtbetalingsperiodeMal(
                vedtak = vedtak,
                erEndringPåEksisterendePeriode = true
            ).lagPeriodeFraAndel(
                andel = sisteAndelIKjede,
                periodeIdOffset = sisteAndelIKjede.periodeOffset!!.toInt(),
                forrigePeriodeIdOffset = sisteAndelIKjede.forrigePeriodeOffset?.toInt(),
                opphørKjedeFom = opphørKjedeFom
            )
        }

    private fun lagUtbetalingsperioderForOpprettelse(
        andeler: List<List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        vedtak: Vedtak,
        erFørsteBehandlingPåFagsak: Boolean,
        sisteOffsetIKjedeOversikt: Map<String, Int>,
        sisteOffsetPåFagsak: Int? = null
    ): Pair<List<AndelTilkjentYtelseForUtbetalingsoppdrag>, List<Utbetalingsperiode>> {
        var offset = if (!erFørsteBehandlingPåFagsak) {
            sisteOffsetPåFagsak?.plus(1)
                ?: throw IllegalStateException("Skal finnes offset når det ikke er første behandling på fagsak")
        } else {
            0
        }

        val utbetalingsperiode = andeler.filter { kjede -> kjede.isNotEmpty() }
            .flatMap { kjede: List<AndelTilkjentYtelseForUtbetalingsoppdrag> ->
                val ident = kjede.first().aktør.aktivFødselsnummer()
                var forrigeOffsetIKjede: Int? = null
                if (!erFørsteBehandlingPåFagsak) {
                    forrigeOffsetIKjede = sisteOffsetIKjedeOversikt[ident]
                }
                kjede.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                    val forrigeOffset = if (index == 0) forrigeOffsetIKjede else offset - 1
                    UtbetalingsperiodeMal(
                        vedtak = vedtak
                    ).lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                        andel.periodeOffset = offset.toLong()
                        andel.forrigePeriodeOffset = forrigeOffset?.toLong()
                        // Trengs for å finne tilbake ved konsistensavstemming
                        andel.kildeBehandlingId = andel.behandlingId
                        offset++
                    }
                }
            }
        return andeler.flatten() to utbetalingsperiode
    }
}

abstract class AndelTilkjentYtelseForUtbetalingsoppdragFactory {
    abstract fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag>
}

fun Collection<AndelTilkjentYtelse>.pakkInnForUtbetaling(
    andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory
) = andelTilkjentYtelseForUtbetalingsoppdragFactory.pakkInnForUtbetaling(this)

abstract class AndelTilkjentYtelseForUtbetalingsoppdrag(private val andelTilkjentYtelse: AndelTilkjentYtelse) {
    val behandlingId: Long = andelTilkjentYtelse.behandlingId
    val tilkjentYtelse: TilkjentYtelse = andelTilkjentYtelse.tilkjentYtelse
    val kalkulertUtbetalingsbeløp: Int = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
    val stønadFom: YearMonth = andelTilkjentYtelse.stønadFom
    val stønadTom: YearMonth = andelTilkjentYtelse.stønadTom
    val aktør: Aktør = andelTilkjentYtelse.aktør
    val type: YtelseType = andelTilkjentYtelse.type
    abstract var periodeOffset: Long?
    abstract var forrigePeriodeOffset: Long?
    abstract var kildeBehandlingId: Long?

    override fun equals(other: Any?): Boolean {
        return if (other is AndelTilkjentYtelseForUtbetalingsoppdrag) {
            this.andelTilkjentYtelse == other.andelTilkjentYtelse
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return andelTilkjentYtelse.hashCode()
    }
}

class AndelTilkjentYtelseForIverksetting(
    private val andelTilkjentYtelse: AndelTilkjentYtelse
) : AndelTilkjentYtelseForUtbetalingsoppdrag(andelTilkjentYtelse) {

    override var periodeOffset: Long?
        get() = andelTilkjentYtelse.periodeOffset
        set(value) {
            andelTilkjentYtelse.periodeOffset = value
        }

    override var forrigePeriodeOffset: Long?
        get() = andelTilkjentYtelse.forrigePeriodeOffset
        set(value) {
            andelTilkjentYtelse.forrigePeriodeOffset = value
        }

    override var kildeBehandlingId: Long?
        get() = andelTilkjentYtelse.kildeBehandlingId
        set(value) {
            andelTilkjentYtelse.kildeBehandlingId = value
        }

    companion object Factory : AndelTilkjentYtelseForUtbetalingsoppdragFactory() {
        override fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag> =
            andelerTilkjentYtelse.map { AndelTilkjentYtelseForIverksetting(it) }
    }
}

class AndelTilkjentYtelseForSimulering(
    andelTilkjentYtelse: AndelTilkjentYtelse
) : AndelTilkjentYtelseForUtbetalingsoppdrag(andelTilkjentYtelse) {

    override var periodeOffset: Long? = andelTilkjentYtelse.periodeOffset
    override var forrigePeriodeOffset: Long? = andelTilkjentYtelse.forrigePeriodeOffset
    override var kildeBehandlingId: Long? = andelTilkjentYtelse.kildeBehandlingId

    companion object Factory : AndelTilkjentYtelseForUtbetalingsoppdragFactory() {
        override fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag> =
            andelerTilkjentYtelse.map { AndelTilkjentYtelseForSimulering(it) }
    }
}
