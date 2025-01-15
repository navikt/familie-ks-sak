package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.UtbetalingEtterKAVedtakDataDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
