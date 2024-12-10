package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrevUtilKtTest {
    @Nested
    inner class HentHjemmeltekstTest {
        @Test
        fun `skal legge til forskrift om overgangsregler dersom overgangsordning begrunnelser er brukt`() {
            // Arrange
            val sanitybegrunnelserBruktIBrev =
                listOf(
                    lagSanityBegrunnelse(
                        apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING.sanityApiNavn,
                        hjemler = listOf("1"),
                    ),
                )

            // Act
            val hjemmeltekster =
                hentHjemmeltekst(
                    sanitybegrunnelserBruktIBrev = sanitybegrunnelserBruktIBrev,
                    målform = Målform.NB,
                    refusjonEøsHjemmelSkalMedIBrev = false,
                )

            // Assert
            assertThat(hjemmeltekster).isEqualTo("kontantstøtteloven § 1 og forskrift om overgangsregler")
        }

        @Test
        fun `skal ikke legge til forskrift om overgangsregler dersom overgangsordning begrunnelser ikke er brukt`() {
            // Arrange
            val sanitybegrunnelserBruktIBrev =
                listOf(
                    lagSanityBegrunnelse(
                        apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
                        hjemler = listOf("1"),
                    ),
                )

            // Act
            val hjemmeltekster =
                hentHjemmeltekst(
                    sanitybegrunnelserBruktIBrev = sanitybegrunnelserBruktIBrev,
                    målform = Målform.NB,
                    refusjonEøsHjemmelSkalMedIBrev = false,
                )

            // Assert
            assertThat(hjemmeltekster).isEqualTo("kontantstøtteloven § 1")
        }
    }
}
