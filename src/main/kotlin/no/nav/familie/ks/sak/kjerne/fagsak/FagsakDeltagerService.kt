package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.api.mapper.FagsakMapper.lagFagsakDeltagerResponsDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PersonInfoBase
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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

        val pdlPersonInfoMedRelasjoner = personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(aktør).personInfoBase()

        // finner fagsak på aktør og henter assosierte fagsak deltagere
        val assosierteFagsakDeltagere =
            hentForelderdeltagereFraBehandling(aktør, pdlPersonInfoMedRelasjoner).toMutableList()

        val erBarn = pdlPersonInfoMedRelasjoner.erBarn()

        // fagsaker som ikke finnes i assosierteForeldreDeltagere, er barn
        val fagsakForBarn = fagsakRepository.finnFagsakForAktør(aktør)

        if (assosierteFagsakDeltagere.none { it.ident == aktør.aktivFødselsnummer() && it.fagsakId == fagsakForBarn?.id }) {
            assosierteFagsakDeltagere.add(
                lagFagsakDeltagerResponsDto(
                    personInfo = pdlPersonInfoMedRelasjoner,
                    ident = aktør.aktivFødselsnummer(),
                    // Vi setter rollen til Ukjent når det ikke er barn
                    rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    fagsak = fagsakForBarn,
                    adressebeskyttelseGradering = pdlPersonInfoMedRelasjoner.adressebeskyttelseGradering,
                ),
            )
        }

        // Hvis søkparam(aktør) er barn og søker til barn ikke har behandling ennå, hentes det søker til barnet
        if (erBarn) {
            leggTilForeldreDeltagerSomIkkeHarBehandling(pdlPersonInfoMedRelasjoner, assosierteFagsakDeltagere)
        }
        val fagsakDeltagereMedEgenAnsattStatus = settEgenAnsattStatusPåFagsakDeltagere(assosierteFagsakDeltagere)

        return fagsakDeltagereMedEgenAnsattStatus
    }

    private fun hentForelderdeltagereFraBehandling(
        aktør: Aktør,
        personInfoMedRelasjoner: PersonInfoBase,
    ): Collection<FagsakDeltagerResponsDto> =
        personRepository
            .findByAktør(aktør)
            .filter { it.personopplysningGrunnlag.aktiv }
            .fold(mutableMapOf<Long, FagsakDeltagerResponsDto>()) { fagsakDeltagerMap, person ->
                val behandling = behandlingRepository.hentBehandling(behandlingId = person.personopplysningGrunnlag.behandlingId)
                if (!behandling.aktiv || behandling.fagsak.arkivert || fagsakDeltagerMap.containsKey(behandling.fagsak.id)) {
                    return@fold fagsakDeltagerMap
                }

                val fagsakEier =
                    hentFagsakEier(
                        fagsak = behandling.fagsak,
                        aktør = aktør,
                        personInfoMedRelasjoner = personInfoMedRelasjoner,
                    )
                if (fagsakEier != null) {
                    fagsakDeltagerMap[behandling.fagsak.id] = fagsakEier
                }

                fagsakDeltagerMap
            }.values

    private fun hentFagsakEier(
        fagsak: Fagsak,
        aktør: Aktør,
        personInfoMedRelasjoner: PersonInfoBase,
    ): FagsakDeltagerResponsDto? {
        if (fagsak.aktør == aktør) {
            return lagFagsakDeltagerResponsDto(
                personInfo = personInfoMedRelasjoner,
                ident = fagsak.aktør.aktivFødselsnummer(),
                rolle = FagsakDeltagerRolle.FORELDER,
                fagsak = fagsak,
            )
        }
        val maskertPerson = hentMaskertFagsakdeltakerVedManglendeTilgang(fagsak.aktør)
        if (maskertPerson != null) {
            return maskertPerson.copy(rolle = FagsakDeltagerRolle.FORELDER)
        }

        val forelderInfo = personInfoMedRelasjoner.forelderBarnRelasjoner.find { it.aktør.aktivFødselsnummer() == fagsak.aktør.aktivFødselsnummer() }
        if (forelderInfo != null) {
            return lagFagsakDeltagerResponsDto(
                personInfo = forelderInfo,
                ident = fagsak.aktør.aktivFødselsnummer(),
                rolle = FagsakDeltagerRolle.FORELDER,
                fagsak = fagsak,
            )
        }

        // Person med forelderrolle uten direkte relasjon
        return hentPersonMedForeldrerolle(fagsak)
    }

    private fun hentPersonMedForeldrerolle(fagsak: Fagsak): FagsakDeltagerResponsDto? =
        runCatching {
            personopplysningerService.hentPdlPersonInfoEnkel(fagsak.aktør).personInfoBase()
        }.fold(
            onSuccess = {
                lagFagsakDeltagerResponsDto(
                    personInfo = it,
                    ident = fagsak.aktør.aktivFødselsnummer(),
                    rolle = FagsakDeltagerRolle.FORELDER,
                    fagsak = fagsak,
                    adressebeskyttelseGradering = it.adressebeskyttelseGradering,
                )
            },
            onFailure = { exception ->
                when (exception) {
                    is PdlPersonKanIkkeBehandlesIFagsystem -> {
                        // Filtrerer bort personer som ikke kan behandles i fagsystem og som ikke har falsk ident
                        logger.warn("Filtrerer bort eier av en fagsak som ikke kan behandles i fagsystem pga ${exception.årsak}")
                        null
                    }

                    is FunksjonellFeil -> {
                        throw exception
                    }

                    else -> {
                        throw Feil("Feil ved henting av person fra PDL", throwable = exception)
                    }
                }
            },
        )

    private fun leggTilForeldreDeltagerSomIkkeHarBehandling(
        personInfoMedRelasjoner: PersonInfoBase,
        assosierteFagsakDeltagere: MutableList<FagsakDeltagerResponsDto>,
    ) {
        personInfoMedRelasjoner.forelderBarnRelasjoner.filter { it.harForelderRelasjon() }.forEach { relasjon ->
            if (assosierteFagsakDeltagere.none { it.ident == relasjon.aktør.aktivFødselsnummer() }) {
                val maskertForelder = hentMaskertFagsakdeltakerVedManglendeTilgang(relasjon.aktør)
                when {
                    maskertForelder != null -> {
                        assosierteFagsakDeltagere.add(maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER))
                    }

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
        val egenAnsattPerIdent = integrasjonService.sjekkErEgenAnsattBulk(fagsakDeltagere.map { it.ident }.toSet())
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

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakDeltagerService::class.java)
    }
}
