package no.nav.familie.ks.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.ks.sak.common.TestClockProvider
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class KlagebehandlingOppretterTest {
    private val dagensDato = LocalDate.of(2025, 8, 25)

    private val fagsakService = mockk<FagsakService>()
    private val klageClient = mockk<KlageClient>()
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val tilpassArbeidsfordelingService = mockk<TilpassArbeidsfordelingService>()
    private val clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(dagensDato)

    private val klagebehandlingOppretter =
        KlagebehandlingOppretter(
            fagsakService,
            klageClient,
            integrasjonClient,
            tilpassArbeidsfordelingService,
            clockProvider,
        )

    @Nested
    inner class OpprettKlage {
        @Test
        fun `skal kaste exception hvis klagebehandlingen blir mottatt etter dagens dato`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato.plusDays(1)

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)
                }
            assertThat(exception.message).isEqualTo("Kan ikke opprette klage med krav mottatt frem i tid.")
        }

        @Test
        fun `skal kaste exception om man ikke finner en behandlende enhet for aktør`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato

            every { integrasjonClient.hentBehandlendeEnheter(fagsak.aktør.aktivFødselsnummer()) } returns emptyList()

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)
                }
            assertThat(exception.message).isEqualTo("Fant ingen arbeidsfordelingsenhet for aktør.")
        }

        @Test
        fun `skal kaste exception om man ikke finner flere behandlende enheter for aktør`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato

            every { integrasjonClient.hentBehandlendeEnheter(fagsak.aktør.aktivFødselsnummer()) } returns
                listOf(
                    Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.OSLO),
                    Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.VADSØ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)
                }
            assertThat(exception.message).isEqualTo("Fant flere arbeidsfordelingsenheter for aktør.")
        }

        @Test
        fun `skal opprette klage ved å sende inn kun fagsakId som parameter`() {
            // Arrange
            val fagsak = lagFagsak()
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            every { integrasjonClient.hentBehandlendeEnheter(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak.id, dagensDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.KONTANTSTØTTE)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.KS)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }

        @Test
        fun `skal lage OpprettKlageRequest uten å tilpasse enhetsnummeret fra fagsak aktør`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { integrasjonClient.hentBehandlendeEnheter(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.KONTANTSTØTTE)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.KS)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }

        @Test
        fun `skal lage OpprettKlageRequest og tilpasse enhetsnummeret fra fagsak aktør basert på tilgangene til saksbehandler`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.OSLO)
            val tilpassetArbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.VADSØ)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { integrasjonClient.hentBehandlendeEnheter(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns tilpassetArbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.KONTANTSTØTTE)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.KS)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(tilpassetArbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }
    }
}
