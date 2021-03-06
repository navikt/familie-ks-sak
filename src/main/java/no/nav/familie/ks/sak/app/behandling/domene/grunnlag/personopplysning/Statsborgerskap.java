package no.nav.familie.ks.sak.app.behandling.domene.grunnlag.personopplysning;

import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.Landkode;
import no.nav.familie.ks.sak.app.behandling.domene.typer.AktørId;
import no.nav.familie.ks.sak.app.behandling.domene.typer.BaseEntitet;
import no.nav.familie.ks.sak.app.behandling.domene.typer.DatoIntervallEntitet;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "PO_STATSBORGERSKAP")
public class Statsborgerskap extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PO_STATSBORGERSKAP_SEQ")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "kode", column = @Column(name = "statsborgerskap")))
    private Landkode statsborgerskap = Landkode.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    private Person person;

    Statsborgerskap() {
    }

    Statsborgerskap(Statsborgerskap statsborgerskap) {
        this.aktørId = statsborgerskap.getAktørId();
        this.periode = statsborgerskap.getPeriode();
        this.statsborgerskap = statsborgerskap.getStatsborgerskap();
    }

    public Statsborgerskap(AktørId aktørId, DatoIntervallEntitet periode, Landkode statsborgerskap) {
        Objects.requireNonNull(aktørId, "AktørId");
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(statsborgerskap, "statsborgerskap");
        this.aktørId = aktørId;
        this.periode = periode;
        this.statsborgerskap = statsborgerskap;
    }

    void setPerson(Person person) {
        this.person = person;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet gyldighetsperiode) {
        this.periode = gyldighetsperiode;
    }

    public Landkode getStatsborgerskap() {
        return statsborgerskap;
    }

    void setStatsborgerskap(Landkode statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statsborgerskap entitet = (Statsborgerskap) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
            Objects.equals(periode, entitet.periode) &&
            Objects.equals(statsborgerskap, entitet.statsborgerskap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, statsborgerskap);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StatsborgerskapEntitet{");
        sb.append("gyldighetsperiode=").append(periode);
        sb.append(", statsborgerskap=").append(statsborgerskap);
        sb.append('}');
        return sb.toString();
    }

}
