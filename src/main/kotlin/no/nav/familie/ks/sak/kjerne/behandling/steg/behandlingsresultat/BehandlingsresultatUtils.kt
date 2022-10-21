package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validerBarnasVilkår
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelValidator.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelValidator.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

object BehandlingsresultatUtils {

    fun validerAtBehandlingsresultatKanUtføres(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        tilkjentYtelse: TilkjentYtelse,
        endretUtbetalingMedAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
    ) {
        val behandling = vilkårsvurdering.behandling
        // valider vilkårsvurdering
        if (behandling.type != BehandlingType.TEKNISK_ENDRING) {
            validerBarnasVilkår(vilkårsvurdering, personopplysningGrunnlag.barna)
        }
        // valider TilkjentYtelse // TODO ikke komplett ennå, implementeres det med Sats
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
}
