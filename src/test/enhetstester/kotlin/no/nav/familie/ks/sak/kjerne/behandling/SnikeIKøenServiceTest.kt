package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBehandlingStegTilstand
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagLogg
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SnikeIKøenServiceTest {
    private val clock = Clock.fixed(Instant.parse("2024-01-01T18:00:00.00Z"), ZoneId.of("Europe/Oslo"))
    private val behandlingService: BehandlingService = mockk()
    private val loggService: LoggService = mockk(relaxed = true)
    private val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk(relaxed = true)

    private val snikeIKøenService: SnikeIKøenService =
        SnikeIKøenService(
            behandlingService = behandlingService,
            loggService = loggService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
            clock,
        )

    @Nested
    inner class SettAktivBehandlingPåMaskinellVentTest {
        @Test
        fun `skal kaste exception hvis behandling ikke er aktiv`() {
            // Arrange
            val behandling = lagBehandling(id = 1L, aktiv = false)

            every { behandlingService.hentBehandling(1L) }.returns(behandling)

            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                        1L,
                        SettPåMaskinellVentÅrsak.SATSENDRING,
                    )
                }
            assertThat(exception.message).isEqualTo("Behandling=${behandling.id} er ikke aktiv")
        }

        @Test
        fun `skal kaste exception hvis behandlingsstatusen ikke er UTREDES og behandlingen ikke er på vent`() {
            // Arrange
            val behandling = lagBehandling(
                id = 1L,
                status = BehandlingStatus.OPPRETTET
            )
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.REGISTRERE_PERSONGRUNNLAG,
                BehandlingStegStatus.KLAR,
            )

            every { behandlingService.hentBehandling(1L) }.returns(behandling)

            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                        1L,
                        SettPåMaskinellVentÅrsak.SATSENDRING,
                    )
                }
            assertThat(exception.message).isEqualTo("Behandling=${behandling.id} kan ikke settes på maskinell vent då status=${behandling.status}")
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingStegStatus::class)
        fun `skal sette behandling på maskinell vent uansett behandlingStegStatus når BehandlingStatus er UTREDES`(
            behandlingStegStatus: BehandlingStegStatus
        ) {
            // Arrange
            val behandling = lagBehandling(status = BehandlingStatus.UTREDES)
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.REGISTRERE_SØKNAD,
                behandlingStegStatus,
            )

            val årsak = SettPåMaskinellVentÅrsak.SATSENDRING

            every { behandlingService.hentBehandling(1L) }.returns(behandling)
            every { behandlingService.oppdaterBehandling(behandling) }.returns(behandling)
            every { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) } just runs

            // Act
            snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                1L,
                årsak,
            )

            // Assert
            assertThat(behandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
            assertThat(behandling.aktiv).isFalse()
            verify(exactly = 1) { behandlingService.oppdaterBehandling(behandling) }
            verify(exactly = 1) { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) }
        }


        @ParameterizedTest
        @EnumSource(value = BehandlingStatus::class)
        fun `skal sette behandling på maskinell vent uansett BehandlingStatus når behandlingStegStatus er VENTER`(
            behandlingStatus: BehandlingStatus
        ) {
            // Arrange
            val behandling = lagBehandling(status = behandlingStatus)
            behandling.behandlingStegTilstand.clear()
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.AVSLUTT_BEHANDLING,
                BehandlingStegStatus.VENTER,
            )

            val årsak = SettPåMaskinellVentÅrsak.SATSENDRING

            every { behandlingService.hentBehandling(1L) }.returns(behandling)
            every { behandlingService.oppdaterBehandling(behandling) }.returns(behandling)
            every { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) } just runs

            // Act
            snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                1L,
                årsak,
            )

            // Assert
            assertThat(behandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
            assertThat(behandling.aktiv).isFalse()
            verify(exactly = 1) { behandlingService.oppdaterBehandling(behandling) }
            verify(exactly = 1) { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) }
        }

    }

    @Nested
    inner class ReaktiverBehandlingPåMaskinellVentTest {

        @Test
        fun `skal ikke reaktivere behandling når ingen behandling på maskinell vent finnes`() {

            // Arrange
            val fagsak = lagFagsak(
                id = 1L
            )

            val behandling = lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.IVERKSETTER_VEDTAK,
            )

            every {
                behandlingService.hentBehandlingerPåFagsak(fagsak.id)
            }.returns(listOf(behandling))

            // Act
            val erReaktivert = snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling)

            // Assert
            assertThat(erReaktivert).isEqualTo(Reaktivert.NEI)

        }

        @Test
        fun `skal kaste exception hvis flere behandlinger står på maskinell vent`() {

            // Arrange
            val fagsak = lagFagsak(
                id = 1L
            )

            val behandlingSomHarSneketIKøen = lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.IVERKSETTER_VEDTAK,
            )

            val behandlingPåMaskinellVent1 = lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
            )

            val behandlingPåMaskinellVent2 = lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
            )

            every {
                behandlingService.hentBehandlingerPåFagsak(fagsak.id)
            }.returns(
                listOf(
                    behandlingSomHarSneketIKøen,
                    behandlingPåMaskinellVent1,
                    behandlingPåMaskinellVent2
                )
            )

            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomHarSneketIKøen)
            }
            assertThat(exception.message).isEqualTo("Forventer kun en behandling på maskinell vent for fagsak=${fagsak.id}")

        }

        @Test
        fun `skal feile når behandling på maskinell vent er aktiv`() {

            // Arrange
            val fagsak = lagFagsak(
                id = 1L
            )

            val behandlingPåVent = lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
                aktiv = true
            )

            val behandlingSomSnekIKøen = lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.AVSLUTTET,
                aktiv = false
            )

            every { behandlingService.hentBehandlingerPåFagsak(fagsak.id) }
                .returns(
                    listOf(
                        behandlingSomSnekIKøen,
                        behandlingPåVent
                    )
                )

            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen)
            }
            assertThat(exception.message).isEqualTo("Behandling på vent er aktiv")

        }

        @Test
        fun `skal feile når aktiv behandling ikke er avsluttet`() {

            // Arrange
            val fagsak = lagFagsak(
                id = 1L
            )

            val behandlingPåVent = lagBehandling(
                id = 1L,
                fagsak = fagsak,
                status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
                aktiv = false
            )

            val behandlingSomSnekIKøen = lagBehandling(
                id = 2L,
                fagsak = fagsak,
                status = BehandlingStatus.IVERKSETTER_VEDTAK,
                aktiv = true
            )

            every { behandlingService.hentBehandlingerPåFagsak(fagsak.id) }
                .returns(
                    listOf(
                        behandlingSomSnekIKøen,
                        behandlingPåVent
                    )
                )

            // Act & assert
            val exception = assertThrows<BehandlingErIkkeAvsluttetException> {
                snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen)
            }
            assertThat(exception.message).isEqualTo(
                "Behandling=${behandlingSomSnekIKøen.id} har status=${behandlingSomSnekIKøen.status} og er ikke avsluttet"
            )
        }

        @Test
        fun `skal reaktivere maskinell behandling og deaktivere aktiv behandling`() {

            // Arrange
            val fagsak = lagFagsak(
                id = 1L
            )

            val behandlingPåVent = lagBehandling(
                id = 1L,
                fagsak = fagsak,
                status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
                aktiv = false
            )

            val behandlingSomSnekIKøen = lagBehandling(
                id = 2L,
                fagsak = fagsak,
                status = BehandlingStatus.AVSLUTTET,
                aktiv = true
            )

            every { behandlingService.hentBehandlingerPåFagsak(fagsak.id) }
                .returns(
                    listOf(
                        behandlingSomSnekIKøen,
                        behandlingPåVent
                    )
                )

            every { behandlingService.oppdaterBehandling(behandlingSomSnekIKøen) }
                .returns(behandlingSomSnekIKøen)

            every { behandlingService.oppdaterBehandling(behandlingPåVent) }
                .returns(behandlingPåVent)

            // Act
            val reaktivert = snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen)

            // Assert
            assertThat(behandlingSomSnekIKøen.aktiv).isFalse();
            assertThat(behandlingPåVent.aktiv).isTrue()
            assertThat(behandlingPåVent.aktivertTidspunkt).isNotNull()
            assertThat(behandlingPåVent.status).isEqualTo(BehandlingStatus.UTREDES)
            verify(exactly = 1) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingPåVent.id) }
            verify(exactly = 1) { loggService.opprettTattAvMaskinellVent(behandlingPåVent) }
            assertThat(reaktivert).isEqualTo(Reaktivert.JA)

        }

        @Test
        fun `skal reaktivere maskinell behandling og ikke deaktivere noen behandlinger da ingen behandlinger er aktiv`() {

            // Arrange
            val fagsak = lagFagsak(
                id = 1L
            )

            val behandlingPåVent = lagBehandling(
                id = 1L,
                fagsak = fagsak,
                status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
                aktiv = false
            )

            val behandlingSomSnekIKøen = lagBehandling(
                id = 2L,
                fagsak = fagsak,
                status = BehandlingStatus.AVSLUTTET,
                aktiv = false
            )

            every { behandlingService.hentBehandlingerPåFagsak(fagsak.id) }
                .returns(
                    listOf(
                        behandlingSomSnekIKøen,
                        behandlingPåVent
                    )
                )

            every { behandlingService.oppdaterBehandling(behandlingSomSnekIKøen) }
                .returns(behandlingSomSnekIKøen)

            every { behandlingService.oppdaterBehandling(behandlingPåVent) }
                .returns(behandlingPåVent)

            // Act
            val reaktivert = snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen)

            // Assert
            assertThat(behandlingSomSnekIKøen.aktiv).isFalse();
            assertThat(behandlingPåVent.aktiv).isTrue()
            assertThat(behandlingPåVent.aktivertTidspunkt).isNotNull()
            assertThat(behandlingPåVent.status).isEqualTo(BehandlingStatus.UTREDES)
            verify(exactly = 1) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingPåVent.id) }
            verify(exactly = 1) { loggService.opprettTattAvMaskinellVent(behandlingPåVent) }
            assertThat(reaktivert).isEqualTo(Reaktivert.JA)

        }


        @Test
        fun `skal reaktivere maskinell behandling og sette BehandlingStegTilstand tilbake til VENTER`() {

            // Arrange
            val dagensDato = LocalDate.now()

            val fagsak = lagFagsak(
                id = 1L
            )

            val behandlingPåVent = lagBehandling(
                id = 1L,
                fagsak = fagsak,
                status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
                aktiv = false
            )
            behandlingPåVent.behandlingStegTilstand.clear()
            lagBehandlingStegTilstand(
                behandlingPåVent,
                BehandlingSteg.VILKÅRSVURDERING,
                BehandlingStegStatus.VENTER,
                frist = dagensDato,
                årsak = VenteÅrsak.AVVENTER_BEHANDLING
            )

            val behandlingSomSnekIKøen = lagBehandling(
                id = 2L,
                fagsak = fagsak,
                status = BehandlingStatus.AVSLUTTET,
                aktiv = true
            )

            every { behandlingService.hentBehandlingerPåFagsak(fagsak.id) }
                .returns(
                    listOf(
                        behandlingSomSnekIKøen,
                        behandlingPåVent
                    )
                )

            every { behandlingService.oppdaterBehandling(behandlingSomSnekIKøen) }
                .returns(behandlingSomSnekIKøen)

            every { behandlingService.oppdaterBehandling(behandlingPåVent) }
                .returns(behandlingPåVent)

            // Act
            val reaktivert = snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen)

            // Assert
            assertThat(behandlingSomSnekIKøen.aktiv).isFalse();
            assertThat(behandlingPåVent.aktiv).isTrue()
            assertThat(behandlingPåVent.aktivertTidspunkt).isNotNull()
            assertThat(behandlingPåVent.status).isEqualTo(BehandlingStatus.UTREDES)
            assertThat(behandlingPåVent.behandlingStegTilstand).hasSize(1)
            assertThat(behandlingPåVent.behandlingStegTilstand).anySatisfy {
                assertThat(it.behandlingStegStatus).isEqualTo(BehandlingStegStatus.VENTER)
                assertThat(it.frist).isEqualTo(dagensDato)
                assertThat(it.årsak).isEqualTo(VenteÅrsak.AVVENTER_BEHANDLING)
            }
            verify(exactly = 1) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingPåVent.id) }
            verify(exactly = 1) { loggService.opprettTattAvMaskinellVent(behandlingPåVent) }
            assertThat(reaktivert).isEqualTo(Reaktivert.JA)

        }

    }

    @Nested
    inner class KanSnikeForbiTest {

        @Test
        fun `skal kunne snike i køen om BehandlingStegStatus er VENTER`() {

            // Arrange
            val behandling = lagBehandling()
            behandling.behandlingStegTilstand.clear()
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.VILKÅRSVURDERING,
                BehandlingStegStatus.VENTER,
            )

            // Act
            val kanSnikeForbi = snikeIKøenService.kanSnikeForbi(behandling)

            // Assert
            assertThat(kanSnikeForbi).isTrue()

        }

        @Test
        fun `skal ikke kunne snike i køen om EndretTidspunkt på behandlingen er mindre enn 4 timer siden`() {

            // Arrange
            val endretTidspunkt = LocalDateTime.now(clock).minusHours(3).minusMinutes(59).minusSeconds(59)

            val behandling = lagBehandling(
                endretTidspunkt = endretTidspunkt
            )

            // Act
            val kanSnikeForbi = snikeIKøenService.kanSnikeForbi(behandling)

            // Assert
            assertThat(kanSnikeForbi).isFalse()

        }

        @Test
        fun `skal ikke kunne snike i køen da siste logghendelse er mindre enn 4 timer siden`() {

            // Arrange
            val behandling = lagBehandling(
                endretTidspunkt = LocalDateTime.now(clock).minusHours(4)
            )

            val logg = lagLogg(
                behandlingId = behandling.id,
                opprettetTidspunkt = LocalDateTime.now().minusHours(3).minusMinutes(59).minusSeconds(59)
            )

            every { loggService.hentLoggForBehandling(behandling.id) }
                .returns(listOf(logg))

            // Act
            val kanSnikeForbi = snikeIKøenService.kanSnikeForbi(behandling)

            // Assert
            assertThat(kanSnikeForbi).isFalse()

        }

        @Test
        fun `skal kunne snike i køen om BehandlingStegStatus ikke er VENTER dersom endretTidspunkt og opprettTidspunkt er 4 timer siden eller mer`() {

            // Arrange
            val behandling = lagBehandling(
                endretTidspunkt = LocalDateTime.now(clock).minusHours(4)
            )

            val logg = lagLogg(
                behandlingId = behandling.id,
                opprettetTidspunkt = LocalDateTime.now(clock).minusHours(4)
            )

            every { loggService.hentLoggForBehandling(behandling.id) }
                .returns(listOf(logg))

            // Act
            val kanSnikeForbi = snikeIKøenService.kanSnikeForbi(behandling)

            // Assert
            assertThat(kanSnikeForbi).isTrue()

        }

    }
}
