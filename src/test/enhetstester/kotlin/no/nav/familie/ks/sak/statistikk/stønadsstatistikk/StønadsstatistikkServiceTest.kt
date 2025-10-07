package no.nav.familie.ks.sak.statistikk.stønadsstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.eksterne.kontrakter.KompetanseAktivitet
import no.nav.familie.eksterne.kontrakter.Vilkår
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.OVERGANGSORDNING_UTBETALINGSMÅNED
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class StønadsstatistikkServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingService>(relaxed = true)
    private val kompetanseService = mockk<KompetanseService>()
    private val persongrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val vedtakService = mockk<VedtakService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()

    private val stønadsstatistikkService =
        StønadsstatistikkService(
            behandlingService = behandlingHentOgPersisterService,
            kompetanseService = kompetanseService,
            personopplysningGrunnlagService = persongrunnlagService,
            personOpplysningerService = personopplysningerService,
            vedtakService = vedtakService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            vilkårsvurderingService = vilkårsvurderingService,
        )

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val søkerFnr = behandling.fagsak.aktør.aktivFødselsnummer()
    private val barnFnr = randomFnr()
    private val barn2Fnr = randomFnr()

    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr, barn2Fnr),
            søkerAktør = behandling.fagsak.aktør,
        ).also {
            it.personer.forEach { it.bostedsadresser = mutableListOf(mockk(), mockk()) }
        }
    private val barn1 = personopplysningGrunnlag.barna.first()
    private val barn2 = personopplysningGrunnlag.barna.last()

    private val vilkårsVurdering =
        lagVilkårsvurderingOppfylt(
            personopplysningGrunnlag.personer,
            behandling,
            false,
        )

    @Test
    fun `hentVedtakDVH skal kaste feil dersom vedtak ikke har noe dato satt`() {
        // Arrange
        val vedtak = Vedtak(behandling = behandling)

        every { behandlingHentOgPersisterService.hentBehandling(any()) } returns behandling
        every { persongrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            listOf(mockk())
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns vilkårsVurdering

        // Act
        val exception =
            assertThrows<Feil> {
                stønadsstatistikkService.hentVedtakDVH(1L)
            }

        // Assert
        assertThat("Fant ikke vedtaksdato for behandling 1").isEqualTo(exception.message)
    }

    @Test
    fun `hentVedtakDVH skal hente og generere VedtakDVH med riktige detaljer om utbetalinger og at vilkårsresultater foreligger`() {
        // Arrange
        val vedtak = Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now())

        val andelTilkjentYtelseBarn1 =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn1.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn1.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 1054,
                    aktør = barn1.aktør,
                    periodeOffset = 1,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseBarn2PeriodeMed0Beløp =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn2.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn2.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 0,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseSøker =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn2.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn2.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 50,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelerTilkjentYtelse =
            listOf(
                andelTilkjentYtelseBarn1,
                andelTilkjentYtelseBarn2PeriodeMed0Beløp,
                andelTilkjentYtelseSøker,
            )

        every { behandlingHentOgPersisterService.hentBehandling(any()) } returns behandling
        every { persongrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            andelerTilkjentYtelse
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns vilkårsVurdering
        every { persongrunnlagService.hentBarna(any()) } returns personopplysningGrunnlag.barna
        every { kompetanseService.hentKompetanser(any()) } returns emptyList()

        // Act
        val vedtakDvh = stønadsstatistikkService.hentVedtakDVH(1L)

        // Assert
        assertThat(vedtakDvh.utbetalingsperioder[0].utbetalingsDetaljer.size).isEqualTo(2)

        vedtakDvh.utbetalingsperioder
            .flatMap { it.utbetalingsDetaljer.map { ud -> ud.person } }
            .filter { it.personIdent != søkerFnr }
            .forEach {
                assertThat(it.delingsprosentYtelse).isEqualTo(0)
            }

        assertThat(vedtakDvh.vilkårResultater?.isNotEmpty())

        assertThat(vedtakDvh.vilkårResultater!!.any { it.vilkårType == Vilkår.BARNEHAGEPLASS })
    }

    @Test
    fun `hentVedtakDVH skal hente og generere VedtakDVH med riktige utbetalingsperioder når det er overgangsandeler`() {
        // Arrange
        val vedtak = Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now())

        val andelTilkjentYtelseBarn1 =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = OVERGANGSORDNING_UTBETALINGSMÅNED.førsteDagIInneværendeMåned().toYearMonth().minusMonths(1),
                    stønadTom = OVERGANGSORDNING_UTBETALINGSMÅNED.sisteDagIInneværendeMåned().toYearMonth(),
                    sats = 7500,
                    aktør = barn1.aktør,
                    periodeOffset = 1,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseBarn1Periode2 =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = OVERGANGSORDNING_UTBETALINGSMÅNED.førsteDagIInneværendeMåned().toYearMonth().plusMonths(1),
                    stønadTom = OVERGANGSORDNING_UTBETALINGSMÅNED.sisteDagIInneværendeMåned().toYearMonth().plusMonths(1),
                    sats = 3750,
                    aktør = barn1.aktør,
                    periodeOffset = 1,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseBarn2 =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = OVERGANGSORDNING_UTBETALINGSMÅNED.førsteDagIInneværendeMåned().toYearMonth().minusMonths(4),
                    stønadTom = OVERGANGSORDNING_UTBETALINGSMÅNED.sisteDagIInneværendeMåned().toYearMonth().plusMonths(2),
                    sats = 1054,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseSøker =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn2.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn2.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 50,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelerTilkjentYtelse =
            listOf(
                andelTilkjentYtelseBarn1,
                andelTilkjentYtelseBarn1Periode2,
                andelTilkjentYtelseBarn2,
                andelTilkjentYtelseSøker,
            )

        every { behandlingHentOgPersisterService.hentBehandling(any()) } returns behandling
        every { persongrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            andelerTilkjentYtelse
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns vilkårsVurdering
        every { persongrunnlagService.hentBarna(any()) } returns personopplysningGrunnlag.barna
        every { kompetanseService.hentKompetanser(any()) } returns emptyList()

        // Act
        val vedtakDvh = stønadsstatistikkService.hentVedtakDVH(1L)

        // Assert
        val utbetalingsperioderAugTilFeb =
            vedtakDvh.utbetalingsperioder.filter {
                it.stønadFom in LocalDate.of(2024, 8, 1)..LocalDate.of(2025, 2, 28) &&
                    it.stønadTom in LocalDate.of(2024, 8, 1)..LocalDate.of(2025, 2, 28)
            }

        assertThat(utbetalingsperioderAugTilFeb.size).isEqualTo(4)

        val utbetalingerForBarn1IAugTilFeb =
            utbetalingsperioderAugTilFeb.filter {
                it.utbetalingsDetaljer.any { it.person.personIdent == barnFnr }
            }
        assertThat(utbetalingerForBarn1IAugTilFeb.size).isEqualTo(2)

        val overgangsordningDetaljer = utbetalingerForBarn1IAugTilFeb.flatMap { it.utbetalingsDetaljer }.filter { it.person.personIdent == barnFnr }
        assertThat(overgangsordningDetaljer.map { it.klassekode }.toSet().singleOrNull()).isEqualTo(YtelseType.OVERGANGSORDNING.klassifisering)
    }

    @Test
    fun `hentVedtakDVH skal hente og generere VedtakDVH med riktige kompetanser for eøs perioder`() {
        // Arrange
        val vedtak = Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now())

        val andelTilkjentYtelseBarn1 =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn1.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn1.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 1054,
                    aktør = barn1.aktør,
                    periodeOffset = 1,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseBarn2PeriodeMed0Beløp =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn2.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn2.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 0,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseSøker =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn2.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn2.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 50,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelerTilkjentYtelse =
            listOf(
                andelTilkjentYtelseBarn1,
                andelTilkjentYtelseBarn2PeriodeMed0Beløp,
                andelTilkjentYtelseSøker,
            )

        every { behandlingHentOgPersisterService.hentBehandling(any()) } returns behandling
        every { persongrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            andelerTilkjentYtelse
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns vilkårsVurdering
        every { persongrunnlagService.hentBarna(any()) } returns personopplysningGrunnlag.barna
        every { kompetanseService.hentKompetanser(any()) } returns
            listOf(
                Kompetanse(
                    fom = YearMonth.now(),
                    tom = null,
                    barnAktører = setOf(barn1.aktør),
                    søkersAktivitet = no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet.ARBEIDER,
                    annenForeldersAktivitet = no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet.ARBEIDER,
                    annenForeldersAktivitetsland = "DK",
                    søkersAktivitetsland = "DK",
                    barnetsBostedsland = "DK",
                    resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                ),
            )

        // Act
        val vedtakDvh = stønadsstatistikkService.hentVedtakDVH(1L)

        // Assert
        assertThat(vedtakDvh.kompetanseperioder?.isNotEmpty())

        assertThat(vedtakDvh.kompetanseperioder!!.any { it.kompetanseAktivitet == KompetanseAktivitet.ARBEIDER })
    }
}
