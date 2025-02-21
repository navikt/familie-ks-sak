package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.hjemler.HjemmeltekstUtleder
import no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak.SammensattKontrollsakBrevDtoUtleder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ks.sak.sikkerhet.SaksbehandlerContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GenererBrevServiceTest {
    val brevKlient: BrevKlient = mockk()
    val personopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    val saksbehandlerContext: SaksbehandlerContext = mockk()
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    val genererBrevService =
        GenererBrevService(
            brevKlient = brevKlient,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            simuleringService = mockk<SimuleringService>(),
            vedtaksperiodeService = mockk<VedtaksperiodeService>(),
            brevPeriodeService = mockk<BrevPeriodeService>(),
            korrigertVedtakService = mockk<KorrigertVedtakService>(),
            feilutbetaltValutaService = mockk<FeilutbetaltValutaService>(),
            saksbehandlerContext = saksbehandlerContext,
            sammensattKontrollsakService = mockk<SammensattKontrollsakService>(),
            opprettGrunnlagOgSignaturDataService = mockk<OpprettGrunnlagOgSignaturDataService>(),
            etterbetalingService = mockk<EtterbetalingService>(),
            søkersMeldepliktService = mockk<SøkersMeldepliktService>(),
            sammensattKontrollsakBrevDtoUtleder = mockk<SammensattKontrollsakBrevDtoUtleder>(),
            brevmalService = mockk<BrevmalService>(),
            andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>(),
            hjemmeltekstUtleder = mockk<HjemmeltekstUtleder>(),
        )

    private val søker = randomAktør()
    private val fagsak = lagFagsak(søker)
    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val manueltBrevDto =
        ManueltBrevDto(
            brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
            mottakerIdent = søker.aktivFødselsnummer(),
            multiselectVerdier = listOf("Dokumentasjon som viser når barna kom til Norge."),
            enhet = Enhet("id", "id"),
        )

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["VEDTAK_FØRSTEGANGSVEDTAK", "VEDTAK_ENDRING", "VEDTAK_OPPHØRT", "VEDTAK_OPPHØR_MED_ENDRING", "VEDTAK_AVSLAG", "VEDTAK_FORTSATT_INNVILGET", "VEDTAK_KORREKSJON_VEDTAKSBREV", "VEDTAK_OPPHØR_DØDSFALL", "AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG", "AUTOVEDTAK_NYFØDT_FØRSTE_BARN", "AUTOVEDTAK_NYFØDT_BARN_FRA_FØR"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `genererManueltBrev - skal ikke journalføre brev for brevmaler som ikke kan sendes manuelt`(brevmal: Brevmal) {
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "test"

        val feil =
            assertThrows<Feil> {
                genererBrevService.genererManueltBrev(
                    ManueltBrevDto(
                        brevmal = brevmal,
                        mottakerIdent = søker.aktivFødselsnummer(),
                    ),
                )
            }
        assertEquals("Kan ikke mappe fra manuel brevrequest til $brevmal.", feil.message)
    }

    @Test
    fun `hentForhåndsvisningAvBrev - skal kaste feil dersom kall mot 'familie-brev' feiler`() {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns
            lagPerson(
                lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
                søker,
            )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetNavn = "Behandlende enhet",
                behandlendeEnhetId = "1234",
            )

        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "test"

        every {
            brevKlient.genererBrev(any(), any())
        } throws Exception("Kall mot familie-brev feilet")

        val feil =
            assertThrows<Feil> { genererBrevService.genererManueltBrev(manueltBrevDto, true) }
        assertEquals(
            "Klarte ikke generere brev for ${manueltBrevDto.brevmal}. Kall mot familie-brev feilet",
            feil.message,
        )
        assertEquals(
            "Det har skjedd en feil. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
            feil.frontendFeilmelding,
        )
    }
}
