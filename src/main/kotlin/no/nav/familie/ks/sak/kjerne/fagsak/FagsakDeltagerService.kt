package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.api.mapper.FagsakMapper.lagFagsakDeltagerResponsDto
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class FagsakDeltagerService(
    private val personidentService: PersonidentService,
    private val integrasjonService: IntegrasjonService,
    private val personopplysningerService: PersonopplysningerService,
    private val fagsakRepository: FagsakRepository,
    private val personRepository: PersonRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun hentFagsakDeltagere(personIdent: String): List<FagsakDeltagerResponsDto> {
        val aktør = personidentService.hentAktør(personIdent)

        // returnerer maskert fagsak deltaker hvis saksbehandler ikke har tilgang til aktøren
        hentMaskertFagsakdeltakerVedManglendeTilgang(aktør)?.let { return listOf(it) }

        val personInfoMedRelasjoner = personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)

        // finner fagsak på aktør og henter assosierte fagsak deltagere
        val assosierteFagsakDeltagere =
            hentForelderdeltagereFraBehandling(aktør, personInfoMedRelasjoner).toMutableList()

        val erBarn = Period.between(personInfoMedRelasjoner.fødselsdato, LocalDate.now()).years < 18

        // fagsaker som ikke finnes i assosierteForeldreDeltagere, er barn
        val fagsakForBarn = fagsakRepository.finnFagsakForAktør(aktør)

        if (assosierteFagsakDeltagere.none { it.ident == aktør.aktivFødselsnummer() && it.fagsakId == fagsakForBarn?.id }) {
            assosierteFagsakDeltagere.add(
                lagFagsakDeltagerResponsDto(
                    personInfo = personInfoMedRelasjoner,
                    ident = aktør.aktivFødselsnummer(),
                    // Vi setter rollen til Ukjent når det ikke er barn
                    rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    fagsak = fagsakForBarn,
                    adressebeskyttelseGradering = personInfoMedRelasjoner.adressebeskyttelseGradering,
                ),
            )
        }

        // Hvis søkparam(aktør) er barn og søker til barn ikke har behandling ennå, hentes det søker til barnet
        if (erBarn) {
            leggTilForeldreDeltagerSomIkkeHarBehandling(personInfoMedRelasjoner, assosierteFagsakDeltagere)
        }
        val fagsakDeltagereMedEgenAnsattStatus = settEgenAnsattStatusPåFagsakDeltagere(assosierteFagsakDeltagere)

        return fagsakDeltagereMedEgenAnsattStatus
    }

    private fun hentForelderdeltagereFraBehandling(
        aktør: Aktør,
        personInfoMedRelasjoner: PdlPersonInfo,
    ): List<FagsakDeltagerResponsDto> {
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, FagsakDeltagerResponsDto>()
        personRepository.findByAktør(aktør).filter { it.personopplysningGrunnlag.aktiv }.forEach { person ->
            val behandling = behandlingRepository.hentBehandling(person.personopplysningGrunnlag.behandlingId)
            val fagsak = behandling.fagsak // Behandling opprettet alltid med søker aktør
            if (assosierteFagsakDeltagerMap.containsKey(fagsak.id)) return@forEach
            val fagsakDeltagerRespons: FagsakDeltagerResponsDto =
                when {
                    // når søkparam er samme som aktør til behandlingen
                    fagsak.aktør == aktør ->
                        lagFagsakDeltagerResponsDto(
                            personInfo = personInfoMedRelasjoner,
                            ident = fagsak.aktør.aktivFødselsnummer(),
                            rolle = FagsakDeltagerRolle.FORELDER,
                            fagsak = behandling.fagsak,
                            adressebeskyttelseGradering = personInfoMedRelasjoner.adressebeskyttelseGradering,
                        )

                    else -> { // søkparam(aktør) er ikke søkers aktør, da hentes her forelder til søkparam(aktør)
                        val maskertForelder = hentMaskertFagsakdeltakerVedManglendeTilgang(fagsak.aktør)
                        maskertForelder?.copy(rolle = FagsakDeltagerRolle.FORELDER)
                            ?: run {
                                val personInfo = personopplysningerService.hentPersoninfoEnkel(fagsak.aktør)
                                lagFagsakDeltagerResponsDto(
                                    personInfo = personInfo,
                                    ident = fagsak.aktør.aktivFødselsnummer(),
                                    rolle = FagsakDeltagerRolle.FORELDER,
                                    adressebeskyttelseGradering = personInfo.adressebeskyttelseGradering,
                                    fagsak = fagsak,
                                )
                            }
                    }
                }
            assosierteFagsakDeltagerMap[fagsak.id] = fagsakDeltagerRespons
        }
        return assosierteFagsakDeltagerMap.values.toList()
    }

    private fun leggTilForeldreDeltagerSomIkkeHarBehandling(
        personInfoMedRelasjoner: PdlPersonInfo,
        assosierteFagsakDeltagere: MutableList<FagsakDeltagerResponsDto>,
    ) {
        personInfoMedRelasjoner.forelderBarnRelasjoner.filter { it.harForelderRelasjon() }.forEach { relasjon ->
            if (assosierteFagsakDeltagere.none { it.ident == relasjon.aktør.aktivFødselsnummer() }) {
                val maskertForelder = hentMaskertFagsakdeltakerVedManglendeTilgang(relasjon.aktør)
                when {
                    maskertForelder != null -> assosierteFagsakDeltagere.add(maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER))
                    else -> {
                        val forelderInfo = personopplysningerService.hentPersoninfoEnkel(relasjon.aktør)
                        val fagsak = fagsakRepository.finnFagsakForAktør(relasjon.aktør)
                        assosierteFagsakDeltagere.add(
                            lagFagsakDeltagerResponsDto(
                                personInfo = forelderInfo,
                                ident = relasjon.aktør.aktivFødselsnummer(),
                                rolle = FagsakDeltagerRolle.FORELDER,
                                fagsak = fagsak,
                                adressebeskyttelseGradering = forelderInfo.adressebeskyttelseGradering,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun settEgenAnsattStatusPåFagsakDeltagere(fagsakDeltagere: List<FagsakDeltagerResponsDto>): List<FagsakDeltagerResponsDto> {
        val egenAnsattPerIdent = integrasjonService.sjekkErEgenAnsattBulk(fagsakDeltagere.map { it.ident })
        return fagsakDeltagere.map { fagsakDeltager ->
            fagsakDeltager.copy(
                erEgenAnsatt = egenAnsattPerIdent.getOrDefault(fagsakDeltager.ident, null),
            )
        }
    }

    private fun hentMaskertFagsakdeltakerVedManglendeTilgang(aktør: Aktør): FagsakDeltagerResponsDto? {
        val harTilgang = integrasjonService.sjekkTilgangTilPerson(aktør.aktivFødselsnummer()).harTilgang

        return when {
            !harTilgang -> {
                val adressebeskyttelse = personopplysningerService.hentAdressebeskyttelseSomSystembruker(aktør)

                lagFagsakDeltagerResponsDto(
                    rolle = FagsakDeltagerRolle.UKJENT,
                    adressebeskyttelseGradering = adressebeskyttelse,
                    harTilgang = false,
                )
            }

            else -> {
                null
            }
        }
    }
}
