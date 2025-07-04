package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.integrasjon.pdl.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.tilPeriode
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

internal enum class Søknadsresultat {
    INNVILGET,
    AVSLÅTT,
    DELVIS_INNVILGET,
    INGEN_RELEVANTE_ENDRINGER,
}

object BehandlingsresultatSøknadUtils {
    internal fun utledResultatPåSøknad(
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        nåværendePersonResultater: Set<PersonResultat>,
        personerFremstiltKravFor: List<Aktør>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
        behandlingÅrsak: BehandlingÅrsak,
        finnesUregistrerteBarn: Boolean,
    ): Søknadsresultat {
        val resultaterFraAndeler =
            utledSøknadResultatFraAndelerTilkjentYtelse(
                forrigeAndeler = forrigeAndeler,
                nåværendeAndeler = nåværendeAndeler,
                personerFremstiltKravFor = personerFremstiltKravFor,
                endretUtbetalingAndeler = endretUtbetalingAndeler,
            )

        val erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIVilkårsvurdering =
            erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIVilkårsvurdering(
                nåværendePersonResultater = nåværendePersonResultater,
                personerFremstiltKravFor = personerFremstiltKravFor,
            )

        val erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIEndretUtbetalingAndeler =
            erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIEndretUtbetalingAndeler(
                nåværendeEndretUtbetalingAndeler = endretUtbetalingAndeler,
                personerFremstiltKravFor = personerFremstiltKravFor,
            )

        val alleResultater =
            (
                if (erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIVilkårsvurdering ||
                    erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIEndretUtbetalingAndeler ||
                    finnesUregistrerteBarn
                ) {
                    resultaterFraAndeler.plus(Søknadsresultat.AVSLÅTT)
                } else {
                    resultaterFraAndeler
                }
            ).distinct()

        return alleResultater.kombinerSøknadsresultater(behandlingÅrsak = behandlingÅrsak)
    }

    internal fun utledSøknadResultatFraAndelerTilkjentYtelse(
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ): List<Søknadsresultat> {
        val alleSøknadsresultater =
            personerFremstiltKravFor.flatMap { aktør ->
                val ytelseTyper = (forrigeAndeler + nåværendeAndeler).map { it.type.tilYtelseType() }.distinct()

                ytelseTyper.flatMap { ytelseType ->
                    utledSøknadResultatFraAndelerTilkjentYtelsePerPersonOgType(
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør && it.type.tilYtelseType() == ytelseType },
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør && it.type.tilYtelseType() == ytelseType },
                        endretUtbetalingAndelerForPerson = endretUtbetalingAndeler.filter { it.personer.any { person -> person.aktør == aktør } },
                    )
                }
            }

        return alleSøknadsresultater.distinct()
    }

    private fun utledSøknadResultatFraAndelerTilkjentYtelsePerPersonOgType(
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>,
        nåværendeAndelerForPerson: List<AndelTilkjentYtelse>,
        endretUtbetalingAndelerForPerson: List<EndretUtbetalingAndel>,
    ): List<Søknadsresultat> {
        val forrigeTidslinje = forrigeAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()
        val nåværendeTidslinje = nåværendeAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()
        val endretUtbetalingTidslinje = endretUtbetalingAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()

        val resultatTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje, endretUtbetalingTidslinje) { nåværende, forrige, endretUtbetalingAndel ->
                val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp
                val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp

                when {
                    nåværendeBeløp == null -> Søknadsresultat.INGEN_RELEVANTE_ENDRINGER // Finnes ikke andel i denne behandlingen
                    forrigeBeløp == null && nåværendeBeløp == 0 -> { // Lagt til ny andel, men den er overstyrt til 0 kr. Må se på årsak for å finne resultat
                        when (endretUtbetalingAndel?.årsak) {
                            null ->
                                if (nåværende.differanseberegnetPeriodebeløp != null) {
                                    Søknadsresultat.INNVILGET
                                } else {
                                    secureLogger.info(
                                        "Andel $nåværende er satt til 0kr, men det skyldes verken differanseberegning eller endret utbetaling andel." +
                                            "\nNåværende andeler: $nåværendeAndelerForPerson" +
                                            "\nEndret utbetaling andeler: $endretUtbetalingAndelerForPerson",
                                    )
                                    throw Feil("Andel er satt til 0 kr, men det skyldes verken differanseberegning eller endret utbetaling andel")
                                }

                            Årsak.ALLEREDE_UTBETALT,
                            Årsak.ETTERBETALING_3MND,
                            Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                            -> Søknadsresultat.AVSLÅTT
                        }
                    }

                    forrigeBeløp != nåværendeBeløp && nåværendeBeløp > 0 -> Søknadsresultat.INNVILGET // Innvilget beløp som er annerledes enn forrige
                    else -> Søknadsresultat.INGEN_RELEVANTE_ENDRINGER
                }
            }

        return resultatTidslinje.tilPerioderIkkeNull().map { it.verdi }.distinct()
    }

    private fun erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIVilkårsvurdering(
        nåværendePersonResultater: Set<PersonResultat>,
        personerFremstiltKravFor: List<Aktør>,
    ): Boolean =
        nåværendePersonResultater
            .filter { personerFremstiltKravFor.contains(it.aktør) || it.erSøkersResultater() }
            .any {
                it.harEksplisittAvslag()
            }

    private fun erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøkerIEndretUtbetalingAndeler(
        nåværendeEndretUtbetalingAndeler: List<EndretUtbetalingAndel>,
        personerFremstiltKravFor: List<Aktør>,
    ): Boolean =
        nåværendeEndretUtbetalingAndeler
            .filter { it.personer.any { person -> personerFremstiltKravFor.contains(person.aktør) } }
            .any { it.erEksplisittAvslagPåSøknad == true }

    internal fun List<Søknadsresultat>.kombinerSøknadsresultater(behandlingÅrsak: BehandlingÅrsak): Søknadsresultat {
        val resultaterUtenIngenEndringer = this.filter { it != Søknadsresultat.INGEN_RELEVANTE_ENDRINGER }

        val ingenSøknadsresultatFeil =
            if (behandlingÅrsak == BehandlingÅrsak.KLAGE) {
                FunksjonellFeil(
                    frontendFeilmelding = "Du har opprettet en revurdering med årsak klage, men ikke innvilget noen perioder. Denne behandlingen kan kun brukes til full omgjøring.",
                    melding = "Klarer ikke utlede søknadsresultat for behandling med årsak klage. Det er ikke innvilget noen perioder.",
                )
            } else {
                FunksjonellFeil(
                    frontendFeilmelding = "Du har opprettet en behandling som følge av søknad, men har enten ikke krysset av for noen barn det er søkt for eller avslått/innvilget noen perioder.",
                    melding = "Klarer ikke utlede søknadsresultat. Finner ingen resultater.",
                )
            }

        return when {
            this.isEmpty() -> throw ingenSøknadsresultatFeil
            this.size == 1 -> this.single()
            resultaterUtenIngenEndringer.size == 1 -> resultaterUtenIngenEndringer.single()
            resultaterUtenIngenEndringer.size == 2 &&
                resultaterUtenIngenEndringer.containsAll(
                    listOf(
                        Søknadsresultat.INNVILGET,
                        Søknadsresultat.AVSLÅTT,
                    ),
                )
            -> Søknadsresultat.DELVIS_INNVILGET

            else -> throw Feil("Klarer ikke kombinere søknadsresultater: $this")
        }
    }
}

fun EndretUtbetalingAndel.tilPeriode() =
    Periode(
        verdi = this,
        fom = this.fom?.førsteDagIInneværendeMåned() ?: throw Feil("Endret utbetaling andel har ingen fom-dato: $this"),
        tom = this.tom?.sisteDagIInneværendeMåned() ?: throw Feil("Endret utbetaling andel har ingen tom-dato: $this"),
    )
