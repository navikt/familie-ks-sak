
package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
    val sanityService: SanityService,
    val vilkårsvurderingRepository: VilkårsvurderingRepository,
    val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    val overgangsordningAndelService: OvergangsordningAndelService,
    val søknadGrunnlagService: SøknadGrunnlagService,
    val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    val vedtaksperiodeService: VedtaksperiodeService,
    val kompetanseService: KompetanseService,
    val integrasjonClient: IntegrasjonClient,
    private val adopsjonService: AdopsjonService,
) {
    fun hentBegrunnelsesteksterForPeriode(vedtaksperiodeId: Long): List<BegrunnelseDto> {
        val behandlingId =
            vedtaksperiodeHentOgPersisterService
                .hentVedtaksperiodeThrows(vedtaksperiodeId)
                .vedtak.behandling.id

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
                ?: throw Feil("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val andelTilkjentYtelserMedEndreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId,
            )
        val kompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(behandlingId))

        return utvidetVedtaksperioderMedBegrunnelser
            .sortedBy { it.fom }
            .mapNotNull { utvidetVedtaksperiodeMedBegrunnelser ->
                val erFørsteVedtaksperiodePåFagsak =
                    !andelTilkjentYtelserMedEndreteUtbetalinger.map { it.andel }.any {
                        it.stønadFom.isBefore(
                            utvidetVedtaksperiodeMedBegrunnelser.fom?.toYearMonth() ?: TIDENES_MORGEN.toYearMonth(),
                        )
                    }

                BrevPeriodeContext(
                    utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                    sanityBegrunnelser = sanityBegrunnelser,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    personResultater = vilkårsvurdering.personResultater.toList(),
                    andelTilkjentYtelserMedEndreteUtbetalinger = andelTilkjentYtelserMedEndreteUtbetalinger,
                    uregistrerteBarn =
                        søknadGrunnlagService.finnAktiv(behandlingId)?.hentUregistrerteBarn()
                            ?: emptyList(),
                    kompetanser = kompetanser.map { it.tilIKompetanse() }.filterIsInstance<UtfyltKompetanse>(),
                    landkoder = integrasjonClient.hentLandkoderISO2(),
                    erFørsteVedtaksperiode = erFørsteVedtaksperiodePåFagsak,
                    overgangsordningAndeler = overgangsordningAndelService.hentOvergangsordningAndeler(behandlingId),
                    adopsjonerIBehandling = adopsjonService.hentAlleAdopsjonerForBehandling(BehandlingId(behandlingId)),
                ).genererBrevPeriodeDto()
            }
    }

    fun beskrivPerioderMedUavklartRefusjonEøs(vedtak: Vedtak) =
        vedtaksperiodeService
            .beskrivPerioderMedRefusjonEøs(behandling = vedtak.behandling, avklart = false)
            ?.let { RefusjonEøsUavklart(perioderMedRefusjonEøsUavklart = it) }

    fun beskrivPerioderMedAvklartRefusjonEøs(vedtak: Vedtak) =
        vedtaksperiodeService
            .beskrivPerioderMedRefusjonEøs(behandling = vedtak.behandling, avklart = true)
            ?.let { RefusjonEøsAvklart(perioderMedRefusjonEøsAvklart = it) }
}
