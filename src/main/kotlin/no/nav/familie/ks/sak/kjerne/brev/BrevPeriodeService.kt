package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.dødeBarnForrigePeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
    val sanityService: SanityService,
    val vilkårsvurderingRepository: VilkårsvurderingRepository,
    val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    val søknadGrunnlagService: SøknadGrunnlagService,
    val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    val vedtaksperiodeService: VedtaksperiodeService,
    val kompetanseService: KompetanseService,
) {
    fun hentBegrunnelsesteksterForPeriode(vedtaksperiodeId: Long): List<BegrunnelseDto> {
        val behandlingId = vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId).vedtak.behandling.id

        val utvidetVedtaksperiodeMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtaksperiodeId)

        return hentBrevPeriodeDtoer(listOf(utvidetVedtaksperiodeMedBegrunnelser), behandlingId).singleOrNull()?.begrunnelser
            ?: emptyList()
    }

    fun hentBrevPeriodeDtoer(
        utvidetVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        behandlingId: Long,
    ): List<BrevPeriodeDto> {
        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val vilkårsvurdering =
            vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)
                ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val andelTilkjentYtelserMedEndreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId,
            )
        val kompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(behandlingId))

        return utvidetVedtaksperioderMedBegrunnelser
            .sortedBy { it.fom }
            .mapNotNull { utvidetVedtaksperiodeMedBegrunnelser ->
                val barnSomDødeIForrigePeriode =
                    dødeBarnForrigePeriode(
                        ytelserForrigePeriode =
                            andelTilkjentYtelserMedEndreteUtbetalinger.map { it.andel }
                                .filter { ytelseErFraForrigePeriode(it, utvidetVedtaksperiodeMedBegrunnelser) },
                        barnIBehandling = personopplysningGrunnlag.personer.filter { it.type == PersonType.BARN },
                    )

                val erFørsteVedtaksperiodePåFagsak =
                    !andelTilkjentYtelserMedEndreteUtbetalinger.map { it.andel }.any {
                        it.stønadFom.isBefore(
                            utvidetVedtaksperiodeMedBegrunnelser.fom?.toYearMonth() ?: TIDENES_MORGEN.toYearMonth(),
                        )
                    }

                BrevPeriodeContext(
                    utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                    sanityBegrunnelser = sanityBegrunnelser,
                    persongrunnlag = personopplysningGrunnlag,
                    personResultater = vilkårsvurdering.personResultater.toList(),
                    andelTilkjentYtelserMedEndreteUtbetalinger = andelTilkjentYtelserMedEndreteUtbetalinger,
                    uregistrerteBarn =
                        søknadGrunnlagService.finnAktiv(behandlingId)?.hentUregistrerteBarn()
                            ?: emptyList(),
                    barnSomDødeIForrigePeriode = barnSomDødeIForrigePeriode,
                    erFørsteVedtaksperiode = erFørsteVedtaksperiodePåFagsak,
                    kompetanser = kompetanser,
                ).genererBrevPeriodeDto()
            }
    }

    fun beskrivPerioderMedUavklartRefusjonEøs(vedtak: Vedtak) =
        vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = vedtak.behandling, avklart = false)
            ?.let { RefusjonEøsUavklart(perioderMedRefusjonEøsUavklart = it) }

    fun beskrivPerioderMedAvklartRefusjonEøs(vedtak: Vedtak) =
        vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = vedtak.behandling, avklart = true)
            ?.let { RefusjonEøsAvklart(perioderMedRefusjonEøsAvklart = it) }
}

fun ytelseErFraForrigePeriode(
    ytelse: AndelTilkjentYtelse,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
) = ytelse.stønadTom.sisteDagIInneværendeMåned().erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom)
