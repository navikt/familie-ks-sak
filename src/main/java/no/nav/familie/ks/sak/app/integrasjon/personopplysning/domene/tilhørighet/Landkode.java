package no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.tilhørighet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Landkode {

    public static final Landkode UDEFINERT = new Landkode("UDEFINERT");
    public static final Landkode NORGE = new Landkode("NOR");
    public static final Landkode SVERIGE = new Landkode("SWE");

    private final String kode;

    @JsonCreator
    public Landkode(@JsonProperty("kode") String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public boolean erNorge() {
        return NORGE.equals(this);
    }

    @Override
    public String toString() {
        return "Landkode{" +
                "kode='" + kode + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Landkode landkode = (Landkode) o;
        return Objects.equals(kode, landkode.kode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode);
    }
}
