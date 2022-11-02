package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonopplysningGrunnlagService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val beregningService: BeregningService,
    private val personService: PersonService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val personidentService: PersonidentService
) {

    fun opprettPersonopplysningGrunnlag(
        behandling: Behandling,
        sisteVedtattBehandling: Behandling?
    ) {
        val søkersAktør = behandling.fagsak.aktør
        val barnasAktørFraSisteVedtattBehandling = when (behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> emptyList()
            BehandlingType.REVURDERING, BehandlingType.TEKNISK_ENDRING -> {
                if (sisteVedtattBehandling == null) {
                    throw Feil("Kan ikke behandle ${behandling.type} uten minst en vedtatt behandling")
                }
                beregningService.finnBarnFraBehandlingMedTilkjentYtelse(sisteVedtattBehandling.id)
            }
        }
        val målform = sisteVedtattBehandling?.let { hentSøkersMålform(behandlingId = it.id) } ?: Målform.NB
        lagreSøkerOgBarnINyttGrunnlag(
            aktør = søkersAktør,
            barnasAktør = barnasAktørFraSisteVedtattBehandling,
            behandling = behandling,
            målform = målform
        )
    }

    @Transactional
    fun oppdaterPersonopplysningGrunnlag(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        søknadDto: SøknadDto
    ) {
        val eksisterendePersonopplysningGrunnlag =
            hentAktivPersonopplysningGrunnlag(behandling.id)
                ?: throw Feil("Det finnes ikke noe aktivt personopplysningsgrunnlag for ${behandling.id}")

        val valgteBarnAktører = søknadDto.barnaMedOpplysninger.filter { it.inkludertISøknaden }
            .map { personidentService.hentOgLagreAktør(it.ident, true) }

        val barnAktører = when {
            forrigeBehandlingSomErVedtatt != null -> {
                // Dersom det finnes en tidligere vedtatt behandling MÅ alle barna som har tilkjent ytelse være med i ny behandling.
                val barnAktørerMedTilkjentYtelse = finnBarnMedTilkjentYtelseIBehandling(forrigeBehandlingSomErVedtatt)
                barnAktørerMedTilkjentYtelse.union(valgteBarnAktører).toList()
            }

            else -> valgteBarnAktører
        }

        lagreSøkerOgBarnINyttGrunnlag(
            aktør = eksisterendePersonopplysningGrunnlag.søker.aktør,
            barnasAktør = barnAktører,
            behandling = behandling,
            målform = eksisterendePersonopplysningGrunnlag.søker.målform
        )
    }

    private fun finnBarnMedTilkjentYtelseIBehandling(behandling: Behandling): List<Aktør> =
        hentBarna(behandlingId = behandling.id)?.map { it.aktør }?.filter {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, it).isNotEmpty()
        } ?: emptyList()

    fun hentSøker(behandlingId: Long): Person? =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.søker

    fun hentBarna(behandlingId: Long): List<Person>? =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.barna

    fun hentSøkersMålform(behandlingId: Long) = hentSøker(behandlingId)?.målform ?: Målform.NB

    fun lagreOgDeaktiverGammel(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag {
        hentAktivPersonopplysningGrunnlag(personopplysningGrunnlag.behandlingId)?.let {
            personopplysningGrunnlagRepository.saveAndFlush(it.also { it.aktiv = false })
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter persongrunnlag $personopplysningGrunnlag")
        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    fun hentAktivPersonopplysningGrunnlag(behandlingId: Long): PersonopplysningGrunnlag? =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)

    fun hentAktivPersonopplysningGrunnlagThrows(behandlingId: Long): PersonopplysningGrunnlag =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)
            ?: throw Feil("Det finnes ikke noe aktivt personopplysningsgrunnlag for $behandlingId")

    @Transactional
    fun lagreSøkerOgBarnINyttGrunnlag(
        aktør: Aktør,
        behandling: Behandling,
        målform: Målform,
        barnasAktør: List<Aktør>
    ): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = lagreOgDeaktiverGammel(PersonopplysningGrunnlag(behandlingId = behandling.id))
        val krevesEnkelPersonInfo = behandling.erSatsendring()
        val søker = personService.lagPerson(
            aktør = aktør,
            personopplysningGrunnlag = personopplysningGrunnlag,
            målform = målform,
            personType = PersonType.SØKER,
            krevesEnkelPersonInfo = krevesEnkelPersonInfo
        )
        personopplysningGrunnlag.personer.add(søker)
        val barna = barnasAktør.map {
            personService.lagPerson(
                aktør = it,
                personopplysningGrunnlag = personopplysningGrunnlag,
                målform = målform,
                personType = PersonType.BARN,
                krevesEnkelPersonInfo = krevesEnkelPersonInfo
            )
        }
        personopplysningGrunnlag.personer.addAll(barna)

        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag).also {
            /**
             * For sikkerhetsskyld fastsetter vi alltid behandlende enhet når nytt personopplysningsgrunnlag opprettes.
             * Dette gjør vi fordi det kan ha blitt introdusert personer med fortrolig adresse.
             */
            arbeidsfordelingService.fastsettBehandledeEnhet(behandling)
        }
    }

    fun oppdaterRegisteropplysningerPåBehandling(behandling: Behandling): PersonopplysningGrunnlag {
        if (behandling.status != BehandlingStatus.UTREDES) {
            throw Feil("BehandlingStatus må være UTREDES for å manuelt oppdatere registeropplysninger")
        }

        val nåværendeGrunnlag = hentAktivPersonopplysningGrunnlagThrows(behandling.id)

        return lagreSøkerOgBarnINyttGrunnlag(
            aktør = nåværendeGrunnlag.søker.aktør,
            barnasAktør = nåværendeGrunnlag.barna.map { it.aktør },
            behandling = behandling,
            målform = nåværendeGrunnlag.søker.målform
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PersonopplysningGrunnlagService::class.java)
    }
}
