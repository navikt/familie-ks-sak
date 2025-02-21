package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AutomatiskSatteVilkårUtilsKtTest {
    private val fødselsDato = LocalDate.of(2024, 1, 1)
    private val behandlingId = 0L
    private val personResultat =
        lagPersonResultat(
            vilkårsvurdering = lagVilkårsvurdering(),
            lagVilkårResultater = { emptySet() },
        )

    @Test
    fun `skal bruke nytt regelverk for barn født fra og med første januar 2024 hvis toggle er skrudd på`() {
        // Act
        val barnetsAlderVilkår =
            lagAutomatiskGenererteVilkårForBarnetsAlder(
                personResultat = personResultat,
                behandlingId = behandlingId,
                fødselsdato = fødselsDato,
                adopsjonsdato = null,
            )

        // Assert
        assertThat(barnetsAlderVilkår).hasSize(1)
        assertThat(barnetsAlderVilkår).anySatisfy {
            validerFellesfelter(it)
            assertThat(it.periodeFom).isEqualTo(fødselsDato.plusMonths(12))
            assertThat(it.periodeTom).isEqualTo(fødselsDato.plusMonths(20))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
    }

    @Test
    fun `skal bruke nytt regelverk for barn adoptert fra og med første januar 2024 hvis toggle er skrudd på`() {
        // Act
        val barnetsAlderVilkår =
            lagAutomatiskGenererteVilkårForBarnetsAlder(
                personResultat = personResultat,
                behandlingId = behandlingId,
                fødselsdato = fødselsDato,
                adopsjonsdato = fødselsDato.plusMonths(10),
            )

        // Assert
        assertThat(barnetsAlderVilkår).hasSize(1)
        assertThat(barnetsAlderVilkår).anySatisfy {
            validerFellesfelter(it)
            assertThat(it.periodeFom).isEqualTo(fødselsDato.plusMonths(12))
            assertThat(it.periodeTom).isEqualTo(fødselsDato.plusMonths(20))
            assertThat(it.utdypendeVilkårsvurderinger).containsExactly(UtdypendeVilkårsvurdering.ADOPSJON)
        }
    }

    @Test
    fun `skal bruke gammelt regelverk for barn født før første januar 2024 hvis toggle er skrudd på`() {
        val fødselsDatoFørJan24 = LocalDate.of(2023, 12, 31)

        // Act
        val barnetsAlderVilkår =
            lagAutomatiskGenererteVilkårForBarnetsAlder(
                personResultat = personResultat,
                behandlingId = behandlingId,
                fødselsdato = fødselsDatoFørJan24,
                adopsjonsdato = null,
            )

        // Assert
        assertThat(barnetsAlderVilkår).hasSize(1)
        assertThat(barnetsAlderVilkår).anySatisfy {
            validerFellesfelter(it)
            assertThat(it.periodeFom).isEqualTo(fødselsDatoFørJan24.plusMonths(13))
            assertThat(it.periodeTom).isEqualTo(fødselsDatoFørJan24.plusMonths(19))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
    }

    private fun validerFellesfelter(it: VilkårResultat) {
        assertThat(it.personResultat).isEqualTo(personResultat)
        assertThat(it.erAutomatiskVurdert).isTrue()
        assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(it.vilkårType).isEqualTo(Vilkår.BARNETS_ALDER)
        assertThat(it.begrunnelse).isEqualTo("Vurdert og satt automatisk")
        assertThat(it.behandlingId).isEqualTo(behandlingId)
    }
}
