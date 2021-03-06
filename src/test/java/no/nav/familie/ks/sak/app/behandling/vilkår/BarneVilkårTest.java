package no.nav.familie.ks.sak.app.behandling.vilkår;

import no.nav.familie.ks.sak.FaktagrunnlagTestBuilder;
import no.nav.familie.ks.sak.app.behandling.fastsetting.Faktagrunnlag;
import no.nav.familie.ks.sak.app.behandling.regel.vilkår.barn.BarneVilkår;
import no.nav.fpsak.nare.evaluation.Resultat;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BarneVilkårTest {

    @Test
    public void norsk_statsborgerskap_på_barn_gir_oppfylt() {
        final var vilkår = new BarneVilkår();
        Faktagrunnlag faktagrunnlag = FaktagrunnlagTestBuilder.familieNorskStatsborgerskapUtenBarnehage();
        final var evaluering = vilkår.evaluer(faktagrunnlag);
        assertThat(evaluering.result()).isEqualByComparingTo(Resultat.JA);
    }

    @Test
    public void utenlandsk_statsborgerskap_på_barn_gir_oppfylt() {
        final var vilkår = new BarneVilkår();
        Faktagrunnlag faktagrunnlag = FaktagrunnlagTestBuilder.familieUtenlandskStatsborgerskapMedBarnehage();
        final var evaluering = vilkår.evaluer(faktagrunnlag);
        assertThat(evaluering.result()).isEqualByComparingTo(Resultat.NEI);
    }
}
