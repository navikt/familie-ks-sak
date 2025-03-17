package no.nav.familie.ks.sak.kjerne.klage

import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KlageService(
    private val klageClient: KlageClient,
    private val integrasjonClient: IntegrasjonClient,
) {
    fun opprettKlage(
        fagsak: Fagsak,
        kravMottattDato: LocalDate,
    ) {
        if (kravMottattDato.isAfter(LocalDate.now())) {
            throw FunksjonellFeil("Kan ikke opprette klage med krav mottatt frem i tid")
        }

        val aktivtFødselsnummer = fagsak.aktør.aktivFødselsnummer()
        val enhetId = integrasjonClient.hentBehandlendeEnhetForPersonIdentMedRelasjoner(aktivtFødselsnummer).enhetId

        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = aktivtFødselsnummer,
                stønadstype = Stønadstype.KONTANTSTØTTE,
                eksternFagsakId = fagsak.id.toString(),
                fagsystem = Fagsystem.KS,
                klageMottatt = kravMottattDato,
                behandlendeEnhet = enhetId,
                behandlingsårsak = Klagebehandlingsårsak.ORDINÆR,
            ),
        )
    }

    fun hentKlagebehandlingerPåFagsak(fagsakId: Long): List<KlagebehandlingDto> {
        val klagebehandligerPerFagsak = klageClient.hentKlagebehandlinger(setOf(fagsakId))

        val klagerPåFagsak =
            klagebehandligerPerFagsak[fagsakId]
                ?: throw Feil("Fikk ikke fagsakId=$fagsakId tilbake fra kallet til klage.")

        return klagerPåFagsak.map { it.brukVedtaksdatoFraKlageinstansHvisOversendt() }
    }
}
