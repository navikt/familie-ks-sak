package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.ArbeidsfordelingsEnhet
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val integrasjonClient: IntegrasjonClient,
    private val personOpplysningerService: PersonOpplysningerService,
    private val oppgaveService: OppgaveService,
    private val loggService: LoggService,
    private val personidentService: PersonidentService,
) {
    fun hentAlleBehandlingerPåEnhet(enhetId: String) =
        arbeidsfordelingPåBehandlingRepository.hentAlleArbeidsfordelingPåBehandlingMedEnhet(enhetId)

    fun hentArbeidsfordelingPåBehandling(behandlingId: Long) =
        arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandlingId)
            ?: error("Finner ikke tilknyttet arbeidsfordeling på behandling med id $behandlingId")

    fun fastsettBehandledeEnhet(
        behandling: Behandling,
        sisteVedtattBehandling: Behandling? = null,
    ) {
        val aktivArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)
        val arbeidsfordelingsEnhet = hentArbeidsfordelingsEnhet(behandling)

        val oppdatertArbeidsfordelingPåBehandling =
            if (behandling.erSatsendring()) {
                aktivArbeidsfordelingPåBehandling ?: fastsettBehandledeEnhetPåSatsendringsbehandling(
                    behandling,
                    sisteVedtattBehandling,
                )
            } else {
                when (aktivArbeidsfordelingPåBehandling) {
                    null ->
                        arbeidsfordelingPåBehandlingRepository.save(
                            ArbeidsfordelingPåBehandling(
                                behandlingId = behandling.id,
                                behandlendeEnhetId = arbeidsfordelingsEnhet.enhetId,
                                behandlendeEnhetNavn = arbeidsfordelingsEnhet.enhetNavn,
                            ),
                        )
                    else -> {
                        if (!aktivArbeidsfordelingPåBehandling.manueltOverstyrt &&
                            (aktivArbeidsfordelingPåBehandling.behandlendeEnhetId != arbeidsfordelingsEnhet.enhetId)
                        ) {
                            aktivArbeidsfordelingPåBehandling.also {
                                it.behandlendeEnhetId = arbeidsfordelingsEnhet.enhetId
                                it.behandlendeEnhetNavn = arbeidsfordelingsEnhet.enhetNavn
                            }
                            arbeidsfordelingPåBehandlingRepository.save(aktivArbeidsfordelingPåBehandling)
                        }
                        aktivArbeidsfordelingPåBehandling
                    }
                }
            }
        settBehandlendeEnhet(
            behandling = behandling,
            aktivArbeidsfordelingEnhet = arbeidsfordelingsEnhet,
            oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = false,
        )
    }

    fun manueltOppdaterBehandlendeEnhet(
        behandling: Behandling,
        endreBehandlendeEnhet: EndreBehandlendeEnhetDto,
    ) {
        val aktivArbeidsfordelingPåBehandling = hentArbeidsfordelingPåBehandling(behandling.id)

        val aktivArbeidsfordelingsEnhet =
            ArbeidsfordelingsEnhet(
                enhetId = aktivArbeidsfordelingPåBehandling.behandlendeEnhetId,
                enhetNavn = aktivArbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            )

        val oppdatertArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.save(
                aktivArbeidsfordelingPåBehandling.copy(
                    behandlendeEnhetId = endreBehandlendeEnhet.enhetId,
                    behandlendeEnhetNavn = integrasjonClient.hentNavKontorEnhet(endreBehandlendeEnhet.enhetId).navn,
                    manueltOverstyrt = true,
                ),
            )

        settBehandlendeEnhet(
            behandling = behandling,
            aktivArbeidsfordelingEnhet = aktivArbeidsfordelingsEnhet,
            oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = true,
            begrunnelse = endreBehandlendeEnhet.begrunnelse,
        )
    }

    fun hentArbeidsfordelingsEnhet(behandling: Behandling): ArbeidsfordelingsEnhet {
        val søker = identMedAdressebeskyttelse(behandling.fagsak.aktør)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)

        val personer =
            personopplysningGrunnlag?.barna?.map { barn -> identMedAdressebeskyttelse(barn.aktør) }?.plus(søker)
                ?: listOf(søker)

        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(personer)

        return integrasjonClient.hentBehandlendeEnheter(identMedStrengeste ?: søker.first).singleOrNull()
            ?: throw Feil(message = "Fant flere eller ingen enheter på behandling.")
    }

    fun hentArbeidsfordelingsenhetPåIdenter(
        søkerIdent: String,
        barnIdenter: List<String>,
    ): ArbeidsfordelingsEnhet {
        val identerLagtSammen = barnIdenter + søkerIdent

        val identTilAdresseBeskyttelseGraderingMap =
            identerLagtSammen.map {
                it to identMedAdressebeskyttelse(it).adressebeskyttelsegradering
            }

        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(identTilAdresseBeskyttelseGraderingMap)

        return integrasjonClient.hentBehandlendeEnheter(identMedStrengeste ?: søkerIdent).singleOrNull()
            ?: throw Feil(message = "Fant flere eller ingen enheter på behandling.")
    }

    private fun fastsettBehandledeEnhetPåSatsendringsbehandling(
        behandling: Behandling,
        sisteVedtattBehandling: Behandling?,
    ): ArbeidsfordelingPåBehandling =
        if (sisteVedtattBehandling != null) {
            val forrigeVedtattBehandlingArbeidsfordelingsenhet =
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(sisteVedtattBehandling.id)

            arbeidsfordelingPåBehandlingRepository.save(
                forrigeVedtattBehandlingArbeidsfordelingsenhet?.copy(behandlingId = behandling.id)
                    ?: throw Feil(
                        "Finner ikke arbeidsfordelingsenhet på " +
                            "forrige vedtatt behandling på satsendringsbehandling",
                    ),
            )
        } else {
            throw Feil("Klarte ikke å fastsette arbeidsfordelingsenhet på satsendringsbehandling.")
        }

    private fun settBehandlendeEnhet(
        behandling: Behandling,
        aktivArbeidsfordelingEnhet: ArbeidsfordelingsEnhet,
        oppdatertArbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        manuellOppdatering: Boolean,
        begrunnelse: String = "",
    ) {
        val oppdateringstype = if (manuellOppdatering) "manuelt" else "automatisk"
        logger.info(
            "Fastsatt behandlende enhet $oppdateringstype på behandling ${behandling.id}: " +
                "$oppdatertArbeidsfordelingPåBehandling",
        )
        secureLogger.info(
            "Fastsatt behandlende enhet oppdateringstype på behandling ${behandling.id}: " +
                oppdatertArbeidsfordelingPåBehandling.toSecureString(),
        )

        if (aktivArbeidsfordelingEnhet.enhetId != oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId
        ) {
            loggService.opprettBehandlendeEnhetEndret(
                behandling = behandling,
                fraEnhet = aktivArbeidsfordelingEnhet,
                tilEnhet = oppdatertArbeidsfordelingPåBehandling,
                manuellOppdatering = manuellOppdatering,
                begrunnelse = begrunnelse,
            )

            oppgaveService.endreTilordnetEnhetPåOppgaverForBehandling(
                behandling,
                oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId,
            )
        }
    }

    private fun identMedAdressebeskyttelse(aktør: Aktør) =
        aktør.aktivFødselsnummer() to personOpplysningerService.hentPersoninfoEnkel(aktør).adressebeskyttelseGradering

    private fun identMedAdressebeskyttelse(ident: String) =
        IdentMedAdressebeskyttelse(
            ident = ident,
            adressebeskyttelsegradering =
                personOpplysningerService
                    .hentPersoninfoEnkel(
                        personidentService.hentAktør(ident),
                    ).adressebeskyttelseGradering,
        )

    data class IdentMedAdressebeskyttelse(
        val ident: String,
        val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING?,
    )

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val logger = LoggerFactory.getLogger(ArbeidsfordelingService::class.java)
    }
}
