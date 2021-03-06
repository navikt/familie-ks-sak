package no.nav.familie.ks.sak.app.behandling.domene.grunnlag.personopplysning;

import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.Landkode;
import no.nav.familie.ks.sak.app.behandling.domene.typer.AktørId;
import no.nav.familie.ks.sak.app.behandling.domene.typer.BaseEntitet;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.PersonIdent;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity(name = "Person")
@Table(name = "PO_PERSON")
public class Person extends BaseEntitet {

    private static final String REF_NAME = "person";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PO_PERSON_SEQ")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "ident", column = @Column(name = "person_ident", updatable = false)))
    private PersonIdent personIdent;

    @Column(name = "navn")
    private String navn;

    @Column(name = "kjoenn")
    private String kjønn;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private PersonType type; //SØKER, BARN, ANNENPART

    @Column(name = "foedselsdato", nullable = false)
    private LocalDate fødselsdato;

    @Column(name = "doedsdato")
    private LocalDate dødsdato;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "BOSTEDSADRESSE_ID")
    private Bostedsadresse bostedsadresse;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "kode", column = @Column(name = "statsborgerskap")))
    private Landkode statsborgerskap = Landkode.UDEFINERT;

    @OneToMany(mappedBy = REF_NAME, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<PersonAdresse> adresseHistorikk = new ArrayList<>();


    @OneToMany(mappedBy = REF_NAME, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<PersonRelasjon> relasjoner = new ArrayList<>();

    @OneToMany(mappedBy = REF_NAME, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Statsborgerskap> statsborgerskapHistorikk = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_gr_personopplysninger_id", nullable = false, updatable = false)
    private PersonopplysningGrunnlag personopplysningGrunnlag;


    Person() {
    }

    Person(Person person) {
        this.aktørId = person.getAktørId();
        this.navn = person.getNavn();
        this.kjønn = person.getKjønn();
        this.fødselsdato = person.getFødselsdato();
        this.dødsdato = person.getDødsdato();
        this.bostedsadresse = person.getBostedsadressen();
        this.statsborgerskap = person.getStatsborgerskap();
    }

    public Person(PersonIdent personIdent) {
        this.personIdent = personIdent;
    }

    public Person(AktørId aktørId, PersonIdent personIdent) {
        this.aktørId = aktørId;
        this.personIdent = personIdent;
    }
    public Person(AktørId aktørId, PersonIdent personIdent, PersonType personType) {
        this.aktørId = aktørId;
        this.type = personType;
        this.personIdent = personIdent;
    }

    public List<PersonAdresse> getAdresseHistorikk() {
        return adresseHistorikk;
    }

    public void setAdresser(List<PersonAdresse> adresseHistorikk) {
        this.adresseHistorikk = adresseHistorikk;
    }

    public List<PersonRelasjon> getRelasjoner() {
        return relasjoner;
    }

    public void setRelasjoner(List<PersonRelasjon> relasjoner) {
        this.relasjoner = relasjoner;
    }

    public List<Statsborgerskap> getStatsborgerskapHistorikk() {
        return statsborgerskapHistorikk;
    }

    public void setStatsborgerskapHistorikk(List<Statsborgerskap> statsborgerskapHistorikk) {
        this.statsborgerskapHistorikk = statsborgerskapHistorikk;
    }

    public PersonopplysningGrunnlag getPersonopplysningGrunnlag() {
        return personopplysningGrunnlag;
    }

    public PersonType getType() {
        return type;
    }

    public void setType(PersonType type) {
        this.type = type;
    }


    void setPersonopplysningGrunnlag(PersonopplysningGrunnlag personopplysningGrunnlag) {
        this.personopplysningGrunnlag = personopplysningGrunnlag;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public Landkode getStatsborgerskap() {
        return statsborgerskap;
    }

    void setStatsborgerskap(Landkode statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public Person medStatsborgerskap(Landkode statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
        return this;
    }

    public Optional<Bostedsadresse> getBostedsadresse() {
        return Optional.ofNullable(bostedsadresse);
    }

    // Kun for bruk av konstruktøren
    private Bostedsadresse getBostedsadressen() {
        return bostedsadresse;
    }

    public void setBostedsadresse(Bostedsadresse bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
    }

    public Person medBostedsadresse(Bostedsadresse bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
        return this;
    }

    public String getNavn() {
        return navn;
    }

    void setNavn(String navn) {
        this.navn = navn;
    }

    public Person medNavn(String navn) {
        this.navn = navn;
        return this;
    }

    public String getKjønn() {
        return kjønn;
    }

    public void setKjønn(String kjønn) {
        this.kjønn = kjønn;
    }

    public Person medKjønn(String kjønn) {
        this.kjønn = kjønn;
        return this;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public Person medFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
        return this;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    public Person medDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
        return this;
    }

    public Person leggTilStatsborgerskap(Statsborgerskap statsborgerskap) {
        statsborgerskap.setPerson(this);
        this.statsborgerskapHistorikk.add(statsborgerskap);
        return this;
    }
    public Person leggTilAdresse(PersonAdresse adresse) {
        adresse.setPerson(this);
        adresseHistorikk.add(adresse);
        return this;
    }
    public Person leggTilPersonrelasjon(PersonRelasjon relasjon) {
        relasjon.setPerson(this);
        this.relasjoner.add(relasjon);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Person entitet = (Person) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
            Objects.equals(navn, entitet.navn) &&
            Objects.equals(fødselsdato, entitet.fødselsdato) &&
            Objects.equals(dødsdato, entitet.dødsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, navn, fødselsdato, dødsdato);
    }

    @Override
    public String toString() {
        return "PersonopplysningEntitet{" + "id=" + id +
            ", type=" + type +
            ", navn='" + navn + '\'' +
            ", kjønn=" + kjønn +
            ", fødselsdato=" + fødselsdato +
            ", dødsdato=" + dødsdato +
            "}";
    }
}
