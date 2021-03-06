package no.nav.familie.ks.sak.app.behandling.domene.resultat.vilkår;

import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.AvvikType;
import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.UtfallType;
import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.VilkårType;
import no.nav.familie.ks.sak.app.behandling.domene.typer.BaseEntitet;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "VILKAR_RESULTAT")
public class VilkårResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq")
    @SequenceGenerator(name = "vilkar_resultat_seq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "samlet_vilkar_resultat_id")
    private SamletVilkårResultat samletVilkårResultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "vilkar")
    private VilkårType vilkårType;

    @Enumerated(EnumType.STRING)
    @Column(name = "avvik")
    private AvvikType avvikType;

    @Enumerated(EnumType.STRING)
    @Column(name = "utfall")
    private UtfallType utfall = UtfallType.IKKE_VURDERT;

    @Column(name = "regel_input", updatable = false, columnDefinition = "text")
    private String regelInput;

    @Column(name = "regel_output", updatable = false, columnDefinition = "text")
    private String regelOutput;

    VilkårResultat() {
    }

    public VilkårResultat(VilkårType vilkårType, UtfallType utfall, String regelInput, String regelOutput) {
        this.vilkårType = vilkårType;
        this.utfall = utfall;
        this.regelInput = regelInput;
        this.regelOutput = regelOutput;
    }

    public VilkårResultat(AvvikType avvikType, UtfallType utfall) {
        this.avvikType = avvikType;
        this.utfall = utfall;
    }

    void setSamletVilkårResultat(SamletVilkårResultat samletVilkårResultat) {
        this.samletVilkårResultat = samletVilkårResultat;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }


    public AvvikType getAvvikType() {
        return avvikType;
    }

    public UtfallType getUtfall() {
        return utfall;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VilkårResultat that = (VilkårResultat) o;
        return vilkårType == that.vilkårType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vilkårType);
    }

    @Override
    public String toString() {
        return "VilkårResultat{" +
                "id=" + id +
                ", vilkårType=" + vilkårType +
                ", utfall=" + utfall +
                '}';
    }
}
