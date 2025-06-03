package no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene

import junit.framework.TestCase.assertTrue
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TotrinnskontrollTest {
    private val behandling = lagBehandling()

    @Nested
    inner class ErUgyldigTest {
        @Test
        fun `Skal returnere false dersom det er samme navn på saksbehandler og beslutter men annen ID`() {
            val kontroll =
                Totrinnskontroll(
                    behandling = behandling,
                    godkjent = true,
                    saksbehandler = "NAV-Bruker",
                    saksbehandlerId = "X123Saksbehandler",
                    beslutter = "NAV-Bruker",
                    beslutterId = "X123Beslutter",
                )
            assertFalse(kontroll.erUgyldig())
        }

        @Test
        fun `Skal returnere false dersom det er samme person som beslutter men det er system brukeren`() {
            val kontroll =
                Totrinnskontroll(
                    behandling = behandling,
                    godkjent = true,
                    saksbehandler = SikkerhetContext.SYSTEM_NAVN,
                    saksbehandlerId = "VL",
                    beslutter = SikkerhetContext.SYSTEM_NAVN,
                    beslutterId = "VL",
                )

            assertFalse(kontroll.erUgyldig())
        }

        @Test
        fun `Skal returnere false dersom det er forskjellige brukere som har fattet vedtak og besluttet`() {
            val kontroll =
                Totrinnskontroll(
                    behandling = behandling,
                    godkjent = true,
                    saksbehandler = "Bruker1",
                    saksbehandlerId = "ID1",
                    beslutter = "Bruker2",
                    beslutterId = "ID2",
                )

            assertFalse(kontroll.erUgyldig())
        }

        @Test
        fun `Skal returnere true dersom id'ne er det samme selvom navn er annerledes på saksbehandler og beslutter`() {
            val kontroll =
                Totrinnskontroll(
                    behandling = behandling,
                    godkjent = true,
                    saksbehandler = "X1",
                    saksbehandlerId = "samme-id",
                    beslutter = "Y1",
                    beslutterId = "samme-id",
                )

            assertTrue(kontroll.erUgyldig())
        }
    }
}
