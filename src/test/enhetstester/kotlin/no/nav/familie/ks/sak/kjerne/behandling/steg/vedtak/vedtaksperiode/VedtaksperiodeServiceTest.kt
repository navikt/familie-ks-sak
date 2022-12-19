package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser.UtbetalingsperiodeMedBegrunnelserService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class VedtaksperiodeServiceTest {

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @MockK
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @MockK
    private lateinit var sanityService: SanityService

    @MockK
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @MockK
    private lateinit var utbetalingsperiodeMedBegrunnelserService: UtbetalingsperiodeMedBegrunnelserService

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @InjectMockKs
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        behandling = lagBehandling()
    }

    @Test
    fun `finnEndringstidspunktForBehandling finner endringstidspunkt for førstegangsbehandling`() {
        assertEquals(TIDENES_MORGEN, vedtaksperiodeService.finnEndringstidspunktForBehandling(behandling, null))
    }

    @Test
    fun `finnEndringstidspunktForBehandling finner endringstidspunkt for revurdering`() {
        val aktør = randomAktør()
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            tilkjentYtelse = lagInitieltTilkjentYtelse(behandling),
            behandling = behandling,
            aktør = aktør,
            stønadFom = YearMonth.now().minusMonths(5),
            stønadTom = YearMonth.now().plusMonths(4),
            sats = 7500
        )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse, emptyList()))

        val revurdering = lagBehandling()
        val andelTilkjentYtelseForRevurdering = lagAndelTilkjentYtelse(
            tilkjentYtelse = lagInitieltTilkjentYtelse(revurdering),
            behandling = revurdering,
            aktør = aktør,
            stønadFom = YearMonth.now().minusMonths(3),
            stønadTom = YearMonth.now().plusMonths(4),
            sats = 7500
        )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(revurdering.id)
        } returns listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelseForRevurdering, emptyList()))

        // Siden periode med fom=YearMonth.now().minusMonths(5), tom=YearMonth.now().minusMonths(4) er opphørt nå
        assertEquals(
            YearMonth.now().minusMonths(5).førsteDagIInneværendeMåned(),
            vedtaksperiodeService.finnEndringstidspunktForBehandling(
                behandling = revurdering,
                sisteVedtattBehandling = behandling
            )
        )
    }

    @Test
    fun `finnEndringstidspunktForBehandling finner første endringstidspunkt for revurdering med flere perioder`() {
        val aktør = randomAktør()
        val periode1 = MånedPeriode(YearMonth.now().minusMonths(5), YearMonth.now().minusMonths(3))
        val periode2 = MånedPeriode(YearMonth.now().minusMonths(1), YearMonth.now().plusMonths(4))
        val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(
            tilkjentYtelse = lagInitieltTilkjentYtelse(behandling),
            behandling = behandling,
            aktør = aktør,
            stønadFom = periode1.fom,
            stønadTom = periode1.tom,
            sats = 7500
        )
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(
            tilkjentYtelse = lagInitieltTilkjentYtelse(behandling),
            behandling = behandling,
            aktør = aktør,
            stønadFom = periode2.fom,
            stønadTom = periode2.tom,
            sats = 7500
        )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns listOf(
            AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse1, emptyList()),
            AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse2, emptyList())
        )

        val revurdering = lagBehandling()
        val andelTilkjentYtelseForRevurdering1 = lagAndelTilkjentYtelse(
            tilkjentYtelse = lagInitieltTilkjentYtelse(revurdering),
            behandling = revurdering,
            aktør = aktør,
            stønadFom = periode1.fom,
            stønadTom = periode1.tom,
            sats = 7500
        )
        val andelTilkjentYtelseForRevurdering2 = lagAndelTilkjentYtelse(
            tilkjentYtelse = lagInitieltTilkjentYtelse(revurdering),
            behandling = revurdering,
            aktør = aktør,
            stønadFom = periode2.fom,
            stønadTom = periode2.tom,
            sats = 3500
        )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(revurdering.id)
        } returns listOf(
            AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelseForRevurdering1, emptyList()),
            AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelseForRevurdering2, emptyList())
        )

        // endring i beløp på revurdering for periode2
        assertEquals(
            periode2.fom.førsteDagIInneværendeMåned(),
            vedtaksperiodeService.finnEndringstidspunktForBehandling(
                behandling = revurdering,
                sisteVedtattBehandling = behandling
            )
        )
    }
}
