package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.UtbetalingEtterKAVedtakDataDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ManueltBrevDtoTest {
    @Nested
    inner class TilBrev {
        @Test
        fun `skal generere UTBETALING_ETTER_KA_VEDTAK-brev`() {
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.UTBETALING_ETTER_KA_VEDTAK,
                    fritekstAvsnitt = "Fritekst avsnitt",
                )

            val brevDto = manueltBrevDto.tilBrev(saksbehandlerNavn = "Saks Behandlersen")

            assertThat(brevDto.mal).isEqualTo(Brevmal.UTBETALING_ETTER_KA_VEDTAK)

            val delmalData = brevDto.data.delmalData as UtbetalingEtterKAVedtakDataDto.DelmalData
            assertThat(delmalData.signatur.saksbehandler).containsExactly("Saks Behandlersen")
            assertThat(delmalData.fritekstAvsnitt?.fritekstAvsnittTekst).containsExactly("Fritekst avsnitt")
        }
    }

    private fun lagManueltBrevDto(
        brevmal: Brevmal,
        mottakerIdent: String = randomFnr(),
        fritekstAvsnitt: String? = null,
        enhet: Enhet = Enhet("1234", "Enhet"),
    ) = ManueltBrevDto(
        brevmal = brevmal,
        mottakerIdent = mottakerIdent,
        fritekstAvsnitt = fritekstAvsnitt,
        enhet = enhet,
    )
}
