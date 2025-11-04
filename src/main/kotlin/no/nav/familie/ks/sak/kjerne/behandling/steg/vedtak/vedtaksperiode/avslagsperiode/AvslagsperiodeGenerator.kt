package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.avslagsperiode

import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.EØSBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse.AVSLAG_UREGISTRERT_BARN
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import org.springframework.stereotype.Component

@Component
class AvslagsperiodeGenerator(
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
) {
    fun genererAvslagsperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = vedtak.behandling
        val avslagsperioderFraVilkårsvurdering = hentAvslagsperioderFraVilkårsvurdering(behandling.id, vedtak)

        val avslagsperioderFraEndretUtbetalinger = hentAvslagsperioderFraEndretUtbetalinger(behandling.id, vedtak)

        val uregistrerteBarn =
            if (behandling.erSøknad()) {
                søknadGrunnlagService.hentAktiv(behandlingId = behandling.id).hentUregistrerteBarn()
            } else {
                emptyList()
            }

        return if (uregistrerteBarn.isNotEmpty()) {
            leggTilAvslagsbegrunnelseForUregistrertBarn(
                avslagsperioder = avslagsperioderFraVilkårsvurdering + avslagsperioderFraEndretUtbetalinger,
                vedtak = vedtak,
                uregistrerteBarn = uregistrerteBarn,
            )
        } else {
            avslagsperioderFraVilkårsvurdering + avslagsperioderFraEndretUtbetalinger
        }
    }

    private fun hentAvslagsperioderFraEndretUtbetalinger(
        behandlingId: Long,
        vedtak: Vedtak,
    ): List<VedtaksperiodeMedBegrunnelser> {
        val endreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                behandlingId,
            )

        val periodegrupperteAvslagEndreteUtbetalinger =
            endreteUtbetalinger
                .filter { it.erEksplisittAvslagPåSøknad == true }
                .filterNot { it.erEksplisittAvslagPåSøknad == true && it.årsak == Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 }
                .groupBy { NullablePeriode(it.fom?.toLocalDate(), it.tom?.toLocalDate()) }

        val avslagsperioder =
            periodegrupperteAvslagEndreteUtbetalinger
                .map { (fellesPeriode, endretUtbetalinger) ->

                    val avslagsbegrunnelser =
                        endretUtbetalinger
                            .map { it.vedtaksbegrunnelser }
                            .flatten()
                            .toSet()
                            .toList()

                    lagVedtaksPeriodeMedBegrunnelser(vedtak, fellesPeriode, avslagsbegrunnelser)
                }.toMutableList()

        return avslagsperioder
    }

    private fun hentAvslagsperioderFraVilkårsvurdering(
        behandlingId: Long,
        vedtak: Vedtak,
    ): MutableList<VedtaksperiodeMedBegrunnelser> {
        val vilkårsvurdering =
            vilkårsvurderingRepository.finnAktivForBehandling(behandlingId = behandlingId) ?: throw Feil(
                "Fant ikke vilkårsvurdering for behandling $behandlingId ved generering av avslagsperioder",
            )

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId,
            )

        val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
            vilkårsvurdering.personResultater
                .flatMap { it.vilkårResultater }
                .filter { it.erEksplisittAvslagPåSøknad == true }
                .groupBy {
                    val finnesAndelForPersonSomSlutterSammeMånedSomFomIVilkårResultat =
                        andelerTilkjentYtelse.any { andel ->
                            andel.aktør == it.personResultat?.aktør && andel.stønadTom == it.periodeFom?.toYearMonth()
                        }
                    if (finnesAndelForPersonSomSlutterSammeMånedSomFomIVilkårResultat) {
                        NullablePeriode(it.periodeFom?.plusMonths(1), it.periodeTom)
                    } else {
                        NullablePeriode(it.periodeFom, it.periodeTom)
                    }
                }

        val avslagsperioder =
            periodegrupperteAvslagsvilkår
                .map { (fellesPeriode, vilkårResultater) ->

                    val avslagsbegrunnelser =
                        vilkårResultater
                            .map { it.begrunnelser }
                            .flatten()
                            .toSet()
                            .toList()

                    lagVedtaksPeriodeMedBegrunnelser(vedtak, fellesPeriode, avslagsbegrunnelser)
                }.toMutableList()

        return avslagsperioder
    }

    private fun lagVedtaksPeriodeMedBegrunnelser(
        vedtak: Vedtak,
        periode: NullablePeriode,
        avslagsbegrunnelser: List<IBegrunnelse>,
    ): VedtaksperiodeMedBegrunnelser =
        VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = periode.fom?.førsteDagIInneværendeMåned(),
            tom = periode.tom?.sisteDagIMåned(),
            type = Vedtaksperiodetype.AVSLAG,
        ).apply {
            begrunnelser.addAll(
                avslagsbegrunnelser.filterIsInstance<NasjonalEllerFellesBegrunnelse>().map { begrunnelse ->
                    NasjonalEllerFellesBegrunnelseDB(
                        vedtaksperiodeMedBegrunnelser = this,
                        nasjonalEllerFellesBegrunnelse = begrunnelse,
                    )
                },
            )

            eøsBegrunnelser.addAll(
                avslagsbegrunnelser.filterIsInstance<EØSBegrunnelse>().map { eøsBegrunnelse ->
                    EØSBegrunnelseDB(vedtaksperiodeMedBegrunnelser = this, begrunnelse = eøsBegrunnelse)
                },
            )
            vedtaksperiodeHentOgPersisterService.lagre(this)
        }

    private fun leggTilAvslagsbegrunnelseForUregistrertBarn(
        avslagsperioder: List<VedtaksperiodeMedBegrunnelser>,
        vedtak: Vedtak,
        uregistrerteBarn: List<BarnMedOpplysningerDto>,
    ): List<VedtaksperiodeMedBegrunnelser> {
        val avslagsperioderMedTomPeriode =
            if (avslagsperioder.none { it.fom == null && it.tom == null }) {
                avslagsperioder +
                    VedtaksperiodeMedBegrunnelser(
                        vedtak = vedtak,
                        fom = null,
                        tom = null,
                        type = Vedtaksperiodetype.AVSLAG,
                    )
            } else {
                avslagsperioder
            }

        return avslagsperioderMedTomPeriode
            .map {
                if (it.fom == null && it.tom == null && uregistrerteBarn.isNotEmpty()) {
                    it.apply {
                        begrunnelser.add(
                            NasjonalEllerFellesBegrunnelseDB(
                                vedtaksperiodeMedBegrunnelser = this,
                                nasjonalEllerFellesBegrunnelse = AVSLAG_UREGISTRERT_BARN,
                            ),
                        )
                    }
                } else {
                    it
                }
            }.toList()
    }
}
