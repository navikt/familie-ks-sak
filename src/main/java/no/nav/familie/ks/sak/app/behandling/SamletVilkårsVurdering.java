package no.nav.familie.ks.sak.app.behandling;

import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.UtfallType;
import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.VilkårType;
import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.årsak.VilkårIkkeOppfyltÅrsak;
import no.nav.familie.ks.sak.app.behandling.fastsetting.Faktagrunnlag;
import no.nav.familie.ks.sak.app.behandling.vilkår.Regelresultat;
import no.nav.familie.ks.sak.app.behandling.vilkår.SamletVurdering;
import no.nav.fpsak.nare.evaluation.Evaluation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SamletVilkårsVurdering implements SamletVurdering {

    private final Map<VilkårType, Evaluation> vurderinger;
    private final Faktagrunnlag faktagrunnlag;

    SamletVilkårsVurdering(Map<VilkårType, Evaluation> vurderinger, Faktagrunnlag faktagrunnlag) {
        this.vurderinger = valider(vurderinger);
        this.faktagrunnlag = faktagrunnlag;
    }

    private Map<VilkårType, Evaluation> valider(Map<VilkårType, Evaluation> vurderinger) {
        vurderinger.entrySet().forEach(this::validerVilkårsVurdering);
        return vurderinger;
    }

    private void validerVilkårsVurdering(Map.Entry<VilkårType, Evaluation> entry) {
        final var outcome = entry.getValue().getOutcome();
        if (outcome instanceof VilkårIkkeOppfyltÅrsak) {
            if (!((VilkårIkkeOppfyltÅrsak) outcome).getVilkårType().equals(entry.getKey())) {
                throw new IllegalStateException("Vilkåret='" + entry.getKey() + "' har en årsak fra et annet vilkår. " + outcome);
            }
        }
    }

    public List<Regelresultat> getResultater() {
        return vurderinger.entrySet().stream()
                .map(entry -> new Regelresultat(entry.getKey(), faktagrunnlag, entry.getValue()))
                .collect(Collectors.toList());
    }

    // FIXME vurder om samlet utfall også kan være manuell_behandling
    @Override
    public UtfallType getSamletUtfallType() {
        final var utfall = getResultater()
                .stream()
                .map(Regelresultat::getUtfallType)
                .distinct()
                .collect(Collectors.toList());

        if (utfall.size() == 1) {
            return utfall.get(0);
        } else {
            return UtfallType.MANUELL_BEHANDLING;
        }
    }
}
