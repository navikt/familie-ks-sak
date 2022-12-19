package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.dødeBarnForrigePeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
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
    val vedtaksperiodeService: VedtaksperiodeService

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
        behandlingId: Long
    ): List<BrevPeriodeDto> {
        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val andelTilkjentYtelserMedEndreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId
            )

        return utvidetVedtaksperioderMedBegrunnelser
            .sorted()
            .mapNotNull { utvidetVedtaksperiodeMedBegrunnelser ->
                val barnSomDødeIForrigePeriode = dødeBarnForrigePeriode(
                    ytelserForrigePeriode = andelTilkjentYtelserMedEndreteUtbetalinger.map { it.andel }
                        .filter { ytelseErFraForrigePeriode(it, utvidetVedtaksperiodeMedBegrunnelser) },
                    barnIBehandling = personopplysningGrunnlag.personer.filter { it.type == PersonType.BARN }
                )

                BrevPeriodeContext(
                    utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                    sanityBegrunnelser = sanityBegrunnelser,
                    persongrunnlag = personopplysningGrunnlag,
                    personResultater = vilkårsvurdering.personResultater.toList(),
                    andelTilkjentYtelserMedEndreteUtbetalinger = andelTilkjentYtelserMedEndreteUtbetalinger,

                    uregistrerteBarn = søknadGrunnlagService.hentAktiv(behandlingId).hentUregistrerteBarn(),
                    barnSomDødeIForrigePeriode = barnSomDødeIForrigePeriode
                ).genererBrevPeriodeDto()
            }
    }
}

fun ytelseErFraForrigePeriode(
    ytelse: AndelTilkjentYtelse,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser
) = ytelse.stønadTom.sisteDagIInneværendeMåned().erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom)
