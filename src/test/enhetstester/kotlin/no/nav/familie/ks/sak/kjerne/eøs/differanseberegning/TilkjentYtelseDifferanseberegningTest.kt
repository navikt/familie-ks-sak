package no.nav.familie.ks.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår.BARNEHAGEPLASS
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår.BARNETS_ALDER
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår.MEDLEMSKAP
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår.MEDLEMSKAP_ANNEN_FORELDER
import no.nav.familie.ks.sak.kjerne.eøs.util.DeltBostedBuilder
import no.nav.familie.ks.sak.kjerne.eøs.util.TilkjentYtelseBuilder
import no.nav.familie.ks.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ks.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ks.sak.kjerne.eøs.util.oppdaterTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ks.sak.kjerne.tidslinje.util.byggTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Merk at operasjoner som tilsynelatende lager en ny instans av TilkjentYtelse, faktisk returner samme.
 * Det skyldes at JPA krever muterbare objekter.
 * Ikke-muterbarhet krever en omskrivning av koden. F.eks å koble vekk EndretUtbetalingPeriode fra AndelTilkjentYtelse
 */
class TilkjentYtelseDifferanseberegningTest {
    private fun Int.jan(år: Int): LocalDate = LocalDate.of(år, 1, this)

    @Test
    fun `skal gjøre differanseberegning på en tilkjent ytelse med endringsperioder`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val behandling = lagBehandling()
        val behandlingId = BehandlingId(behandling.id)
        val startMåned = barnsFødselsdato.toYearMonth()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, startMåned)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", BOSATT_I_RIKET)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", MEDLEMSKAP)
                .forPerson(barn1, startMåned)
                .medVilkår("+>", BARNETS_ALDER, BARNEHAGEPLASS)
                .medVilkår("E>", BOSATT_I_RIKET, MEDLEMSKAP, BOR_MED_SØKER, MEDLEMSKAP_ANNEN_FORELDER)
                .forPerson(barn2, startMåned)
                .medVilkår("+>", BARNETS_ALDER, BARNEHAGEPLASS)
                .medVilkår("E>", BOSATT_I_RIKET, MEDLEMSKAP, BOR_MED_SØKER, MEDLEMSKAP_ANNEN_FORELDER)
                .byggPerson()

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse()

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        DeltBostedBuilder(startMåned, tilkjentYtelse)
            .medDeltBosted(" //////00000000001111>", barn1, barn2)
            .oppdaterTilkjentYtelse()

        val forventetTilkjentYtelseMedDelt =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1, barn2)
                .medOrdinær(" $$$$$$", prosent = 50) { it / 2 }
                .medOrdinær("       $$$$$$$$$$", prosent = 0) { 0 }
                .medOrdinær("                 $$$$$", prosent = 100) { it }
                .bygg()

        assertThat(tilkjentYtelse.andelerTilkjentYtelse)
            .containsAll(forventetTilkjentYtelseMedDelt.andelerTilkjentYtelse)
            .hasSize(forventetTilkjentYtelseMedDelt.andelerTilkjentYtelse.size)

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
                .medBeløp(" 44555666>", "EUR", "fr", barn1, barn2)
                .bygg()

        val valutakurser =
            ValutakursBuilder(startMåned, behandlingId)
                .medKurs(" 888899999>", "EUR", barn1, barn2)
                .bygg()

        val forventetTilkjentYtelseMedDiff =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1, barn2)
                .medOrdinær(" $$", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 32 }) { it / 2 - 32 }
                .medOrdinær("   $$", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 40 }) { it / 2 - 40 }
                .medOrdinær("     $", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 45 }) { it / 2 - 45 }
                .medOrdinær("      $", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 54 }) { it / 2 - 54 }
                .medOrdinær("       $$$$$$$$$$", 0, nasjonalt = { 0 }, differanse = { -54 }) { 0 }
                .medOrdinær("                 $$$$$", 100, nasjonalt = { it }, differanse = { it - 54 }) { it - 54 }
                .bygg()

        val andelerMedDifferanse =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse.toList(), utenlandskePeriodebeløp, valutakurser)

        assertThat(andelerMedDifferanse)
            .containsAll(forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse)
            .hasSize(forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse.size)
    }

    @Test
    fun `skal fjerne differanseberegning når utenlandsk periodebeløp eller valutakurs nullstilles`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val behandling = lagBehandling()
        val behandlingId = BehandlingId(behandling.id)
        val startMåned = barnsFødselsdato.toYearMonth()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, startMåned)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", BOSATT_I_RIKET)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", MEDLEMSKAP)
                .forPerson(barn1, startMåned)
                .medVilkår("+>", BARNETS_ALDER, BARNEHAGEPLASS)
                .medVilkår("E>", BOSATT_I_RIKET, MEDLEMSKAP, BOR_MED_SØKER, MEDLEMSKAP_ANNEN_FORELDER)
                .byggPerson()

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse()

        val forventetTilkjentYtelseKunSats =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1)
                .medOrdinær(" $$$$$$$$$$$$$$$$$$$$$", nasjonalt = { null }, differanse = { null })
                .bygg()

        assertThat(tilkjentYtelse.andelerTilkjentYtelse)
            .containsAll(forventetTilkjentYtelseKunSats.andelerTilkjentYtelse)
            .hasSize(forventetTilkjentYtelseKunSats.andelerTilkjentYtelse.size)

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
                .medBeløp(" 44555666>", "EUR", "fr", barn1)
                .bygg()

        val valutakurser =
            ValutakursBuilder(startMåned, behandlingId)
                .medKurs(" 888899999>", "EUR", barn1)
                .bygg()

        val forventetTilkjentYtelseMedDiff =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1)
                .medOrdinær(" $$                   ", nasjonalt = { it }, differanse = { it - 32 }) { it - 32 }
                .medOrdinær("   $$                 ", nasjonalt = { it }, differanse = { it - 40 }) { it - 40 }
                .medOrdinær("     $                ", nasjonalt = { it }, differanse = { it - 45 }) { it - 45 }
                .medOrdinær("      $$$$$$$$$$$$$$$$", nasjonalt = { it }, differanse = { it - 54 }) { it - 54 }
                .bygg()

        val andelerMedDiff =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse.toMutableList(), utenlandskePeriodebeløp, valutakurser)

        assertThat(andelerMedDiff)
            .containsAll(forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse)
            .hasSize(forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse.size)

        val blanktUtenlandskPeridebeløp =
            UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
                .medBeløp(" >", null, null, barn1)
                .bygg()

        val andelerUtenDiff =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse.toList(), blanktUtenlandskPeridebeløp, valutakurser)

        assertThat(andelerUtenDiff)
            .containsAll(forventetTilkjentYtelseKunSats.andelerTilkjentYtelse)
            .hasSize(forventetTilkjentYtelseKunSats.andelerTilkjentYtelse.size)

        val andelerMedDiffIgjen =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse.toList(), utenlandskePeriodebeløp, valutakurser)

        assertThat(andelerMedDiffIgjen)
            .containsAll(forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse)
            .hasSize(forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse.size)
    }
}
