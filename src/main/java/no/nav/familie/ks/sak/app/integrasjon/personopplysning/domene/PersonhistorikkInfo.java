package no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.adresse.AdressePeriode;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.status.PersonstatusPeriode;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.tilhørighet.StatsborgerskapPeriode;

public class PersonhistorikkInfo {

    private PersonIdent personIdent;
    private List<PersonstatusPeriode> personstatushistorikk = new ArrayList<>();
    private List<StatsborgerskapPeriode> statsborgerskaphistorikk = new ArrayList<>();
    private List<AdressePeriode> adressehistorikk = new ArrayList<>();

    private PersonhistorikkInfo() {
    }

    public List<PersonstatusPeriode> getPersonstatushistorikk() {
        return this.personstatushistorikk;
    }

    public List<StatsborgerskapPeriode> getStatsborgerskaphistorikk() {
        return this.statsborgerskaphistorikk;
    }

    public List<AdressePeriode> getAdressehistorikk() {
        return this.adressehistorikk;
    }

    public PersonIdent getPersonIdent() {
        return this.personIdent;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PersonhistorikkInfo{");
        sb.append("personstatushistorikk=").append(personstatushistorikk);
        sb.append(", statsborgerskaphistorikk=").append(statsborgerskaphistorikk);
        sb.append(", adressehistorikk=").append(adressehistorikk);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonhistorikkInfo that = (PersonhistorikkInfo) o;
        return Objects.equals(personIdent, that.personIdent) &&
            Objects.equals(personstatushistorikk, that.personstatushistorikk) &&
            Objects.equals(statsborgerskaphistorikk, that.statsborgerskaphistorikk) &&
            Objects.equals(adressehistorikk, that.adressehistorikk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personIdent, personstatushistorikk, statsborgerskaphistorikk, adressehistorikk);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PersonhistorikkInfo kladd;

        public Builder() {
            this.kladd = new PersonhistorikkInfo();
        }

        public Builder medPersonIdent(PersonIdent personIdent) {
            this.kladd.personIdent = personIdent;
            return this;
        }

        public Builder leggTil(PersonstatusPeriode personstatus) {
            this.kladd.personstatushistorikk.add(personstatus);
            return this;
        }

        public Builder leggTil(StatsborgerskapPeriode statsborgerskap) {
            this.kladd.statsborgerskaphistorikk.add(statsborgerskap);
            return this;
        }

        public Builder leggTil(AdressePeriode adresse) {
            this.kladd.adressehistorikk.add(adresse);
            return this;
        }

        public PersonhistorikkInfo build() {
            requireNonNull(kladd.personIdent, "PersonhistorikkInfo må ha personIdent");
            return kladd;
        }
    }
}
