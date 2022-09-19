package no.nav.familie.ks.sak.kjerne.fagsak

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.api.dto.FagsakMapper.lagBehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakMapper.lagFagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakMapper.lagMinimalFagsakResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.api.dto.MinimalFagsakResponsDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period

@Service
class FagsakService(
    private val personidentService: PersonidentService,
    private val integrasjonClient: IntegrasjonClient,
    private val personopplysningerService: PersonOpplysningerService,
    private val fagsakRepository: FagsakRepository,
    private val personRepository: PersonRepository,
    private val behandlingRepository: BehandlingRepository
) {

    private val antallFagsakerOpprettetFraManuell =
        Metrics.counter("familie.ks.sak.fagsak.opprettet", "saksbehandling", "manuell")

    fun hentFagsakDeltagere(personIdent: String): List<FagsakDeltagerResponsDto> {
        val aktør = personidentService.hentAktør(personIdent)

        // returnerer maskert fagsak deltaker hvis aktør ikke har tilgang
        hentMaskertFagsakdeltakerVedManglendeTilgang(aktør)?.let { return listOf(it) }

        val personInfoMedRelasjoner = personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)

        // finner fagsak på aktør og henter assosierte fagsak deltagere
        val assosierteFagsakDeltagere = hentForelderdeltagereFraBehandling(aktør, personInfoMedRelasjoner).toMutableList()

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
                    fagsak = fagsakForBarn
                )
            )
        }

        // Hvis søkparam(aktør) er barn og søker til barn ikke har behandling ennå, hentes det søker til barnet
        if (erBarn) {
            leggTilForeldreDeltagerSomIkkeHarBehandling(personInfoMedRelasjoner, assosierteFagsakDeltagere)
        }
        return assosierteFagsakDeltagere
    }

    @Transactional
    fun hentEllerOpprettFagsak(fagsakRequest: FagsakRequestDto): MinimalFagsakResponsDto {
        val personident = fagsakRequest.personIdent ?: fagsakRequest.aktørId ?: throw Feil(
            "Hverken aktørid eller personident er satt på fagsak-requesten. Klarer ikke opprette eller hente fagsak.",
            "Fagsak er forsøkt opprettet uten ident. Dette er en systemfeil, vennligst ta kontakt med systemansvarlig.",
            HttpStatus.BAD_REQUEST
        )

        val aktør = personidentService.hentOgLagreAktør(personident, true)
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør) ?: lagre(Fagsak(aktør = aktør))
        antallFagsakerOpprettetFraManuell.increment()
        return lagMinimalFagsakResponsDto(fagsak, behandlingRepository.findByFagsakAndAktiv(fagsak.id))
    }

    fun hentMinimalFagsak(fagsakId: Long): MinimalFagsakResponsDto {
        val fagsak = fagsakRepository.findByIdOrNull(fagsakId) ?: throw Feil("Fagsak $fagsakId finnes ikke")
        val alleBehandlinger = behandlingRepository.finnBehandlinger(fagsakId).map { lagBehandlingResponsDto(it) }
        return lagMinimalFagsakResponsDto(
            fagsak = fagsak,
            aktivtBehandling = behandlingRepository.findByFagsakAndAktiv(fagsakId),
            behandlinger = alleBehandlinger
        )
    }

    @Transactional
    fun lagre(fagsak: Fagsak): Fagsak {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak)
    }

    private fun hentForelderdeltagereFraBehandling(
        aktør: Aktør,
        personInfoMedRelasjoner: PdlPersonInfo
    ): List<FagsakDeltagerResponsDto> {
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, FagsakDeltagerResponsDto>()
        personRepository.findByAktør(aktør).filter { it.personopplysningGrunnlag.aktiv }.forEach { person ->
            val behandling = behandlingRepository.finnAktivBehandling(person.personopplysningGrunnlag.behandlingId)
            val fagsak = behandling.fagsak // Behandling opprettet alltid med søker aktør
            if (assosierteFagsakDeltagerMap.containsKey(fagsak.id)) return@forEach
            val fagsakDeltagerRespons: FagsakDeltagerResponsDto = when {
                // når søkparam er samme som aktør til behandlingen
                fagsak.aktør == aktør -> lagFagsakDeltagerResponsDto(
                    personInfo = personInfoMedRelasjoner,
                    ident = fagsak.aktør.aktivFødselsnummer(),
                    rolle = FagsakDeltagerRolle.FORELDER,
                    fagsak = behandling.fagsak
                )
                else -> { // søkparam(aktør) er ikke søkers aktør, da hentes her forelder til søkparam(aktør)
                    val maskertForelder = hentMaskertFagsakdeltakerVedManglendeTilgang(fagsak.aktør)
                    maskertForelder?.copy(rolle = FagsakDeltagerRolle.FORELDER)
                        ?: lagFagsakDeltagerResponsDto(
                            personopplysningerService.hentPersoninfoEnkel(fagsak.aktør),
                            fagsak.aktør.aktivFødselsnummer(),
                            FagsakDeltagerRolle.FORELDER,
                            fagsak
                        )
                }
            }
            assosierteFagsakDeltagerMap[fagsak.id] = fagsakDeltagerRespons
        }
        return assosierteFagsakDeltagerMap.values.toList()
    }

    private fun leggTilForeldreDeltagerSomIkkeHarBehandling(
        personInfoMedRelasjoner: PdlPersonInfo,
        assosierteFagsakDeltagere: MutableList<FagsakDeltagerResponsDto>
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
                                fagsak = fagsak
                            )
                        )
                    }
                }
            }
        }
    }

    private fun hentMaskertFagsakdeltakerVedManglendeTilgang(aktør: Aktør): FagsakDeltagerResponsDto? {
        val harTilgang = integrasjonClient.sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).harTilgang

        return when {
            !harTilgang -> {
                val adressebeskyttelse = personopplysningerService.hentAdressebeskyttelseSomSystembruker(aktør)

                lagFagsakDeltagerResponsDto(
                    rolle = FagsakDeltagerRolle.UKJENT,
                    adressebeskyttelseGradering = adressebeskyttelse,
                    harTilgang = false
                )
            }
            else -> {
                null
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
