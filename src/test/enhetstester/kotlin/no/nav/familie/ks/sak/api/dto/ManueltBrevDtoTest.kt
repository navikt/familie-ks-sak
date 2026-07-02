package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.brev.LANDKODER
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SvartidsbrevDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SvartidsbrevDto
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
            // Arrange
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.UTBETALING_ETTER_KA_VEDTAK,
                    fritekstAvsnitt = "Fritekst avsnitt",
                )

            // Act
            val brevDto = manueltBrevDto.tilBrev("Saks Behandlersen") { LANDKODER }.data as UtbetalingEtterKAVedtakDataDto

            // Assert
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
            // Arrange
            val saksbehandler = "Saks Behandlersen"
            val fritekstAvsnitt = "Fritekst avsnitt"
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE,
                    fritekstAvsnitt = fritekstAvsnitt,
                )

            // Act
            val brev = manueltBrevDto.tilBrev(saksbehandler) { LANDKODER }.data as InformasjonsbrevInnhenteOpplysningerKlageDataDto

            // Assert
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
            // Arrange
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE,
                    fritekstAvsnitt = null,
                )

            // Act & Assert
            val funksjonellFeil = assertThrows<FunksjonellFeil> { manueltBrevDto.tilBrev("Saks Behandlersen") { LANDKODER } }
            assertThat(funksjonellFeil.melding).isEqualTo("Du må legge til fritekst for å forklare hvilke opplysninger du ønsker å innhente.")
        }

        @Test
        fun `svartidsbrev med mottakerland skal gi svartidsbrev med SED-delmal`() {
            // Arrange
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.SVARTIDSBREV,
                    behandlingKategori = BehandlingKategori.EØS,
                    mottakerlandSed = listOf("SE", "DK"),
                )

            // Act
            val brev = manueltBrevDto.tilBrev("Saks Behandlersen") { LANDKODER }

            // Assert
            assertThat(brev).isInstanceOf(SvartidsbrevDto::class.java)
            val svartidsbrevData = brev.data as SvartidsbrevDataDto
            assertThat(
                svartidsbrevData.delmalData.sedErSendtTil!!
                    .mottakerlandSed!!
                    .single(),
            ).isEqualTo("Sverige og Danmark")
        }

        @Test
        fun `svartidsbrev uten mottakerland skal gi svartidsbrev uten SED-delmal`() {
            // Arrange
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.SVARTIDSBREV,
                    behandlingKategori = BehandlingKategori.NASJONAL,
                    mottakerlandSed = emptyList(),
                )

            // Act
            val brev = manueltBrevDto.tilBrev("Saks Behandlersen") { LANDKODER }

            // Assert
            assertThat(brev).isInstanceOf(SvartidsbrevDto::class.java)
            val svartidsbrevData = brev.data as SvartidsbrevDataDto
            assertThat(svartidsbrevData.delmalData.sedErSendtTil).isNull()
        }

        @Test
        fun `svartidsbrev med Norge som mottakerland skal kaste funksjonell feil`() {
            // Arrange
            val manueltBrevDto =
                lagManueltBrevDto(
                    brevmal = Brevmal.SVARTIDSBREV,
                    behandlingKategori = BehandlingKategori.EØS,
                    mottakerlandSed = listOf("NO"),
                )

            // Act & Assert
            val funksjonellFeil = assertThrows<FunksjonellFeil> { manueltBrevDto.tilBrev("Saks Behandlersen") { LANDKODER } }
            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("Norge kan ikke velges som mottakerland.")
        }
    }

    private fun lagManueltBrevDto(
        brevmal: Brevmal,
        mottakerNavn: String = this.mottakerNavn,
        mottakerIdent: String = this.mottakerIdent,
        fritekstAvsnitt: String? = null,
        enhet: Enhet = Enhet("1234", "Enhet"),
        behandlingKategori: BehandlingKategori? = null,
        mottakerlandSed: List<String> = emptyList(),
    ) = ManueltBrevDto(
        brevmal = brevmal,
        mottakerIdent = mottakerIdent,
        mottakerNavn = mottakerNavn,
        fritekstAvsnitt = fritekstAvsnitt,
        enhet = enhet,
        behandlingKategori = behandlingKategori,
        mottakerlandSed = mottakerlandSed,
    )
}
