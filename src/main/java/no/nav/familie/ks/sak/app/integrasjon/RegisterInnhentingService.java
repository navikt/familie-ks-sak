package no.nav.familie.ks.sak.app.integrasjon;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.familie.kontrakter.ks.søknad.Søknad;
import no.nav.familie.ks.sak.app.behandling.domene.Behandling;
import no.nav.familie.ks.sak.app.behandling.domene.grunnlag.personopplysning.*;
import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.Landkode;
import no.nav.familie.ks.sak.app.behandling.domene.kodeverk.RelasjonsRolleType;
import no.nav.familie.ks.sak.app.behandling.domene.typer.AktørId;
import no.nav.familie.ks.sak.app.behandling.domene.typer.DatoIntervallEntitet;
import no.nav.familie.ks.sak.app.grunnlag.MedlFakta;
import no.nav.familie.ks.sak.app.grunnlag.PersonMedHistorikk;
import no.nav.familie.ks.sak.app.grunnlag.TpsFakta;
import no.nav.familie.ks.sak.app.integrasjon.infotrygd.domene.InfotrygdFakta;
import no.nav.familie.ks.sak.app.integrasjon.medlemskap.MedlemskapsInfo;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.FDATException;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.PersonIdent;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.PersonhistorikkInfo;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.Personinfo;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.adresse.Adresseinfo;
import no.nav.familie.ks.sak.app.integrasjon.personopplysning.domene.relasjon.Familierelasjon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RegisterInnhentingService {

    private static final Logger secureLogger = LoggerFactory.getLogger("secureLogger");
    private static final Logger logger = LoggerFactory.getLogger(RegisterInnhentingService.class);
    private PersonopplysningService personopplysningService;
    private IntegrasjonTjeneste integrasjonTjeneste;
    private Counter oppgittAnnenPartStemmer = Metrics.counter("soknad.kontantstotte.funksjonell.oppgittannenpart", "erliktsomitps", "JA", "beskrivelse", "Oppgitt annen part stemmer");
    private Counter oppgittAnnenPartStemmerIkke = Metrics.counter("soknad.kontantstotte.funksjonell.oppgittannenpart", "erliktsomitps", "NEI", "beskrivelse", "Oppgitt annen part stemmer ikke");
    private Counter oppgittAnnenPartStemmerDelvis = Metrics.counter("soknad.kontantstotte.funksjonell.oppgittannenpart", "erliktsomitps", "DELVIS", "beskrivelse", "Første 6 tall av fnr er likt");
    private Counter oppgittAnnenPartIkkeOppgitt = Metrics.counter("soknad.kontantstotte.funksjonell.oppgittannenpart", "erliktsomitps", "IKKE_OPPGITT", "beskrivelse", "Annen part er ikke oppgitt");
    private Counter oppgittAnnenPartIkkeFunnetITps = Metrics.counter("soknad.kontantstotte.funksjonell.oppgittannenpart", "erliktsomitps", "IKKE_FUNNET", "beskrivelse", "Annen part er ikke funnet i TPS");

    @Autowired
    public RegisterInnhentingService(PersonopplysningService personopplysningService, IntegrasjonTjeneste integrasjonTjeneste) {
        this.personopplysningService = personopplysningService;
        this.integrasjonTjeneste = integrasjonTjeneste;
    }

    public TpsFakta innhentPersonopplysninger(Behandling behandling, Søknad søknad) throws FDATException {
        final var oppgittAnnenPartPersonIdent = søknad.getOppgittAnnenPartFødselsnummer();
        // TODO skriv om når vi støtter flerlinger
        final var barnFødselsnummer = søknad.getOppgittFamilieforhold().getBarna().iterator().next().getFødselsnummer();
        final var personopplysningGrunnlag = new PersonopplysningGrunnlag(behandling.getId());

        final PersonMedHistorikk søkerPersonMedHistorikk = hentPersonMedHistorikk(søknad.getSøkerFødselsnummer());
        final PersonMedHistorikk barnPersonMedHistorikk = hentPersonMedHistorikk(barnFødselsnummer);
        if (barnHarRelasjonerMedFDAT(barnPersonMedHistorikk)) {
            throw new FDATException();
        }

        mapPersonopplysninger(søkerPersonMedHistorikk, personopplysningGrunnlag, PersonType.SØKER);
        //TODO legg til støtte for flere barn!
        mapPersonopplysninger(barnPersonMedHistorikk, personopplysningGrunnlag, PersonType.BARN);

        PersonMedHistorikk annenPartPersonMedHistorikk = null;
        final Optional<Familierelasjon> annenPartFamilierelasjon = barnPersonMedHistorikk.getPersoninfo().getFamilierelasjoner().stream().filter(
            familierelasjon ->
                (familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.FARA) || familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.MORA))
                    && !familierelasjon.getPersonIdent().getIdent().equals(søknad.getSøkerFødselsnummer()))
            .findFirst();

        PersonIdent annenPartPersonIdent;
        if (annenPartFamilierelasjon.isPresent()) {
            annenPartPersonIdent = annenPartFamilierelasjon.get().getPersonIdent();
            annenPartPersonMedHistorikk = hentPersonMedHistorikk(annenPartPersonIdent.getIdent());

            mapPersonopplysninger(annenPartPersonMedHistorikk, personopplysningGrunnlag, PersonType.ANNENPART);

            //TODO legg til støtte for flere barn
            mapRelasjoner(søkerPersonMedHistorikk.getPersoninfo(), annenPartPersonMedHistorikk.getPersoninfo(), barnPersonMedHistorikk.getPersoninfo(), personopplysningGrunnlag);

            if (oppgittAnnenPartPersonIdent != null && !oppgittAnnenPartPersonIdent.isEmpty()) {
                if (annenPartPersonIdent.getIdent().regionMatches(0, oppgittAnnenPartPersonIdent, 0, 6)) {
                    if (!annenPartPersonIdent.getIdent().equals(oppgittAnnenPartPersonIdent)) {
                        oppgittAnnenPartStemmerDelvis.increment();
                    } else {
                        oppgittAnnenPartStemmer.increment();
                    }
                } else {
                    secureLogger.info("Fant annen part: {}. Oppgitt annen part fra søker: {}", annenPartPersonIdent, oppgittAnnenPartPersonIdent);
                    oppgittAnnenPartStemmerIkke.increment();
                    logger.info("Oppgitt annen part fra søker stemmer ikke med relasjonen vi fant på barnet fra TPS");
                }
            }
        } else {
            secureLogger.info("Fant ikke annen part i listen over relasjoner til barnet: {}", barnPersonMedHistorikk.getPersoninfo().getFamilierelasjoner());
            oppgittAnnenPartIkkeFunnetITps.increment();
            mapRelasjoner(søkerPersonMedHistorikk.getPersoninfo(), null, barnPersonMedHistorikk.getPersoninfo(), personopplysningGrunnlag);
        }

        if (oppgittAnnenPartPersonIdent == null || oppgittAnnenPartPersonIdent.isEmpty()) {
            oppgittAnnenPartIkkeOppgitt.increment();
        }

        personopplysningService.lagre(behandling, personopplysningGrunnlag);
        return new TpsFakta.Builder()
            .medForelder(søkerPersonMedHistorikk)
            .medBarn(List.of(barnPersonMedHistorikk))
            .medAnnenForelder(annenPartPersonMedHistorikk)
            .build();
    }

    public InfotrygdFakta hentInfotrygdFakta(Søknad søknad) {
        // TODO: Legg til støtte for flerlinger.
        return new InfotrygdFakta(integrasjonTjeneste.hentInfoOmLøpendeKontantstøtteForBarn(søknad.getOppgittFamilieforhold().getBarna().iterator().next().getFødselsnummer()));
    }

    public MedlFakta hentMedlemskapsopplysninger(Behandling behandling) {
        Optional<PersonopplysningGrunnlag> pers = personopplysningService.hentHvisEksisterer(behandling);
        if (pers.isEmpty()) {
            throw new IllegalStateException("Vi må ha personopplysninger for å kunne hente inn medlInfo.");
        }
        PersonopplysningGrunnlag personopplysningGrunnlag = pers.get();
        final var søkerAktørId = personopplysningGrunnlag.getSøker().getAktørId();
        final MedlemskapsInfo søkerMedlemskapsInfo = integrasjonTjeneste.hentMedlemskapsUnntakFor(søkerAktørId);

        return new MedlFakta.Builder()
            .medSøker(erTom(søkerMedlemskapsInfo.getPersonIdent()) ? Optional.empty() : Optional.of(søkerMedlemskapsInfo))
            .medAnnenForelder(hentAnnenPartMedl(personopplysningGrunnlag))
            .build();
    }

    private boolean barnHarRelasjonerMedFDAT(PersonMedHistorikk barn) {
        long antallFdatRelasjoner = barn.getPersoninfo().getFamilierelasjoner().stream()
            .filter(familierelasjon ->
                (familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.FARA) || familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.MORA))
                    && familierelasjon.getPersonIdent().erFdatNummer())
            .count();

        return antallFdatRelasjoner > 0;
    }

    private Optional<MedlemskapsInfo> hentAnnenPartMedl(PersonopplysningGrunnlag personopplysningGrunnlag) {
        if (personopplysningGrunnlag.getAnnenPart() == null) {
            return Optional.empty();
        }
        MedlemskapsInfo annenForelder = integrasjonTjeneste.hentMedlemskapsUnntakFor(personopplysningGrunnlag.getAnnenPart().getAktørId());
        return erTom(annenForelder.getPersonIdent()) ? Optional.empty() : Optional.of(annenForelder);
    }

    private boolean erTom(String personIdent) {
        return personIdent == null || personIdent.isEmpty();
    }

    private PersonMedHistorikk hentPersonMedHistorikk(String personIdent) {
        final AktørId aktørId = integrasjonTjeneste.hentAktørId(personIdent);
        final Personinfo personinfo = integrasjonTjeneste.hentPersoninfoFor(personIdent).medAktørId(aktørId);
        final PersonhistorikkInfo personhistorikkInfo = integrasjonTjeneste.hentHistorikkFor(personIdent, personinfo.getFødselsdato());
        return new PersonMedHistorikk.Builder()
            .medInfo(personinfo)
            .medPersonhistorikk(personhistorikkInfo)
            .build();
    }

    private void mapRelasjoner(Personinfo søker, Personinfo annenPart, Personinfo barn, PersonopplysningGrunnlag personopplysningGrunnlag) {
        barn.getFamilierelasjoner()
            .stream()
            .filter(it -> it.getPersonIdent().equals(søker.getPersonIdent()))
            .forEach(relasjon -> personopplysningGrunnlag.getBarn(barn.getAktørId()).leggTilPersonrelasjon(new PersonRelasjon(barn.getAktørId(), søker.getAktørId(), barn.getPersonIdent(), relasjon.getPersonIdent(), relasjon.getRelasjonsrolle(), relasjon.getHarSammeBosted())));

        barn.getFamilierelasjoner()
            .stream()
            .filter(it -> annenPart != null && it.getPersonIdent().equals(annenPart.getPersonIdent()))
            .forEach(relasjon -> personopplysningGrunnlag.getBarn(barn.getAktørId()).leggTilPersonrelasjon(new PersonRelasjon(barn.getAktørId(), annenPart.getAktørId(), barn.getPersonIdent(), relasjon.getPersonIdent(), relasjon.getRelasjonsrolle(), relasjon.getHarSammeBosted())));

        søker.getFamilierelasjoner()
            .stream()
            .filter(it -> it.getPersonIdent().equals(barn.getPersonIdent()))
            .forEach(relasjon ->  personopplysningGrunnlag.getSøker().leggTilPersonrelasjon(new PersonRelasjon(søker.getAktørId(), barn.getAktørId(), søker.getPersonIdent(), relasjon.getPersonIdent(), relasjon.getRelasjonsrolle(), relasjon.getHarSammeBosted())));

        søker.getFamilierelasjoner()
            .stream()
            .filter(it -> annenPart != null && it.getPersonIdent().equals(annenPart.getPersonIdent()))
            .forEach(relasjon ->  personopplysningGrunnlag.getSøker().leggTilPersonrelasjon(new PersonRelasjon(søker.getAktørId(), annenPart.getAktørId(), søker.getPersonIdent(), relasjon.getPersonIdent(), relasjon.getRelasjonsrolle(), relasjon.getHarSammeBosted())));

        if (annenPart != null) {
            annenPart.getFamilierelasjoner()
                .stream()
                .filter(it -> it.getPersonIdent().equals(barn.getPersonIdent()))
                .forEach(relasjon -> personopplysningGrunnlag.getAnnenPart().leggTilPersonrelasjon(new PersonRelasjon(annenPart.getAktørId(), barn.getAktørId(), annenPart.getPersonIdent(), relasjon.getPersonIdent(), relasjon.getRelasjonsrolle(), relasjon.getHarSammeBosted())));

            annenPart.getFamilierelasjoner()
                .stream()
                .filter(it -> it.getPersonIdent().equals(søker.getPersonIdent()))
                .forEach(relasjon -> personopplysningGrunnlag.getAnnenPart().leggTilPersonrelasjon(new PersonRelasjon(annenPart.getAktørId(), søker.getAktørId(), annenPart.getPersonIdent(), relasjon.getPersonIdent(), relasjon.getRelasjonsrolle(), relasjon.getHarSammeBosted())));
        }
    }

    private void mapPersonopplysninger(PersonMedHistorikk personMedHistorikk, PersonopplysningGrunnlag personopplysningGrunnlag, PersonType personType) {
        var personinfo = personMedHistorikk.getPersoninfo();
        var aktørId = personinfo.getAktørId();
        var personhistorikk = personMedHistorikk.getPersonhistorikkInfo();

        Person person = new Person(aktørId, personinfo.getPersonIdent(), personType)
            .medFødselsdato(personinfo.getFødselsdato())
            .medKjønn(personinfo.getKjønn())
            .medDødsdato(personinfo.getDødsdato())
            .medNavn(personinfo.getNavn())
            .medBostedsadresse(leggTilBostedsadresse(aktørId, personinfo.getBostedsadresse()))
            .medStatsborgerskap(new Landkode(personinfo.getStatsborgerskap().getKode()));
        personopplysningGrunnlag.leggTilPerson(person);

        if (personhistorikk != null) {
            personhistorikk.getStatsborgerskaphistorikk()
                .forEach(statsborgerskap -> person.leggTilStatsborgerskap(new Statsborgerskap(aktørId,
                    DatoIntervallEntitet.fraOgMedTilOgMed(statsborgerskap.getPeriode().getFom(), statsborgerskap.getPeriode().getTom()),
                    new Landkode(statsborgerskap.getTilhørendeLand().getKode()))));

            personhistorikk.getAdressehistorikk()
                .forEach(adresse -> person.leggTilAdresse(PersonAdresse.opprettNy(aktørId, adresse)));
        }
    }
    private Bostedsadresse leggTilBostedsadresse(AktørId aktørId, Adresseinfo adresseinfo) {
        if (adresseinfo == null) {
            return null;
        }
        return Bostedsadresse.opprettNy(aktørId, adresseinfo);
    }
}
