package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysning.domene.PersonRepository
import org.springframework.stereotype.Service
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

    fun hentFagsakDeltagere(personIdent: String): List<FagsakDeltagerResponsDto> {
        val aktør = personidentService.hentAktør(personIdent)

        // hent maskert fagsak deltaker hvis aktør ikke har tilgang
        hentMaskertFagsakdeltakerVedManglendeTilgang(aktør)?.let { return listOf(it) }
        val personInfoMedRelasjoner = personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)

        // finner fagsak på aktør og henter assosierte fagsak deltagere
        val assosierteFagsakDeltagere = hentForelderdeltagereFraBehandling(aktør, personInfoMedRelasjoner).toMutableList()
        val erBarn = Period.between(personInfoMedRelasjoner.fødselsdato, LocalDate.now()).years < 18

        // fagsaker som ikke finnes i assosierteForeldreDeltagere, er barn
        // her bruker vi ifEmpty { listOf(null) } slik at fagsakerForBarn.forEach{} kjører minst en gang
        // assosierteFagsakDeltagere kan legges til hvis ingen fagsak finnes for søkparam
        val fagsakerForBarn = fagsakRepository.finnFagsakerForAktør(aktør).ifEmpty { listOf(null) }.filter { fagsak ->
            assosierteFagsakDeltagere.none { it.ident == aktør.aktivFødselsnummer() && it.fagsakId == fagsak?.id }
        }
        fagsakerForBarn.forEach { fagsak ->
            assosierteFagsakDeltagere.add(
                lagFagsakDeltagerResponsDto(
                    personInfo = personInfoMedRelasjoner,
                    ident = aktør.aktivFødselsnummer(),
                    // Vi setter rollen til Ukjent når det ikke er barn
                    rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    fagsak = fagsak
                )
            )
        }

        // Hvis søkparam(aktør) er barn og søker til barn ikke har behandling ennå, hentes det søker til barnet
        if (erBarn) {
            leggTilForeldreDeltagerSomIkkeHarBehandling(personInfoMedRelasjoner, assosierteFagsakDeltagere)
        }
        return assosierteFagsakDeltagere
    }

    fun hentPåFagsakId(fagsakId: Long): Fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw FunksjonellFeil(
        melding = "Finner ikke fagsak med id $fagsakId",
        frontendFeilmelding = "Finner ikke fagsak med id $fagsakId"
    )

    fun hentAktør(fagsakId: Long): Aktør = hentPåFagsakId(fagsakId).aktør

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
                        // her bruker vi ifEmpty { listOf(null) } slik at fagsakerForBarn.forEach{} kjører minst en gang
                        // assosierteFagsakDeltagere kan legges til hvis ingen fagsak finnes for søkparam
                        val fagsaker = fagsakRepository.finnFagsakerForAktør(relasjon.aktør).ifEmpty { listOf(null) }
                        fagsaker.forEach { fagsak ->
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

    private fun lagFagsakDeltagerResponsDto(
        personInfo: PdlPersonInfo? = null,
        ident: String = "",
        rolle: FagsakDeltagerRolle,
        fagsak: Fagsak? = null,
        adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        harTilgang: Boolean = true
    ): FagsakDeltagerResponsDto = FagsakDeltagerResponsDto(
        navn = personInfo?.navn,
        ident = ident,
        rolle = rolle,
        kjønn = personInfo?.kjønn,
        fagsakId = fagsak?.id,
        fagsakStatus = fagsak?.status,
        adressebeskyttelseGradering = adressebeskyttelseGradering,
        harTilgang = harTilgang
    )
}
