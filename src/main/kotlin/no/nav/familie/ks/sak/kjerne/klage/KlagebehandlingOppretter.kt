package no.nav.familie.ks.sak.kjerne.klage

import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.ks.sak.common.ClockProvider
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class KlagebehandlingOppretter(
    private val fagsakService: FagsakService,
    private val klageClient: KlageClient,
    private val integrasjonClient: IntegrasjonClient,
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService,
    private val clockProvider: ClockProvider,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    private val logger = LoggerFactory.getLogger(KlagebehandlingOppretter::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun opprettKlage(
        fagsakId: Long,
        klageMottattDato: LocalDate,
    ): UUID {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        return opprettKlage(fagsak, klageMottattDato)
    }

    fun opprettKlage(
        fagsak: Fagsak,
        klageMottattDato: LocalDate,
    ): UUID {
        if (klageMottattDato.isAfter(LocalDate.now(clockProvider.get()))) {
            throw FunksjonellFeil("Kan ikke opprette klage med krav mottatt frem i tid.")
        }

        val fødselsnummer = fagsak.aktør.aktivFødselsnummer()
        val navIdent = NavIdent(SikkerhetContext.hentSaksbehandler())

        val behandlendeEnhet =
            if (unleashNextMedContextService.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_AA_FINNE_ENHET_FOR_OPPRETTING_AV_KLAGEBEHANDLING)) {
                val arbeidsfordelingsenheter = integrasjonClient.hentBehandlendeEnheter(fødselsnummer)

                if (arbeidsfordelingsenheter.isEmpty()) {
                    logger.error("Fant ingen arbeidsfordelingsenheter for aktør. Se SecureLogs for detaljer.")
                    secureLogger.error("Fant ingen arbeidsfordelingsenheter for aktør $fødselsnummer.")
                    throw Feil("Fant ingen arbeidsfordelingsenhet for aktør.")
                }

                if (arbeidsfordelingsenheter.size > 1) {
                    logger.error("Fant flere arbeidsfordelingsenheter for aktør. Se SecureLogs for detaljer.")
                    secureLogger.error("Fant flere arbeidsfordelingsenheter for aktør $fødselsnummer.")
                    throw Feil("Fant flere arbeidsfordelingsenheter for aktør.")
                }

                val tilpassetArbeidsfordelingsenhet =
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenheter.single(),
                        navIdent,
                    )

                tilpassetArbeidsfordelingsenhet.enhetId
            } else {
                integrasjonClient
                    .hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent)
                    .first()
                    .enhetsnummer
            }

        return klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = fødselsnummer,
                stønadstype = Stønadstype.KONTANTSTØTTE,
                eksternFagsakId = fagsak.id.toString(),
                fagsystem = Fagsystem.KS,
                klageMottatt = klageMottattDato,
                behandlendeEnhet = behandlendeEnhet,
                behandlingsårsak = Klagebehandlingsårsak.ORDINÆR,
            ),
        )
    }
}
