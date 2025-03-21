package no.nav.familie.ks.sak.statistikk.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagRelatertBehandling
import no.nav.familie.ks.sak.data.lagToTrinnskontroll
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService.Companion.TIMEZONE
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SakStatistikkServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val taskService = mockk<TaskService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val fagsakService = mockk<FagsakService>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val personopplysningService = mockk<PersonopplysningerService>()
    private val relatertBehandlingUtleder = mockk<RelatertBehandlingUtleder>()
    private val sakStatistikkService =
        SakStatistikkService(
            behandlingRepository = behandlingRepository,
            fagsakRepository = fagsakRepository,
            taskService = taskService,
            totrinnskontrollService = totrinnskontrollService,
            arbeidsfordelingService = arbeidsfordelingService,
            fagsakService = fagsakService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            personopplysningService = personopplysningService,
            relatertBehandlingUtleder = relatertBehandlingUtleder,
        )

    @Nested
    inner class HentBehandlingensTilstandV2 {
        @Test
        fun `skal hente statistikk for behandling med relatert behandling`() {
            // Arrange
            val behandlingId = 1L

            val behandling = lagBehandling(id = behandlingId)
            val arbeidsfordelingPåBehandling = lagArbeidsfordelingPåBehandling(behandlingId = behandlingId)
            val totrinnskontroll = lagToTrinnskontroll(behandling = behandling)
            val relatertBehandling = lagRelatertBehandling()

            every { behandlingRepository.hentBehandling(behandlingId) } returns behandling
            every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId) } returns arbeidsfordelingPåBehandling
            every { totrinnskontrollService.finnAktivForBehandling(behandlingId) } returns totrinnskontroll
            every { relatertBehandlingUtleder.utledRelatertBehandling(behandling.fagsak.id) } returns relatertBehandling

            // Act
            val behandlingStatistikkV2Dto =
                sakStatistikkService.hentBehandlingensTilstandV2(
                    behandlingId = behandlingId,
                    brukEndretTidspunktSomFunksjonellTidspunkt = true,
                )

            // Assert
            assertThat(behandlingStatistikkV2Dto.saksnummer).isEqualTo(behandling.fagsak.id)
            assertThat(behandlingStatistikkV2Dto.behandlingID).isEqualTo(behandling.id)
            assertThat(behandlingStatistikkV2Dto.mottattTid).isEqualTo(behandling.opprettetTidspunkt.atZone(TIMEZONE))
            assertThat(behandlingStatistikkV2Dto.registrertTid).isEqualTo(behandling.opprettetTidspunkt.atZone(TIMEZONE))
            assertThat(behandlingStatistikkV2Dto.behandlingType).isEqualTo(behandling.type)
            assertThat(behandlingStatistikkV2Dto.utenlandstilsnitt).isEqualTo(behandling.kategori.name)
            assertThat(behandlingStatistikkV2Dto.behandlingStatus).isEqualTo(behandling.status)
            assertThat(behandlingStatistikkV2Dto.behandlingsResultat).isEqualTo(behandling.resultat)
            assertThat(behandlingStatistikkV2Dto.ansvarligEnhet).isEqualTo(arbeidsfordelingPåBehandling.behandlendeEnhetId)
            assertThat(behandlingStatistikkV2Dto.ansvarligBeslutter).isEqualTo(totrinnskontroll.beslutterId)
            assertThat(behandlingStatistikkV2Dto.ansvarligSaksbehandler).isEqualTo(totrinnskontroll.saksbehandlerId)
            assertThat(behandlingStatistikkV2Dto.behandlingErManueltOpprettet).isTrue()
            assertThat(behandlingStatistikkV2Dto.funksjoneltTidspunkt).isEqualTo(behandling.endretTidspunkt.atZone(TIMEZONE))
            assertThat(behandlingStatistikkV2Dto.automatiskBehandlet).isFalse()
            assertThat(behandlingStatistikkV2Dto.sattPaaVent).isNull()
            assertThat(behandlingStatistikkV2Dto.behandlingOpprettetÅrsak).isEqualTo(behandling.opprettetÅrsak)
            assertThat(behandlingStatistikkV2Dto.relatertBehandlingId).isEqualTo(relatertBehandling.id)
            assertThat(behandlingStatistikkV2Dto.relatertBehandlingFagsystem).isEqualTo(relatertBehandling.fagsystem.name)
        }

        @Test
        fun `skal hente statistikk for behandling uten relatert behandling`() {
            // Arrange
            val behandlingId = 1L

            val behandling = lagBehandling(id = behandlingId)
            val arbeidsfordelingPåBehandling = lagArbeidsfordelingPåBehandling(behandlingId = behandlingId)
            val totrinnskontroll = lagToTrinnskontroll(behandling = behandling)

            every { behandlingRepository.hentBehandling(behandlingId) } returns behandling
            every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId) } returns arbeidsfordelingPåBehandling
            every { totrinnskontrollService.finnAktivForBehandling(behandlingId) } returns totrinnskontroll
            every { relatertBehandlingUtleder.utledRelatertBehandling(behandling.fagsak.id) } returns null

            // Act
            val behandlingStatistikkV2Dto =
                sakStatistikkService.hentBehandlingensTilstandV2(
                    behandlingId = behandlingId,
                    brukEndretTidspunktSomFunksjonellTidspunkt = true,
                )

            // Assert
            assertThat(behandlingStatistikkV2Dto.saksnummer).isEqualTo(behandling.fagsak.id)
            assertThat(behandlingStatistikkV2Dto.behandlingID).isEqualTo(behandling.id)
            assertThat(behandlingStatistikkV2Dto.mottattTid).isEqualTo(behandling.opprettetTidspunkt.atZone(TIMEZONE))
            assertThat(behandlingStatistikkV2Dto.registrertTid).isEqualTo(behandling.opprettetTidspunkt.atZone(TIMEZONE))
            assertThat(behandlingStatistikkV2Dto.behandlingType).isEqualTo(behandling.type)
            assertThat(behandlingStatistikkV2Dto.utenlandstilsnitt).isEqualTo(behandling.kategori.name)
            assertThat(behandlingStatistikkV2Dto.behandlingStatus).isEqualTo(behandling.status)
            assertThat(behandlingStatistikkV2Dto.behandlingsResultat).isEqualTo(behandling.resultat)
            assertThat(behandlingStatistikkV2Dto.ansvarligEnhet).isEqualTo(arbeidsfordelingPåBehandling.behandlendeEnhetId)
            assertThat(behandlingStatistikkV2Dto.ansvarligBeslutter).isEqualTo(totrinnskontroll.beslutterId)
            assertThat(behandlingStatistikkV2Dto.ansvarligSaksbehandler).isEqualTo(totrinnskontroll.saksbehandlerId)
            assertThat(behandlingStatistikkV2Dto.behandlingErManueltOpprettet).isTrue()
            assertThat(behandlingStatistikkV2Dto.funksjoneltTidspunkt).isEqualTo(behandling.endretTidspunkt.atZone(TIMEZONE))
            assertThat(behandlingStatistikkV2Dto.automatiskBehandlet).isFalse()
            assertThat(behandlingStatistikkV2Dto.sattPaaVent).isNull()
            assertThat(behandlingStatistikkV2Dto.behandlingOpprettetÅrsak).isEqualTo(behandling.opprettetÅrsak)
            assertThat(behandlingStatistikkV2Dto.relatertBehandlingId).isNull()
            assertThat(behandlingStatistikkV2Dto.relatertBehandlingFagsystem).isNull()
        }
    }
}
