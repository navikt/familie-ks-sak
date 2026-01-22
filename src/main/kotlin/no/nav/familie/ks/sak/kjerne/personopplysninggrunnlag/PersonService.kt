package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.ks.sak.common.util.storForbokstav
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.filtrerUtKunNorskeBostedsadresser
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.opphold.GrOpphold
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val personOpplysningerService: PersonopplysningerService,
    private val statsborgerskapService: StatsborgerskapService,
    private val integrasjonKlient: IntegrasjonKlient,
) {
    private fun Bostedsadresse.poststed(): String? = poststed(vegadresse?.postnummer ?: matrikkeladresse?.postnummer)

    private fun Oppholdsadresse.poststed(): String? = poststed(vegadresse?.postnummer ?: matrikkeladresse?.postnummer)

    private fun poststed(postnummer: String?): String? =
        integrasjonKlient
            .hentPoststeder()
            .betydninger[postnummer]
            ?.firstOrNull()
            ?.beskrivelser[KodeverkSpråk.BOKMÅL.kode]
            ?.term
            ?.storForbokstav()

    fun lagPerson(
        aktør: Aktør,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        målform: Målform,
        personType: PersonType,
        krevesEnkelPersonInfo: Boolean,
    ): Person {
        val personinfo =
            if (krevesEnkelPersonInfo) {
                personOpplysningerService.hentPersoninfoEnkel(aktør)
            } else {
                personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)
            }
        return Person(
            type = personType,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = personinfo.fødselsdato,
            aktør = aktør,
            navn = personinfo.navn ?: "",
            kjønn = personinfo.kjønn?.let { Kjønn.valueOf(it.name) } ?: Kjønn.UKJENT,
            målform = målform,
        ).also { person ->
            person.opphold = personinfo.opphold?.map { GrOpphold.fraOpphold(it, person) }?.toMutableList()
                ?: mutableListOf()
            person.bostedsadresser =
                personinfo.bostedsadresser
                    .filtrerUtKunNorskeBostedsadresser()
                    .map {
                        GrBostedsadresse.fraBostedsadresse(
                            bostedsadresse = it,
                            person = person,
                            poststed = it.poststed(),
                        )
                    }.toMutableList()
            person.oppholdsadresser =
                personinfo.oppholdsadresser
                    .map {
                        GrOppholdsadresse.fraOppholdsadresse(
                            oppholdsadresse = it,
                            person = person,
                            poststed = it.poststed(),
                        )
                    }.toMutableList()
            person.sivilstander = personinfo.sivilstander.map { GrSivilstand.fraSivilstand(it, person) }.toMutableList()
            person.dødsfall =
                Dødsfall.lagDødsfall(
                    person = person,
                    pdlDødsfallDato = personinfo.dødsfall?.dødsdato,
                    pdlDødsfallAdresse = personinfo.kontaktinformasjonForDoedsbo?.adresse,
                )
            person.statsborgerskap = personinfo.statsborgerskap
                ?.flatMap {
                    statsborgerskapService.hentStatsborgerskapMedMedlemskap(it, person)
                }?.sortedBy { it.gyldigPeriode?.fom }
                ?.toMutableList() ?: mutableListOf()
        }
    }
}
