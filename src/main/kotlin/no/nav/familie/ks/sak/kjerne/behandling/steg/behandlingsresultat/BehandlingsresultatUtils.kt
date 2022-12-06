package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_ENDRET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTBETALING
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTEN_UTBETALING
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_ENDRET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.OPPHØRT
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

object BehandlingsresultatUtils {

    fun validerAtBehandlingsresultatKanUtføres(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        tilkjentYtelse: TilkjentYtelse,
        endretUtbetalingMedAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
    ) {
        // valider TilkjentYtelse
        TilkjentYtelseValidator.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)

        // valider EndretUtbetalingAndel
        validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingMedAndeler.map { it.endretUtbetaling })
        validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingMedAndeler)
    }

    fun validerUtledetBehandlingsresultat(behandling: Behandling, behandlingsresultat: Behandlingsresultat) {
        when {
            behandling.type !in behandlingsresultat.gyldigeBehandlingstyper -> {
                val feilmelding = "Behandlingsresultatet ${behandlingsresultat.displayName.lowercase()} " +
                    "er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'."
                throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
            }
            behandlingsresultat.erAvslått() && behandling.erKlage() -> {
                val feilmelding = "Behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn.lowercase()} " +
                    "er ugyldig i kombinasjon med resultat '${behandlingsresultat.displayName.lowercase()}'."
                throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
            }
        }
    }

    fun utledBehandlingsresultatDataForPerson(
        person: Person,
        personerFremstiltKravFor: List<Aktør>,
        andelerMedEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        forrigeAndelerMedEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        erEksplisittAvslag: Boolean
    ): BehandlingsresultatPerson {
        val aktør = person.aktør

        return BehandlingsresultatPerson(
            aktør = aktør,
            søktForPerson = personerFremstiltKravFor.contains(aktør),
            forrigeAndeler =
            forrigeAndelerMedEndringer.filter { it.aktør == aktør }
                .map { andelTilkjentYtelse ->
                    BehandlingsresultatAndelTilkjentYtelse(
                        stønadFom = andelTilkjentYtelse.stønadFom,
                        stønadTom = andelTilkjentYtelse.stønadTom,
                        kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                    )
                },
            andeler = andelerMedEndringer.filter { it.aktør == aktør }
                .map { andelTilkjentYtelse ->
                    BehandlingsresultatAndelTilkjentYtelse(
                        stønadFom = andelTilkjentYtelse.stønadFom,
                        stønadTom = andelTilkjentYtelse.stønadTom,
                        kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                    )
                },
            eksplisittAvslag = erEksplisittAvslag
        )
    }

    fun utledBehandlingsresuiltatBasertPåYtelsePersonResulater(ytelsePersonResultater: Set<YtelsePersonResultat>): Behandlingsresultat {
        // Alle Behandlinsresultatene er importert. Så trenger ikke å bruke Behandlingsresulat.FORTSATT_INNVILGET
        return when {
            // Innvilget
            ytelsePersonResultater.isEmpty() -> FORTSATT_INNVILGET
            ytelsePersonResultater.eq(YtelsePersonResultat.INNVILGET) -> INNVILGET

            // Opphørt
            ytelsePersonResultater.eq(YtelsePersonResultat.FORTSATT_OPPHØRT) -> FORTSATT_OPPHØRT
            ytelsePersonResultater.eq(YtelsePersonResultat.OPPHØRT) -> OPPHØRT
            ytelsePersonResultater.eq(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.FORTSATT_OPPHØRT) -> OPPHØRT

            // Endret
            ytelsePersonResultater.eq(YtelsePersonResultat.ENDRET_UTBETALING) -> ENDRET_UTBETALING
            ytelsePersonResultater.eq(YtelsePersonResultat.ENDRET_UTEN_UTBETALING) -> ENDRET_UTEN_UTBETALING
            ytelsePersonResultater.eq(
                YtelsePersonResultat.ENDRET_UTBETALING,
                YtelsePersonResultat.ENDRET_UTEN_UTBETALING
            ) -> ENDRET_UTBETALING

            // Avslått
            ytelsePersonResultater.eq(YtelsePersonResultat.AVSLÅTT) -> AVSLÅTT
            // for å få riktig brevmål AVSLÅTT siden det var ingen endring fra forrige
            ytelsePersonResultater.eq(YtelsePersonResultat.AVSLÅTT, YtelsePersonResultat.FORTSATT_OPPHØRT) -> AVSLÅTT

            // Innvilget & Opphørt
            ytelsePersonResultater.matcherAltOgHarOpphørtResultat(YtelsePersonResultat.INNVILGET) -> INNVILGET_OG_OPPHØRT

            // Innvilget og Endret
            ytelsePersonResultater.matcherAltOgHarEndretResultat(YtelsePersonResultat.INNVILGET) -> INNVILGET_OG_ENDRET

            // Innvilget, Endret og Opphørt
            ytelsePersonResultater.matcherAltOgHarBådeEndretOgOpphørtResultat(YtelsePersonResultat.INNVILGET) -> INNVILGET_ENDRET_OG_OPPHØRT

            // Delvis Innvilget
            ytelsePersonResultater.eq(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT) -> DELVIS_INNVILGET

            // Delvis Innvilget og Opphørt
            ytelsePersonResultater.matcherAltOgHarOpphørtResultat(delvisInnvilgetKombinasjoner) -> DELVIS_INNVILGET_OG_OPPHØRT

            // Delvis Innvilget og Endret
            ytelsePersonResultater.matcherAltOgHarEndretResultat(delvisInnvilgetKombinasjoner) -> DELVIS_INNVILGET_OG_ENDRET

            // Delvis Innvilget og Endret
            ytelsePersonResultater.matcherAltOgHarBådeEndretOgOpphørtResultat(delvisInnvilgetKombinasjoner) -> DELVIS_INNVILGET_ENDRET_OG_OPPHØRT

            // Endret og Opphørt
            ytelsePersonResultater.matcherAltOgHarBådeEndretOgOpphørtResultat() -> ENDRET_OG_OPPHØRT

            // Avslått og Opphørt, Her trenger å matchhe OPPHØRT spesifikt fordi det kun kan ha FORTSATT_OPPHØRT også. Da får vi AVSLÅTT
            ytelsePersonResultater.matcherAltOgHarOpphørtResultat(YtelsePersonResultat.AVSLÅTT, YtelsePersonResultat.OPPHØRT) -> AVSLÅTT_OG_OPPHØRT

            // Avslått og Endret
            ytelsePersonResultater.matcherAltOgHarEndretResultat(YtelsePersonResultat.AVSLÅTT) -> AVSLÅTT_OG_ENDRET

            // Avslått, Endret og Opphørt
            ytelsePersonResultater.matcherAltOgHarBådeEndretOgOpphørtResultat(YtelsePersonResultat.AVSLÅTT) -> AVSLÅTT_ENDRET_OG_OPPHØRT

            else -> throw Feil(
                frontendFeilmelding = "Behandlingsresultatet du har fått på behandlingen er ikke støttet i løsningen enda. " +
                    "Ta kontakt med Team familie om du er uenig i resultatet.",
                message = "Kombiansjonen av behandlingsresultatene $ytelsePersonResultater er ikke støttet i løsningen."
            )
        }
    }

    private fun Set<YtelsePersonResultat>.eq(vararg ytelsePersonResultat: YtelsePersonResultat): Boolean =
        this == ytelsePersonResultat.toSet()

    private fun Set<YtelsePersonResultat>.matcherAltOgHarBådeEndretOgOpphørtResultat(vararg andreElementer: Any): Boolean {
        val endretResultat = this.singleOrNull {
            it == YtelsePersonResultat.ENDRET_UTBETALING || it == YtelsePersonResultat.ENDRET_UTEN_UTBETALING
        } ?: return false

        val opphørtResultat = this.intersect(setOf(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.FORTSATT_OPPHØRT))

        return if (opphørtResultat.isEmpty()) false else this == setOf(endretResultat) + opphørtResultat + andreElementer.toSet()
    }

    private fun Set<YtelsePersonResultat>.matcherAltOgHarEndretResultat(vararg andreElementer: Any): Boolean {
        val endretResultat = this.singleOrNull {
            it == YtelsePersonResultat.ENDRET_UTBETALING || it == YtelsePersonResultat.ENDRET_UTEN_UTBETALING
        } ?: return false
        return this == setOf(endretResultat) + andreElementer.toSet()
    }

    private fun Set<YtelsePersonResultat>.matcherAltOgHarOpphørtResultat(vararg andreElementer: Any): Boolean {
        val opphørtResultat = this.intersect(setOf(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.FORTSATT_OPPHØRT))
        return if (opphørtResultat.isEmpty()) false else this == andreElementer.toSet() + opphørtResultat
    }

    private val delvisInnvilgetKombinasjoner = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT)
}
