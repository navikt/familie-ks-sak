package no.nav.familie.ks.sak.kjerne.fagsak

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.api.dto.MinimalFagsakResponsDto
import no.nav.familie.ks.sak.api.dto.tilUtbetalingsperiodeResponsDto
import no.nav.familie.ks.sak.api.mapper.FagsakMapper.lagBehandlingResponsDto
import no.nav.familie.ks.sak.api.mapper.FagsakMapper.lagFagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.mapper.FagsakMapper.lagMinimalFagsakResponsDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
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
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val fagsakRepository: FagsakRepository,
    private val personRepository: PersonRepository,
    private val behandlingRepository: BehandlingRepository,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val taskService: TaskService,
    private val vedtakRepository: VedtakRepository
) {

    private val antallFagsakerOpprettetFraManuell =
        Metrics.counter("familie.ks.sak.fagsak.opprettet", "saksbehandling", "manuell")

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
        val fagsak = hentFagsak(fagsakId)
        val alleBehandlinger = behandlingRepository.finnBehandlinger(fagsakId)

        val sistIverksatteBehandling = alleBehandlinger.filter { it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }
            .maxByOrNull { it.opprettetTidspunkt }

        val gjeldendeUtbetalingsperioder = sistIverksatteBehandling?.let {
            val personopplysningGrunnlag =
                personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(it.id)
            val andeler =
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(it.id)

            andeler.tilUtbetalingsperiodeResponsDto(personopplysningGrunnlag)
        }

        return lagMinimalFagsakResponsDto(
            fagsak = fagsak,
            aktivtBehandling = behandlingRepository.findByFagsakAndAktiv(fagsakId),
            behandlinger = alleBehandlinger.map {
                lagBehandlingResponsDto(
                    behandling = it,
                    vedtaksdato = vedtakRepository.findByBehandlingAndAktivOptional(it.id)?.vedtaksdato
                )
            },
            gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder ?: emptyList()
        )
    }

    fun finnMinimalFagsakForPerson(personIdent: String): MinimalFagsakResponsDto? {
        val aktør = personidentService.hentAktør(personIdent)
        val fagsak = finnFagsakForPerson(aktør)
        return fagsak?.let { lagMinimalFagsakResponsDto(it) }
    }

    @Transactional
    fun lagre(fagsak: Fagsak): Fagsak {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak).also {
            taskService.save(PubliserSaksstatistikkTask.lagTask(it.id))
        }
    }

    @Transactional
    fun finnOgAvsluttFagsakerSomSkalAvsluttes(): Int =
        fagsakRepository.finnFagsakerSomSkalAvsluttes()
            .map { oppdaterStatus(it, FagsakStatus.AVSLUTTET) }
            .size

    fun oppdaterStatus(fagsak: Fagsak, nyStatus: FagsakStatus): Fagsak {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status}" +
                " til $nyStatus"
        )
        fagsak.status = nyStatus
        return lagre(fagsak)
    }

    private fun hentForelderdeltagereFraBehandling(
        aktør: Aktør,
        personInfoMedRelasjoner: PdlPersonInfo
    ): List<FagsakDeltagerResponsDto> {
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, FagsakDeltagerResponsDto>()
        personRepository.findByAktør(aktør).filter { it.personopplysningGrunnlag.aktiv }.forEach { person ->
            val behandling = behandlingRepository.hentBehandling(person.personopplysningGrunnlag.behandlingId)
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

    fun hentFagsak(fagsakId: Long): Fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw FunksjonellFeil(
        melding = "Finner ikke fagsak med id $fagsakId",
        frontendFeilmelding = "Finner ikke fagsak med id $fagsakId"
    )

    fun finnFagsakForPerson(aktør: Aktør): Fagsak? = fagsakRepository.finnFagsakForAktør(aktør)

    fun hentFagsakForPerson(aktør: Aktør): Fagsak =
        finnFagsakForPerson(aktør) ?: throw Feil("Fant ikke fagsak på person")

    fun hentFagsakerPåPerson(aktør: Aktør): List<Fagsak> {
        val versjonerAvBarn = personRepository.findByAktør(aktør)
        return versjonerAvBarn.map {
            it.personopplysningGrunnlag.behandlingId
        }.map {
            behandlingRepository.hentBehandling(it).fagsak
        }.distinct()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
