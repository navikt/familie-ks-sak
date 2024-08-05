package no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.ks.sak.common.util.hentDokument
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev.JournalførVedtaksbrevSteg.Companion.KONTANTSTØTTE_VEDTAK_BOKMÅL_VEDLEGG_FILNAVN
import no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev.JournalførVedtaksbrevSteg.Companion.KONTANTSTØTTE_VEDTAK_NYNORSK_VEDLEGG_FILNAVN
import no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev.JournalførVedtaksbrevSteg.Companion.KONTANTSTØTTE_VEDTAK_VEDLEGG_TITTEL
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.prosessering.internal.TaskService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class JournalførVedtaksbrevStegTest {
    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var utgåendeJournalføringService: UtgåendeJournalføringService

    @MockK
    private lateinit var taskService: TaskService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var brevmottakerService: BrevmottakerService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @InjectMockKs
    private lateinit var journalførVedtaksbrevSteg: JournalførVedtaksbrevSteg

    @Test
    fun `journalførVedtaksbrev - skal sende ut vedtaksbrev med bokmål vedlegg dersom søkers målform er NB`() {
        // Arrange
        val dokumentListeSlot = slot<List<Dokument>>()
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = any()) } returns Målform.NB
        every { utgåendeJournalføringService.journalførDokument(any(), any(), any(), any(), capture(dokumentListeSlot), any(), any(), any(), any()) } returns "Test"

        val vedtak = lagVedtak(behandling = lagBehandling(), stønadBrevPdF = ByteArray(5))
        val bokmålDokument = hentDokument(KONTANTSTØTTE_VEDTAK_BOKMÅL_VEDLEGG_FILNAVN)

        // Act
        journalførVedtaksbrevSteg.journalførVedtaksbrev(
            fnr = randomFnr(),
            fagsakId = vedtak.behandling.fagsak.id,
            vedtak = vedtak,
            journalførendeEnhet = "TEST",
            tilVergeEllerFullmektig = false,
            avsenderMottaker = null,
        )

        // Assert
        val dokumentSendt = dokumentListeSlot.captured.single()
        assertThat(dokumentSendt.dokumenttype, Is(Dokumenttype.KONTANTSTØTTE_VEDLEGG))
        assertThat(dokumentSendt.tittel, Is(KONTANTSTØTTE_VEDTAK_VEDLEGG_TITTEL))
        assertThat(dokumentSendt.dokument, Is(bokmålDokument))
    }

    @Test
    fun `journalførVedtaksbrev - skal sende ut vedtaksbrev med nynorsk vedlegg dersom søkers målform er NN`() {
        // Arrange
        val dokumentListeSlot = slot<List<Dokument>>()
        val nynorskDokument = hentDokument(KONTANTSTØTTE_VEDTAK_NYNORSK_VEDLEGG_FILNAVN)
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = any()) } returns Målform.NN
        every { utgåendeJournalføringService.journalførDokument(any(), any(), any(), any(), capture(dokumentListeSlot), any(), any(), any(), any()) } returns "Test"

        val vedtak = lagVedtak(behandling = lagBehandling(), stønadBrevPdF = ByteArray(5))

        // Act
        journalførVedtaksbrevSteg.journalførVedtaksbrev(
            fnr = randomFnr(),
            fagsakId = vedtak.behandling.fagsak.id,
            vedtak = vedtak,
            journalførendeEnhet = "TEST",
            tilVergeEllerFullmektig = false,
            avsenderMottaker = null,
        )

        // Assert
        val dokumentSendt = dokumentListeSlot.captured.single()
        assertThat(dokumentSendt.dokumenttype, Is(Dokumenttype.KONTANTSTØTTE_VEDLEGG))
        assertThat(dokumentSendt.tittel, Is(KONTANTSTØTTE_VEDTAK_VEDLEGG_TITTEL))
        assertThat(dokumentSendt.dokument, Is(nynorskDokument))
    }
}
