package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelValidator
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.utfyltePerioder
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

object BehandlingsresultatValideringUtils {
    internal fun validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
        personerFremstiltKravFor: List<Aktør>,
        personResultater: Set<PersonResultat>,
    ) {
        val personerSomHarEksplisittAvslag = personResultater.filter { it.harEksplisittAvslag() }

        if (personerSomHarEksplisittAvslag.any { !personerFremstiltKravFor.contains(it.aktør) && !it.erSøkersResultater() }) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke er blitt fremstilt krav for.",
                melding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke har blitt fremstilt krav for.",
            )
        }
    }

    fun validerAtBehandlingsresultatKanUtføres(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        tilkjentYtelse: TilkjentYtelse,
        endretUtbetalingMedAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
        overgangsordningAndeler: List<OvergangsordningAndel>,
        personResultaterForBarn: List<PersonResultat>,
        skalValidereOvergangsordningAndeler: Boolean,
    ) {
        val alleBarnetsAlderVilkårResultater = personResultaterForBarn.flatMap { it.vilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BARNETS_ALDER } }
        val barnehageplassVilkårPerPerson = personResultaterForBarn.groupBy { it.aktør }.mapValues { it.value.flatMap { it.vilkårResultater.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS } } }

        // valider TilkjentYtelse
        TilkjentYtelseValidator.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag, alleBarnetsAlderVilkårResultater)

        // valider EndretUtbetalingAndel
        EndretUtbetalingAndelValidator.validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingMedAndeler.map { it.endretUtbetaling })
        EndretUtbetalingAndelValidator.validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingMedAndeler)

        // valider OvergangsordningAndel
        if (skalValidereOvergangsordningAndeler) {
            OvergangsordningAndelValidator.validerAtAlleOpprettedeOvergangsordningAndelerErGyldigUtfylt(overgangsordningAndeler)
            OvergangsordningAndelValidator.validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler(tilkjentYtelse.andelerTilkjentYtelse)
            OvergangsordningAndelValidator.validerAndelerErIPeriodenBarnetEr20Til23Måneder(overgangsordningAndeler.utfyltePerioder())
            OvergangsordningAndelValidator.validerAtBarnehagevilkårErOppfyltForAlleOvergangsordningPerioder(overgangsordningAndeler.utfyltePerioder(), barnehageplassVilkårPerPerson)
        }
    }

    internal fun validerBehandlingsresultat(
        behandling: Behandling,
        resultat: Behandlingsresultat,
    ) {
        if ((
                behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING &&
                    setOf(
                        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                        Behandlingsresultat.ENDRET_UTBETALING,
                        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                        Behandlingsresultat.ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.OPPHØRT,
                        Behandlingsresultat.FORTSATT_INNVILGET,
                        Behandlingsresultat.IKKE_VURDERT,
                    ).contains(resultat)
            ) ||
            (behandling.type == BehandlingType.REVURDERING && resultat == Behandlingsresultat.IKKE_VURDERT)
        ) {
            val feilmelding =
                "Behandlingsresultatet ${resultat.displayName.lowercase()} " +
                    "er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
        if (behandling.opprettetÅrsak == BehandlingÅrsak.KLAGE &&
            setOf(
                Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                Behandlingsresultat.AVSLÅTT,
            ).contains(resultat)
        ) {
            val feilmelding =
                "Behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn.lowercase()} " +
                    "er ugyldig i kombinasjon med resultat '${resultat.displayName.lowercase()}'."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }
}
