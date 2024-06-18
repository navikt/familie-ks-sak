package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class BehandlingsresultatValideringUtilsTest {
    @Test
    fun `Valider eksplisitt avlag - Skal kaste feil hvis eksplisitt avslått for barn det ikke er fremstilt krav for`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val vilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = randomAktør(),
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val barn1 = randomAktør()
        val barn2 = randomAktør()

        val barn1PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = true,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val barn2PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn2,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = true,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        assertThrows<FunksjonellFeil> {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn2),
            )
        }
    }

    @Test
    fun `Valider eksplisitt avslag - Skal ikke kaste feil hvis person med eksplsitt avslag er fremstilt krav for`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val vilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = randomAktør(),
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val barn1 = randomAktør()
        val barn2 = randomAktør()

        val barn1PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = true,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val barn2PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn2,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = true,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn1, barn2),
            )
        }
    }
}
