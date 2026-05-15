package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagToTrinnskontroll
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpprettGrunnlagOgSignaturDataServiceTest {
    private val mockedPersonopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    private val mockedTotrinnskontrollService: TotrinnskontrollService = mockk()
    private val mockedArbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val opprettGrunnlagOgSignaturDataService: OpprettGrunnlagOgSignaturDataService =
        OpprettGrunnlagOgSignaturDataService(
            personopplysningGrunnlagService = mockedPersonopplysningGrunnlagService,
            totrinnskontrollService = mockedTotrinnskontrollService,
            arbeidsfordelingService = mockedArbeidsfordelingService,
        )

    @Test
    fun `skal generere grunnlag og singatur data`() {
        // Arrange
        val vedtak = lagVedtak()

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = vedtak.behandling.id,
            )

        every {
            mockedPersonopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(
                behandlingId = vedtak.behandling.id,
            )
        } returns personopplysningGrunnlag

        val toTrinnskontroll =
            lagToTrinnskontroll(
                behandling = vedtak.behandling,
            )

        every {
            mockedTotrinnskontrollService.finnAktivForBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns toTrinnskontroll

        val arbeidsfordelingPåBehandling =
            lagArbeidsfordelingPåBehandling(
                behandlingId = vedtak.behandling.id,
            )

        every {
            mockedArbeidsfordelingService.hentArbeidsfordelingPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns arbeidsfordelingPåBehandling

        // Act
        val grunnlagOgSignaturData = opprettGrunnlagOgSignaturDataService.opprett(vedtak)

        // Assert
        assertThat(grunnlagOgSignaturData.grunnlag).isEqualTo(personopplysningGrunnlag)
        assertThat(grunnlagOgSignaturData.saksbehandler).isEqualTo("saksbehandler")
        assertThat(grunnlagOgSignaturData.beslutter).isEqualTo("beslutter")
        assertThat(grunnlagOgSignaturData.enhet).isEqualTo("Test enhet")
    }
}
