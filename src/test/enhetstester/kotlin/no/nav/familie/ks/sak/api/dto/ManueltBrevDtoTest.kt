package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.UtbetalingEtterKAVedtakDataDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ManueltBrevDtoTest {
    val mottakerNavn = "Mottaker Navnesen"
    val mottakerIdent = randomFnr()

    @Nested
    inner class TilBrev {
        @Test
        fun `skal generere UTBETALING_ETTER_KA_VEDTAK-brev`() {
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.UTBETALING_ETTER_KA_VEDTAK,
                    fritekstAvsnitt = "Fritekst avsnitt",
                )

            val brevDto = manueltBrevDto.tilBrev("Saks Behandlersen").data as UtbetalingEtterKAVedtakDataDto
            with(brevDto.flettefelter) {
                assertThat(fodselsnummer).containsExactly(mottakerIdent)
                assertThat(navn).containsExactly(mottakerNavn)
                assertThat(gjelder).isNull()
            }
            assertThat(brevDto.fritekst).isEqualTo("Fritekst avsnitt")
            assertThat(brevDto.delmalData.signatur.saksbehandler).containsExactly("Saks Behandlersen")
        }

        @Test
        fun `tilBrev genererer 'innhente opplysninger klage'-brev som forventet`() {
            val saksbehandler = "Saks Behandlersen"
            val fritekstAvsnitt = "Fritekst avsnitt"
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE,
                    fritekstAvsnitt = fritekstAvsnitt,
                )
            val brev = manueltBrevDto.tilBrev(saksbehandler).data as InformasjonsbrevInnhenteOpplysningerKlageDataDto
            with(brev.flettefelter) {
                assertThat(fodselsnummer).containsExactly(manueltBrevDto.mottakerIdent)
                assertThat(navn).containsExactly(manueltBrevDto.mottakerNavn)
                assertThat(gjelder).isNull()
            }
            assertThat(brev.delmalData.fritekstAvsnitt.fritekstAvsnittTekst).containsExactly(fritekstAvsnitt)
            assertThat(brev.delmalData.signatur.saksbehandler).containsExactly(saksbehandler)
        }

        @Test
        fun `'innhente opplysninger klage'-brev krever at fritekst avsnitt har en verdi`() {
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE,
                    fritekstAvsnitt = null,
                )
            val funksjonellFeil = assertThrows<FunksjonellFeil> { manueltBrevDto.tilBrev("Saks Behandlersen") }
            assertThat(funksjonellFeil.melding).isEqualTo("Du må legge til fritekst for å forklare hvilke opplysninger du ønsker å innhente.")
        }
    }

    private fun lagManueltBrevDto(
        brevmal: Brevmal,
        mottakerNavn: String = this.mottakerNavn,
        mottakerIdent: String = this.mottakerIdent,
        fritekstAvsnitt: String? = null,
        enhet: Enhet = Enhet("1234", "Enhet"),
    ) = ManueltBrevDto(
        brevmal = brevmal,
        mottakerIdent = mottakerIdent,
        mottakerNavn = mottakerNavn,
        fritekstAvsnitt = fritekstAvsnitt,
        enhet = enhet,
    )
}
