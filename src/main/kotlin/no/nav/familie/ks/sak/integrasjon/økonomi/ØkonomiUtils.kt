package no.nav.familie.ks.sak.integrasjon.økonomi

import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.AndelTilkjentYtelseForUtbetalingsoppdrag
import java.time.YearMonth

object ØkonomiUtils {

    /**
     * Deler andeler inn i gruppene de skal kjedes i. Utbetalingsperioder kobles i kjeder per person
     *
     * @param[andelerForInndeling] andeler som skal sorteres i grupper for kjeding
     * @return ident med kjedegruppe.
     */
    fun kjedeinndelteAndeler(andelerForInndeling: List<AndelTilkjentYtelseForUtbetalingsoppdrag>): Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> =
        andelerForInndeling.groupBy { it.aktør.aktivFødselsnummer() }

    /**
     * Finn alle presidenter i forrige og oppdatert liste. Presidentene er identifikatorn for hver kjede.
     * Set andeler tilkjentytelse til null som indikerer at hele kjeden skal opphøre.
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map med personident og andel=null som markerer at alle andeler skal opphøres.
     */
    fun sisteAndelPerKjede(
        forrigeKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>
    ): Map<String, AndelTilkjentYtelseForUtbetalingsoppdrag?> =
        forrigeKjeder.keys.union(oppdaterteKjeder.keys).associateWith { null }

    /**
     * Lager oversikt over siste andel i hver kjede som finnes uten endring i oppdatert tilstand.
     * Vi må opphøre og eventuelt gjenoppbygge hver kjede etter denne. Må ta vare på andel og ikke kun offset da
     * filtrering av oppdaterte andeler senere skjer før offset blir satt.
     * Personident er identifikator for hver kjede.
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map med personident og siste bestående andel. Bestående andel=null dersom alle opphøres eller ny person.
     */
    fun sisteBeståendeAndelPerKjede(
        forrigeKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>
    ): Map<String, AndelTilkjentYtelseForUtbetalingsoppdrag?> {
        val allePersoner = forrigeKjeder.keys.union(oppdaterteKjeder.keys)
        return allePersoner.associateWith { kjedeIdentifikator ->
            beståendeAndelerIKjede(
                forrigeKjede = forrigeKjeder[kjedeIdentifikator],
                oppdatertKjede = oppdaterteKjeder[kjedeIdentifikator]
            )
                ?.sortedBy { it.periodeOffset }?.lastOrNull()
        }
    }

    /**
     * Tar utgangspunkt i ny tilstand og finner andeler som må bygges opp (nye, endrede og bestående etter første endring)
     *
     * @param[oppdaterteKjeder] ny tilstand
     * @param[sisteBeståendeAndelIHverKjede] andeler man må bygge opp etter
     * @return andeler som må bygges fordelt på kjeder
     */
    fun andelerTilOpprettelse(
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelseForUtbetalingsoppdrag?>
    ): List<List<AndelTilkjentYtelseForUtbetalingsoppdrag>> =
        oppdaterteKjeder.map { (kjedeIdentifikator, oppdatertKjedeTilstand) ->
            if (sisteBeståendeAndelIHverKjede[kjedeIdentifikator] != null) {
                oppdatertKjedeTilstand.filter { it.stønadFom.isAfter(sisteBeståendeAndelIHverKjede[kjedeIdentifikator]!!.stønadTom) }
            } else {
                oppdatertKjedeTilstand
            }
        }.filter { it.isNotEmpty() }

    /**
     * Tar utgangspunkt i forrige tilstand og finner kjeder med andeler til opphør og tilhørende opphørsdato
     *
     * @param[forrigeKjeder] ny tilstand
     * @param[sisteBeståendeAndelIHverKjede] andeler man må bygge opp etter
     * @return map av siste andel og opphørsdato fra kjeder med opphør
     */
    fun andelerTilOpphørMedDato(
        forrigeKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelseForUtbetalingsoppdrag?>,
    ): List<Pair<AndelTilkjentYtelseForUtbetalingsoppdrag, YearMonth>> =
        forrigeKjeder
            .mapValues { (person, forrigeAndeler) ->
                forrigeAndeler.filter {
                    altIKjedeOpphøres(person, sisteBeståendeAndelIHverKjede) ||
                        andelOpphøres(person, it, sisteBeståendeAndelIHverKjede)
                }
            }
            .filter { (_, andelerSomOpphøres) -> andelerSomOpphøres.isNotEmpty() }
            .mapValues { andelForKjede -> andelForKjede.value.sortedBy { it.stønadFom } }
            .map { (_, kjedeEtterFørsteEndring) ->
                kjedeEtterFørsteEndring.last() to (kjedeEtterFørsteEndring.first().stønadFom)
            }

    private fun andelOpphøres(
        kjedeidentifikator: String,
        andel: AndelTilkjentYtelseForUtbetalingsoppdrag,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelseForUtbetalingsoppdrag?>
    ): Boolean = andel.stønadFom > sisteBeståendeAndelIHverKjede[kjedeidentifikator]!!.stønadTom

    private fun altIKjedeOpphøres(
        kjedeidentifikator: String,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelseForUtbetalingsoppdrag?>
    ): Boolean = sisteBeståendeAndelIHverKjede[kjedeidentifikator] == null

    /**
     * Setter eksisterende offset og kilde på andeler som skal bestå
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map med personident og oppdaterte kjeder
     */
    fun oppdaterBeståendeAndelerMedOffset(
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        forrigeKjeder: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>
    ): Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> {
        oppdaterteKjeder
            .filter { forrigeKjeder.containsKey(it.key) }
            .forEach { (kjedeIdentifikator, oppdatertKjede) ->
                val beståendeFraForrige =
                    beståendeAndelerIKjede(
                        forrigeKjede = forrigeKjeder.getValue(kjedeIdentifikator),
                        oppdatertKjede = oppdatertKjede
                    )
                beståendeFraForrige?.forEach { bestående ->
                    val beståendeIOppdatert = oppdatertKjede.find { it.erTilsvarendeForUtbetaling(bestående) }
                        ?: error("Kan ikke finne andel fra utledet bestående andeler i oppdatert tilstand.")

                    beståendeIOppdatert.periodeOffset = bestående.periodeOffset
                    beståendeIOppdatert.forrigePeriodeOffset = bestående.forrigePeriodeOffset
                    beståendeIOppdatert.kildeBehandlingId = bestående.kildeBehandlingId
                }
            }
        return oppdaterteKjeder
    }

    fun gjeldendeForrigeOffsetForKjede(andelerFraForrigeBehandling: Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>): Map<String, Int> =
        andelerFraForrigeBehandling.map { (personIdent, forrigeKjede) ->
            personIdent to (
                forrigeKjede.filter { it.kalkulertUtbetalingsbeløp > 0 }
                    .maxByOrNull { andel -> andel.periodeOffset!! }?.periodeOffset?.toInt()
                    ?: throw IllegalStateException("Andel i kjede skal ha offset")
                )
        }.toMap()

    private fun beståendeAndelerIKjede(
        forrigeKjede: List<AndelTilkjentYtelseForUtbetalingsoppdrag>?,
        oppdatertKjede: List<AndelTilkjentYtelseForUtbetalingsoppdrag>?
    ): List<AndelTilkjentYtelseForUtbetalingsoppdrag>? {
        val forrige = forrigeKjede?.toSet() ?: emptySet()
        val oppdatert = oppdatertKjede?.toSet() ?: emptySet()
        val førsteEndring = forrige.disjunkteAndeler(oppdatert).minByOrNull { it.stønadFom }?.stønadFom
        return if (førsteEndring != null) {
            forrige.snittAndeler(oppdatert)
                .filter { it.stønadFom.isBefore(førsteEndring) }
        } else {
            forrigeKjede ?: emptyList()
        }
    }

    private fun Set<AndelTilkjentYtelseForUtbetalingsoppdrag>.snittAndeler(other: Set<AndelTilkjentYtelseForUtbetalingsoppdrag>): Set<AndelTilkjentYtelseForUtbetalingsoppdrag> {
        val andelerKunIDenne = this.subtractAndeler(other)
        return this.subtractAndeler(andelerKunIDenne)
    }

    private fun Set<AndelTilkjentYtelseForUtbetalingsoppdrag>.subtractAndeler(other: Set<AndelTilkjentYtelseForUtbetalingsoppdrag>): Set<AndelTilkjentYtelseForUtbetalingsoppdrag> {
        return this.filter { a ->
            other.none { b -> a.erTilsvarendeForUtbetaling(b) }
        }.toSet()
    }

    private fun Set<AndelTilkjentYtelseForUtbetalingsoppdrag>.disjunkteAndeler(other: Set<AndelTilkjentYtelseForUtbetalingsoppdrag>): Set<AndelTilkjentYtelseForUtbetalingsoppdrag> {
        val andelerKunIDenne = this.subtractAndeler(other)
        val andelerKunIAnnen = other.subtractAndeler(this)
        return andelerKunIDenne.union(andelerKunIAnnen)
    }

    private fun AndelTilkjentYtelseForUtbetalingsoppdrag.erTilsvarendeForUtbetaling(other: AndelTilkjentYtelseForUtbetalingsoppdrag): Boolean {
        return (
            this.aktør == other.aktør &&
                this.stønadFom == other.stønadFom &&
                this.stønadTom == other.stønadTom &&
                this.kalkulertUtbetalingsbeløp == other.kalkulertUtbetalingsbeløp &&
                this.type == other.type
            )
    }
}
