package no.nav.familie.ks.sak.app.behandling.vilkår;

import no.nav.familie.ks.sak.FaktagrunnlagTestBuilder;
import no.nav.familie.ks.sak.app.behandling.fastsetting.Faktagrunnlag;
import no.nav.familie.ks.sak.app.behandling.regel.mvp.utland.UtlandVilkår;
import no.nav.fpsak.nare.evaluation.Resultat;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UtlandVilkårTest {

    @Test
    public void søknad_med_tilknytning_utland_gir_ikke_oppfylt() {
        final var vilkår = new UtlandVilkår();
        Faktagrunnlag faktagrunnlag = FaktagrunnlagTestBuilder.familieUtenlandskStatsborgerskapMedTilknytningUtland();
        final var evaluering = vilkår.evaluer(faktagrunnlag);
        assertThat(evaluering.result()).isEqualByComparingTo(Resultat.NEI);
    }

    @Test
    public void søknad_uten_tilknytning_utland_gir_oppfylt() {
        final var vilkår = new UtlandVilkår();
        Faktagrunnlag faktagrunnlag = FaktagrunnlagTestBuilder.familieNorskStatsborgerskapUtenBarnehage();
        final var evaluering = vilkår.evaluer(faktagrunnlag);

        assertThat(evaluering.result()).isEqualByComparingTo(Resultat.JA);
    }
}
