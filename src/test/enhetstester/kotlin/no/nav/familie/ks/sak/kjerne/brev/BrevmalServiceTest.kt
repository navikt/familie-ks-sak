package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BrevmalServiceTest {
    private val brevmalService = BrevmalService()

    @Nested
    inner class HentBrevmalTest {
        @Test
        fun `skal utlede brevmal VEDTAK_OPPHØR_DØDSFALL for behandling med behandlingårsak DØDSFALL`() {
            // Arrange
            val behandling =
                lagBehandling(
                    opprettetÅrsak = BehandlingÅrsak.DØDSFALL,
                )

            // Act
            val brevmal =
                brevmalService.hentBrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(Brevmal.VEDTAK_OPPHØR_DØDSFALL)
        }

        @Test
        fun `skal utlede brevmal VEDTAK_OPPHØR_DØDSFALL for behandling med behandlingårsak KORREKSJON_VEDTAKSBREV`() {
            // Arrange
            val behandling =
                lagBehandling(
                    opprettetÅrsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV,
                )

            // Act
            val brevmal =
                brevmalService.hentBrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV)
        }

        @Test
        fun `skal utlede brevmal ENDRING_AV_FRAMTIDIG_OPPHØR for behandling med behandlingårsak LOVENDRING_2024`() {
            // Arrange
            val behandling =
                lagBehandling(
                    opprettetÅrsak = BehandlingÅrsak.LOVENDRING_2024,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            // Act
            val brevmal =
                brevmalService.hentBrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = [
                "DØDSFALL",
                "KORREKSJON_VEDTAKSBREV",
                "LOVENDRING_2024",
                "OVERGANGSORDNING_2024",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede vedtaksbrev for korrekte behandlingsårsaker`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val behandling =
                lagBehandling(
                    opprettetÅrsak = behandlingÅrsak,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            // Act
            val brevmal =
                brevmalService.hentBrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(Brevmal.VEDTAK_FØRSTEGANGSVEDTAK)
        }
    }

    @Nested
    inner class HentVedtaksbrevmalTest {
        val forventetBrevmalForFørstegangsbehandlingBehandlingsresultat =
            mapOf(
                Behandlingsresultat.INNVILGET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.INNVILGET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.INNVILGET_OG_ENDRET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.AVSLÅTT to Brevmal.VEDTAK_AVSLAG,
            )

        val forventetBrevmalForIkkeLøpendeFagsakBehandlingsresultat =
            mapOf(
                Behandlingsresultat.INNVILGET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.INNVILGET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.INNVILGET_OG_ENDRET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                Behandlingsresultat.AVSLÅTT to Brevmal.VEDTAK_AVSLAG,
            )

        val forventetBrevmalForRevurderingBehandlingsresultat =
            mapOf(
                Behandlingsresultat.INNVILGET to Brevmal.VEDTAK_ENDRING,
                Behandlingsresultat.INNVILGET_OG_ENDRET to Brevmal.VEDTAK_ENDRING,
                Behandlingsresultat.DELVIS_INNVILGET to Brevmal.VEDTAK_ENDRING,
                Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET to Brevmal.VEDTAK_ENDRING,
                Behandlingsresultat.AVSLÅTT_OG_ENDRET to Brevmal.VEDTAK_ENDRING,
                Behandlingsresultat.ENDRET_UTBETALING to Brevmal.VEDTAK_ENDRING,
                Behandlingsresultat.ENDRET_UTEN_UTBETALING to Brevmal.VEDTAK_ENDRING,
                Behandlingsresultat.OPPHØRT to Brevmal.VEDTAK_OPPHØRT,
                Behandlingsresultat.FORTSATT_OPPHØRT to Brevmal.VEDTAK_OPPHØRT,
                Behandlingsresultat.INNVILGET_OG_OPPHØRT to Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT to Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                Behandlingsresultat.AVSLÅTT_OG_OPPHØRT to Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                Behandlingsresultat.ENDRET_OG_OPPHØRT to Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                Behandlingsresultat.FORTSATT_INNVILGET to Brevmal.VEDTAK_FORTSATT_INNVILGET,
                Behandlingsresultat.AVSLÅTT to Brevmal.VEDTAK_AVSLAG,
            )

        @Test
        fun `skal kaste exception for behandlinger som ikke er vurdert`() {
            // Arrange
            val behandling =
                lagBehandling(
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmalService.hentVedtaksbrevmal(
                        behandling = behandling,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Kan ikke opprette brev. Behandlingen er ikke vurdert.",
            )
        }

        @Test
        fun `skal returnere ENDRING_AV_FRAMTIDIG_OPPHØR for behandlinger med opprettetÅrsak LOVENDRING_2024`() {
            // Arrange
            val behandling =
                lagBehandling(
                    opprettetÅrsak = BehandlingÅrsak.LOVENDRING_2024,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            // Act
            val brevmal =
                brevmalService.hentVedtaksbrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR)
        }

        @ParameterizedTest
        @EnumSource(
            value = Behandlingsresultat::class,
            names = [
                "AVSLÅTT_OG_OPPHØRT",
                "AVSLÅTT_OG_ENDRET",
                "AVSLÅTT_ENDRET_OG_OPPHØRT",
                "ENDRET_UTBETALING",
                "ENDRET_UTEN_UTBETALING",
                "ENDRET_OG_OPPHØRT",
                "OPPHØRT",
                "FORTSATT_OPPHØRT",
                "FORTSATT_INNVILGET",
                "HENLAGT_FEILAKTIG_OPPRETTET",
                "HENLAGT_SØKNAD_TRUKKET",
                "HENLAGT_TEKNISK_VEDLIKEHOLD",
                "IKKE_VURDERT",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal returnere korrekt brevmal for førstegangsbehandlinger`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    resultat = behandlingsresultat,
                )

            // Act
            val brevmal =
                brevmalService.hentVedtaksbrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(forventetBrevmalForFørstegangsbehandlingBehandlingsresultat[behandlingsresultat])
        }

        @ParameterizedTest
        @EnumSource(
            value = Behandlingsresultat::class,
            names = [
                "AVSLÅTT_OG_OPPHØRT",
                "AVSLÅTT_OG_ENDRET",
                "AVSLÅTT_ENDRET_OG_OPPHØRT",
                "ENDRET_UTBETALING",
                "ENDRET_UTEN_UTBETALING",
                "ENDRET_OG_OPPHØRT",
                "OPPHØRT",
                "FORTSATT_OPPHØRT",
                "FORTSATT_INNVILGET",
                "HENLAGT_FEILAKTIG_OPPRETTET",
                "HENLAGT_SØKNAD_TRUKKET",
                "HENLAGT_TEKNISK_VEDLIKEHOLD",
                "IKKE_VURDERT",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal returnere korrekt brevmal for revurderinger i fagsak som ikke er løpende`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    resultat = behandlingsresultat,
                    fagsak = lagFagsak(status = FagsakStatus.AVSLUTTET),
                )

            // Act
            val brevmal =
                brevmalService.hentVedtaksbrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(forventetBrevmalForIkkeLøpendeFagsakBehandlingsresultat[behandlingsresultat])
        }

        @ParameterizedTest
        @EnumSource(
            value = Behandlingsresultat::class,
            names = [
                "IKKE_VURDERT",
                "INNVILGET",
                "INNVILGET_OG_OPPHØRT",
                "INNVILGET_OG_ENDRET",
                "INNVILGET_ENDRET_OG_OPPHØRT",
                "DELVIS_INNVILGET",
                "DELVIS_INNVILGET_OG_OPPHØRT",
                "DELVIS_INNVILGET_OG_ENDRET",
                "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT",
                "AVSLÅTT",
                "AVSLÅTT_OG_OPPHØRT",
                "AVSLÅTT_OG_ENDRET",
                "AVSLÅTT_ENDRET_OG_OPPHØRT",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal kaste exception for behandlingsresultat som ikke er støttet for førstegangsbehandling`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    resultat = behandlingsresultat,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    brevmalService.hentVedtaksbrevmal(
                        behandling = behandling,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Brev ikke støttet for førstegangsbehandling og behandlingsresultat=$behandlingsresultat",
            )
        }

        @ParameterizedTest
        @EnumSource(
            value = Behandlingsresultat::class,
            names = [
                "HENLAGT_FEILAKTIG_OPPRETTET",
                "HENLAGT_SØKNAD_TRUKKET",
                "HENLAGT_TEKNISK_VEDLIKEHOLD",
                "IKKE_VURDERT",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal returnere korrekt brevmal ved løpende fagsak og revurdering`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    resultat = behandlingsresultat,
                    fagsak = lagFagsak(status = FagsakStatus.LØPENDE),
                )

            // Act
            val brevmal =
                brevmalService.hentVedtaksbrevmal(
                    behandling = behandling,
                )

            // Assert
            assertThat(brevmal).isEqualTo(forventetBrevmalForRevurderingBehandlingsresultat[behandlingsresultat])
        }

        @ParameterizedTest
        @EnumSource(
            value = Behandlingsresultat::class,
            names = [
                "INNVILGET",
                "INNVILGET_OG_ENDRET",
                "DELVIS_INNVILGET",
                "DELVIS_INNVILGET_OG_ENDRET",
                "AVSLÅTT_OG_ENDRET",
                "ENDRET_UTBETALING",
                "ENDRET_UTEN_UTBETALING",
                "OPPHØRT",
                "FORTSATT_OPPHØRT",
                "INNVILGET_OG_OPPHØRT",
                "INNVILGET_ENDRET_OG_OPPHØRT",
                "DELVIS_INNVILGET_OG_OPPHØRT",
                "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT",
                "AVSLÅTT_OG_OPPHØRT",
                "AVSLÅTT_ENDRET_OG_OPPHØRT",
                "ENDRET_OG_OPPHØRT",
                "FORTSATT_INNVILGET",
                "AVSLÅTT",
                "IKKE_VURDERT",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal kaste exception for behandlingsresultat som ikke er støttet for revurdering i løpende fagsak`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    resultat = behandlingsresultat,
                    fagsak = lagFagsak(status = FagsakStatus.LØPENDE),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    brevmalService.hentVedtaksbrevmal(
                        behandling = behandling,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Brev ikke støttet for revurdering og behandlingsresultat=$behandlingsresultat",
            )
        }

        @Test
        fun `skal kaste exception for behandlinger med behandlingtype TEKNISK_ENDRING`() {
            // Arrange
            val behandling =
                lagBehandling(
                    type = BehandlingType.TEKNISK_ENDRING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    resultat = Behandlingsresultat.INNVILGET,
                    fagsak = lagFagsak(status = FagsakStatus.LØPENDE),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmalService.hentVedtaksbrevmal(
                        behandling = behandling,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Kunne ikke utlede hvilket brevmal som skulle benyttes behandling type TEKNISK_ENDRING og årsak SØKNAD",
            )
        }
    }
}
