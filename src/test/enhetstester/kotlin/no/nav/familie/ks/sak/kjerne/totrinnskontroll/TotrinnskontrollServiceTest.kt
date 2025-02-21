package no.nav.familie.ks.sak.kjerne.totrinnskontroll

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.hamcrest.CoreMatchers.`is` as Is

class TotrinnskontrollServiceTest {
    private val totrinnskontrollRepository = mockk<TotrinnskontrollRepository>()

    private val totrinnskontrollService = TotrinnskontrollService(totrinnskontrollRepository)

    @Test
    fun `finnAktivForBehandling skal returnere null dersom det ikke finnes aktiv totrinnskontroll for behandling`() {
        every { totrinnskontrollRepository.findByBehandlingAndAktiv(404) } returns null

        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(404)

        assertThat(totrinnskontroll, Is(nullValue()))
    }

    @Test
    fun `finnAktivForBehandling skal returnere totrinnskontroll dersom det finnes aktiv for behandling`() {
        val mocketTotrinnskontroll = mockk<Totrinnskontroll>()
        every { totrinnskontrollRepository.findByBehandlingAndAktiv(200) } returns mocketTotrinnskontroll

        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(200)

        assertThat(totrinnskontroll, Is(notNullValue()))
        assertThat(totrinnskontroll, Is(mocketTotrinnskontroll))
    }

    @Test
    fun `hentAktivForBehandling skal kaste feil dersom det ikke eksisterer aktiv totrinnskontroll for behandling`() {
        every { totrinnskontrollRepository.findByBehandlingAndAktiv(404) } returns null

        val feil =
            assertThrows<Feil> {
                totrinnskontrollService.hentAktivForBehandling(404)
            }

        assertThat(feil.message, Is("Fant ikke aktiv totrinnskontroll for behandling 404"))
    }

    @Test
    fun `hentAktivForBehandling skal returnere totrinnskontroll dersom det finnes aktiv for behandling`() {
        val mocketTotrinnskontroll = mockk<Totrinnskontroll>()
        every { totrinnskontrollRepository.findByBehandlingAndAktiv(200) } returns mocketTotrinnskontroll

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(200)

        assertThat(totrinnskontroll, Is(notNullValue()))
        assertThat(totrinnskontroll, Is(mocketTotrinnskontroll))
    }

    @Test
    fun `besluttTotrinnskontroll skal kaste funksjonell feil hvis totrinnskontroll er ugyldig`() {
        val mocketTotrinnskontroll = mockk<Totrinnskontroll>(relaxed = true)

        every { mocketTotrinnskontroll.erUgyldig() } returns true
        every { totrinnskontrollRepository.findByBehandlingAndAktiv(200) } returns mocketTotrinnskontroll

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                totrinnskontrollService.besluttTotrinnskontroll(
                    200,
                    "beslutter",
                    "beslutterId",
                    Beslutning.GODKJENT,
                )
            }

        assertThat(
            funksjonellFeil.message,
            Is("Samme saksbehandler kan ikke foreslå og beslutte iverksetting på samme vedtak"),
        )
        assertThat(funksjonellFeil.frontendFeilmelding, Is("Du kan ikke godkjenne ditt eget vedtak"))
    }

    @Test
    fun `besluttTotrinnskontroll skal oppdatere behandling status til iverksetter vedtak hvis beslutning er godkjent`() {
        val mocketTotrinnskontroll = mockk<Totrinnskontroll>(relaxed = true)

        every { mocketTotrinnskontroll.erUgyldig() } returns false
        every { totrinnskontrollRepository.findByBehandlingAndAktiv(200) } returns mocketTotrinnskontroll
        every { totrinnskontrollRepository.save(mocketTotrinnskontroll) } returns mocketTotrinnskontroll

        totrinnskontrollService.besluttTotrinnskontroll(
            200,
            "beslutter",
            "beslutterId",
            Beslutning.GODKJENT,
        )

        verify(exactly = 1) { mocketTotrinnskontroll.erUgyldig() }
        verify(exactly = 1) { totrinnskontrollRepository.findByBehandlingAndAktiv(200) }
        verify(exactly = 1) { totrinnskontrollRepository.save(mocketTotrinnskontroll) }
    }

    @Test
    fun `lagreOgDeaktiverGammel skal lagre ny totrinnskontroll`() {
        val mocketNyTotrinnskontroll = mockk<Totrinnskontroll>(relaxed = true)
        val mocketEksisterendeTotrinnskontroll = mockk<Totrinnskontroll>(relaxed = true)

        every {
            totrinnskontrollRepository.findByBehandlingAndAktiv(mocketNyTotrinnskontroll.behandling.id)
        } returns mocketEksisterendeTotrinnskontroll
        every { totrinnskontrollRepository.save(mocketNyTotrinnskontroll) } returns mocketNyTotrinnskontroll
        every { totrinnskontrollRepository.saveAndFlush(mocketEksisterendeTotrinnskontroll) } returns mocketEksisterendeTotrinnskontroll
        every { mocketEksisterendeTotrinnskontroll.id } returns 200

        val totrinnskontroll = totrinnskontrollService.lagreOgDeaktiverGammel(mocketNyTotrinnskontroll)

        assertThat(totrinnskontroll, Is(notNullValue()))
        assertThat(totrinnskontroll, Is(mocketNyTotrinnskontroll))

        verify(exactly = 1) { totrinnskontrollRepository.findByBehandlingAndAktiv(mocketNyTotrinnskontroll.behandling.id) }
        verify(exactly = 1) { totrinnskontrollRepository.saveAndFlush(mocketEksisterendeTotrinnskontroll) }
        verify(exactly = 1) { mocketEksisterendeTotrinnskontroll.id }
        verify(exactly = 1) { mocketEksisterendeTotrinnskontroll.aktiv = false }
    }
}
