package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
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
    private val loggService: LoggService
) {
    fun finnArbeidsfordelingPåBehandling(behandlingId: Long) =
        arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandlingId)
            ?: error("Finner ikke tilknyttet arbeidsfordeling på behandling med id $behandlingId")

    fun fastsettBehandledeEnhet(behandling: Behandling, sisteVedtattBehandling: Behandling? = null) {
        val aktivArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)

        val oppdatertArbeidsfordelingPåBehandling = if (behandling.erSatsendring()) {
            aktivArbeidsfordelingPåBehandling ?: fastsettBehandledeEnhetPåSatsendringsbehandling(
                behandling,
                sisteVedtattBehandling
            )
        } else {
            val arbeidsfordelingsenhet = hentArbeidsfordelingsenhet(behandling)
            when (aktivArbeidsfordelingPåBehandling) {
                null -> arbeidsfordelingPåBehandlingRepository.save(
                    ArbeidsfordelingPåBehandling(
                        behandlingId = behandling.id,
                        behandlendeEnhetId = arbeidsfordelingsenhet.enhetId,
                        behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn
                    )
                )
                else -> {
                    if (!aktivArbeidsfordelingPåBehandling.manueltOverstyrt &&
                        (aktivArbeidsfordelingPåBehandling.behandlendeEnhetId != arbeidsfordelingsenhet.enhetId)
                    ) {
                        aktivArbeidsfordelingPåBehandling.also {
                            it.behandlendeEnhetId = arbeidsfordelingsenhet.enhetId
                            it.behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn
                        }
                        arbeidsfordelingPåBehandlingRepository.save(aktivArbeidsfordelingPåBehandling)
                    }
                    aktivArbeidsfordelingPåBehandling
                }
            }
        }
        settBehandlendeEnhet(
            behandling = behandling,
            aktivArbeidsfordelingPåBehandling = aktivArbeidsfordelingPåBehandling,
            oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = false
        )
    }

    fun manueltOppdaterBehandlendeEnhet(behandling: Behandling, endreBehandlendeEnhet: EndreBehandlendeEnhetDto) {
        val aktivArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)
                ?: throw Feil("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling ${behandling.id}")

        val oppdatertArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.save(
            aktivArbeidsfordelingPåBehandling.copy(
                behandlendeEnhetId = endreBehandlendeEnhet.enhetId,
                behandlendeEnhetNavn = integrasjonClient.hentNavKontorEnhet(endreBehandlendeEnhet.enhetId).navn,
                manueltOverstyrt = true
            )
        )

        settBehandlendeEnhet(
            behandling = behandling,
            aktivArbeidsfordelingPåBehandling = aktivArbeidsfordelingPåBehandling,
            oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = true,
            begrunnelse = endreBehandlendeEnhet.begrunnelse
        )
    }

    fun hentArbeidsfordelingsenhet(behandling: Behandling): Arbeidsfordelingsenhet {
        val søker = identMedAdressebeskyttelse(behandling.fagsak.aktør)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)

        val personer = personopplysningGrunnlag?.barna?.map { barn -> identMedAdressebeskyttelse(barn.aktør) }?.plus(søker)
            ?: listOf(søker)

        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(personer)

        return integrasjonClient.hentBehandlendeEnheter(identMedStrengeste ?: søker.first).singleOrNull()
            ?: throw Feil(message = "Fant flere eller ingen enheter på behandling.")
    }

    private fun fastsettBehandledeEnhetPåSatsendringsbehandling(
        behandling: Behandling,
        sisteVedtattBehandling: Behandling?
    ): ArbeidsfordelingPåBehandling {
        return if (sisteVedtattBehandling != null) {
            val forrigeVedtattBehandlingArbeidsfordelingsenhet =
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(sisteVedtattBehandling.id)

            arbeidsfordelingPåBehandlingRepository.save(
                forrigeVedtattBehandlingArbeidsfordelingsenhet?.copy(behandlingId = behandling.id)
                    ?: throw Feil(
                        "Finner ikke arbeidsfordelingsenhet på " +
                            "forrige vedtatt behandling på satsendringsbehandling"
                    )
            )
        } else {
            throw Feil("Klarte ikke å fastsette arbeidsfordelingsenhet på satsendringsbehandling.")
        }
    }

    private fun settBehandlendeEnhet(
        behandling: Behandling,
        aktivArbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling?,
        oppdatertArbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        manuellOppdatering: Boolean,
        begrunnelse: String = ""
    ) {
        val oppdateringstype = if (manuellOppdatering) "manuelt" else "automatisk"
        logger.info(
            "Fastsatt behandlende enhet $oppdateringstype på behandling ${behandling.id}: " +
                "$oppdatertArbeidsfordelingPåBehandling"
        )
        secureLogger.info(
            "Fastsatt behandlende enhet oppdateringstype på behandling ${behandling.id}: " +
                oppdatertArbeidsfordelingPåBehandling.toSecureString()
        )

        if (aktivArbeidsfordelingPåBehandling != null &&
            aktivArbeidsfordelingPåBehandling.behandlendeEnhetId != oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId
        ) {
            loggService.opprettBehandlendeEnhetEndret(
                behandling = behandling,
                fraEnhet = aktivArbeidsfordelingPåBehandling,
                tilEnhet = oppdatertArbeidsfordelingPåBehandling,
                manuellOppdatering = manuellOppdatering,
                begrunnelse = begrunnelse
            )

            oppgaveService.patchOppgaverForBehandling(behandling) {
                logger.info(
                    "Oppdaterer enhet fra ${it.tildeltEnhetsnr} " +
                        "til ${oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId} på oppgave ${it.id}"
                )
                it.copy(tildeltEnhetsnr = oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId)
            }
        }
    }

    private fun identMedAdressebeskyttelse(aktør: Aktør) =
        aktør.aktivFødselsnummer() to personOpplysningerService.hentPersoninfoEnkel(aktør).adressebeskyttelseGradering

    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val logger = LoggerFactory.getLogger(ArbeidsfordelingService::class.java)
    }
}
