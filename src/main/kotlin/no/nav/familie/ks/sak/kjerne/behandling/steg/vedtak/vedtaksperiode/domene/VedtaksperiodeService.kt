package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Opphørsperiode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.mapTilOpphørsperioder
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.tilVedtaksperiodeMedBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser.UtbetalingsperiodeMedBegrunnelserService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelserForPeriodeContext
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class VedtaksperiodeService(
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val utbetalingsperiodeMedBegrunnelserService: UtbetalingsperiodeMedBegrunnelserService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {
    fun oppdaterVedtaksperiodeMedFritekster(
        vedtaksperiodeId: Long,
        fritekster: List<String>
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        vedtaksperiodeMedBegrunnelser.settFritekster(
            fritekster.map {
                tilVedtaksbegrunnelseFritekst(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    fritekst = it
                )
            }
        )

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperiodeMedBegrunnelser(
        vedtaksperiodeId: Long,
        begrunnelserFraFrontend: List<Begrunnelse>,
        eøsBegrunnelserFraFrontend: List<EØSBegrunnelse> = emptyList()
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling

        val persongrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            begrunnelserFraFrontend.map {
                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
            }
        )

        if (
            begrunnelserFraFrontend.any { it.begrunnelseType == BegrunnelseType.ENDRET_UTBETALING }
        ) {
            val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

            validerEndretUtbetalingsbegrunnelse(vedtaksperiodeMedBegrunnelser, andelerTilkjentYtelse, persongrunnlag)
        }

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    private fun validerEndretUtbetalingsbegrunnelse(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        persongrunnlag: PersonopplysningGrunnlag
    ) {
        try {
            vedtaksperiodeMedBegrunnelser.hentUtbetalingsperiodeDetaljer(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag
            )
        } catch (e: Exception) {
            throw FunksjonellFeil(
                "Begrunnelse for endret utbetaling er ikke gyldig for vedtaksperioden"
            )
        }
    }

    @Transactional
    fun oppdaterVedtakMedVedtaksperioder(vedtak: Vedtak, skalOverstyreFortsattInnvilget: Boolean = false) {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)
        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET && !skalOverstyreFortsattInnvilget) {
            vedtaksperiodeHentOgPersisterService.lagre(
                VedtaksperiodeMedBegrunnelser(
                    fom = null,
                    tom = null,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET
                )
            )
        } else {
            vedtaksperiodeHentOgPersisterService.lagre(
                genererVedtaksperioderMedBegrunnelser(
                    vedtak,
                    gjelderFortsattInnvilget = skalOverstyreFortsattInnvilget
                )
            )
        }
    }

    fun genererVedtaksperioderMedBegrunnelser(
        vedtak: Vedtak,
        gjelderFortsattInnvilget: Boolean = false,
        manueltOverstyrtEndringstidspunkt: LocalDate? = null
    ): List<VedtaksperiodeMedBegrunnelser> {
        val opphørsperioder =
            hentOpphørsperioder(vedtak.behandling).map { it.tilVedtaksperiodeMedBegrunnelse(vedtak) }

        val utbetalingsperioder =
            utbetalingsperiodeMedBegrunnelserService.hentUtbetalingsperioder(vedtak, opphørsperioder)

        val avslagsperioder = hentAvslagsperioderMedBegrunnelser(vedtak)

        return filtrerUtPerioderBasertPåEndringstidspunkt(
            vedtaksperioderMedBegrunnelser = (utbetalingsperioder + opphørsperioder),
            behandlingId = vedtak.behandling.id,
            gjelderFortsattInnvilget = gjelderFortsattInnvilget,
            manueltOverstyrtEndringstidspunkt = manueltOverstyrtEndringstidspunkt
        ) + avslagsperioder
    }

    fun filtrerUtPerioderBasertPåEndringstidspunkt(
        vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
        behandlingId: Long,
        gjelderFortsattInnvilget: Boolean = false,
        manueltOverstyrtEndringstidspunkt: LocalDate? = null
    ): List<VedtaksperiodeMedBegrunnelser> {
        val endringstidspunkt = manueltOverstyrtEndringstidspunkt
            ?: if (!gjelderFortsattInnvilget) {
                // TODO: Legg til når vi får inn endringstidspunktservice som er avhengig av beregning
                //     endringstidspunktService.finnEndringstidpunkForBehandling(behandlingId = behandlingId)
                TIDENES_MORGEN
            } else {
                TIDENES_MORGEN
            }

        return vedtaksperioderMedBegrunnelser.filter { (it.tom ?: TIDENES_ENDE).erSammeEllerEtter(endringstidspunkt) }
    }

    @Transactional
    fun genererVedtaksperiodeForOverstyrtEndringstidspunkt(
        behandlingId: Long,
        overstyrtEndringstidspunkt: LocalDate
    ) {
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)

        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) {
            oppdaterVedtakMedVedtaksperioder(vedtak)
        } else {
            vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)
            val vedtaksperioder =
                genererVedtaksperioderMedBegrunnelser(
                    vedtak = vedtak,
                    manueltOverstyrtEndringstidspunkt = overstyrtEndringstidspunkt
                )
            vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioder.sortedBy { it.fom })
        }
        lagreNedOverstyrtEndringstidspunkt(vedtak.behandling.id, overstyrtEndringstidspunkt)
    }

    private fun lagreNedOverstyrtEndringstidspunkt(behandlingId: Long, overstyrtEndringstidspunkt: LocalDate) {
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)
        behandling.overstyrtEndringstidspunkt = overstyrtEndringstidspunkt

        behandlingRepository.save(behandling)
    }

    fun kopierOverVedtaksperioder(deaktivertVedtak: Vedtak, aktivtVedtak: Vedtak) {
        val gamleVedtaksperioderMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioderMedBegrunnelser.forEach { vedtaksperiodeMedBegrunnelser ->
            val nyVedtaksperiodeMedBegrunnelser = vedtaksperiodeHentOgPersisterService.lagre(
                VedtaksperiodeMedBegrunnelser(
                    vedtak = aktivtVedtak,
                    fom = vedtaksperiodeMedBegrunnelser.fom,
                    tom = vedtaksperiodeMedBegrunnelser.tom,
                    type = vedtaksperiodeMedBegrunnelser.type
                )
            )

            nyVedtaksperiodeMedBegrunnelser.settBegrunnelser(
                vedtaksperiodeMedBegrunnelser.begrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )
            nyVedtaksperiodeMedBegrunnelser.settFritekster(
                vedtaksperiodeMedBegrunnelser.fritekster.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )

            vedtaksperiodeHentOgPersisterService.lagre(nyVedtaksperiodeMedBegrunnelser)
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        return vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = vedtak.id)
    }

    fun hentUtvidetVedtaksperiodeMedBegrunnelser(vedtaksperiodeId: Long): UtvidetVedtaksperiodeMedBegrunnelser {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId = vedtaksperiodeId)

        val behandlingId = vedtaksperiodeMedBegrunnelser.vedtak.behandling.id

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandlingId)

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId = behandlingId
            )

        return vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag,
            andelerTilkjentYtelse
        )
    }

    fun hentUtvidetVedtaksperioderMedBegrunnelser(vedtak: Vedtak): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)

        val utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser.map {
            it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag
            )
        }

        val skalSendeMedGyldigeBegrunnelser =
            behandling.status == BehandlingStatus.UTREDES && utvidetVedtaksperioderMedBegrunnelser.isNotEmpty()

        return if (skalSendeMedGyldigeBegrunnelser) {
            hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
                behandling = behandling,
                utvidedeVedtaksperioderMedBegrunnelser = utvidetVedtaksperioderMedBegrunnelser,
                persongrunnlag = personopplysningGrunnlag
            )
        } else {
            utvidetVedtaksperioderMedBegrunnelser
        }
    }

    private fun hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
        behandling: Behandling,
        utvidedeVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        persongrunnlag: PersonopplysningGrunnlag
    ): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandling.id)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        return utvidedeVedtaksperioderMedBegrunnelser.map { utvidetVedtaksperiodeMedBegrunnelser ->

            utvidetVedtaksperiodeMedBegrunnelser.copy(
                gyldigeBegrunnelser = BegrunnelserForPeriodeContext(
                    utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                    sanityBegrunnelser = sanityBegrunnelser,
                    personopplysningGrunnlag = persongrunnlag,
                    personResultater = vilkårsvurdering.personResultater.toList()
                ).hentGyldigeBegrunnelserForVedtaksperiode()
            )
        }
    }

    fun hentOpphørsperioder(behandling: Behandling): List<Opphørsperiode> {
        if (behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) return emptyList()

        val iverksatteBehandlinger =
            behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling = iverksatteBehandlinger
            .filter { it.opprettetTidspunkt.isBefore(behandling.opprettetTidspunkt) && it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }
            .maxByOrNull { it.opprettetTidspunkt }

        val forrigePersonopplysningGrunnlag =
            if (forrigeIverksatteBehandling != null) {
                personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId = forrigeIverksatteBehandling.id)
            } else {
                null
            }

        val forrigeAndelerMedEndringer = if (forrigeIverksatteBehandling != null) {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(forrigeIverksatteBehandling.id)
        } else {
            emptyList()
        }

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId = behandling.id)
                ?: return emptyList()

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        return mapTilOpphørsperioder(
            forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
            forrigeAndelerTilkjentYtelse = forrigeAndelerMedEndringer,
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse
        )
    }

    private fun hentAvslagsperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = vedtak.behandling
        val avslagsperioderFraVilkårsvurdering = hentAvslagsperioderFraVilkårsvurdering(behandling.id, vedtak)
        val avslagsperioderFraEndretUtbetalinger = hentAvslagsperioderFraEndretUtbetalinger(behandling.id, vedtak)

        val uregistrerteBarn = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id).hentUregistrerteBarn()

        return if (uregistrerteBarn.isNotEmpty()) {
            leggTilAvslagsbegrunnelseForUregistrertBarn(
                avslagsperioder = avslagsperioderFraVilkårsvurdering + avslagsperioderFraEndretUtbetalinger,
                vedtak = vedtak,
                uregistrerteBarn = uregistrerteBarn
            )
        } else {
            avslagsperioderFraVilkårsvurdering + avslagsperioderFraEndretUtbetalinger
        }
    }

    private fun hentAvslagsperioderFraEndretUtbetalinger(
        behandlingId: Long,
        vedtak: Vedtak
    ): List<VedtaksperiodeMedBegrunnelser> {
        val endreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                behandlingId
            )

        val periodegrupperteAvslagEndreteUtbetalinger =
            endreteUtbetalinger.filter { it.erEksplisittAvslagPåSøknad == true }
                .groupBy { NullablePeriode(it.fom?.toLocalDate(), it.tom?.toLocalDate()) }

        val avslagsperioder = periodegrupperteAvslagEndreteUtbetalinger.map { (fellesPeriode, endretUtbetalinger) ->

            val avslagsbegrunnelser = endretUtbetalinger.map { it.begrunnelser }.flatten().toSet().toList()

            lagVedtaksPeriodeMedBegrunnelser(vedtak, fellesPeriode, avslagsbegrunnelser)
        }.toMutableList()

        return avslagsperioder
    }

    private fun hentAvslagsperioderFraVilkårsvurdering(
        behandlingId: Long,
        vedtak: Vedtak
    ): MutableList<VedtaksperiodeMedBegrunnelser> {
        val vilkårsvurdering =
            vilkårsvurderingRepository.finnAktivForBehandling(behandlingId = behandlingId) ?: throw Feil(
                "Fant ikke vilkårsvurdering for behandling $behandlingId ved generering av avslagsperioder"
            )

        val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
            vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
                .filter { it.erEksplisittAvslagPåSøknad == true }
                .groupBy { NullablePeriode(it.periodeFom, it.periodeTom) }

        val avslagsperioder = periodegrupperteAvslagsvilkår.map { (fellesPeriode, vilkårResultater) ->

            val avslagsbegrunnelser = vilkårResultater.map { it.begrunnelser }.flatten().toSet().toList()

            lagVedtaksPeriodeMedBegrunnelser(vedtak, fellesPeriode, avslagsbegrunnelser)
        }.toMutableList()

        return avslagsperioder
    }

    private fun lagVedtaksPeriodeMedBegrunnelser(
        vedtak: Vedtak,
        periode: NullablePeriode,
        avslagsbegrunnelser: List<Begrunnelse>
    ): VedtaksperiodeMedBegrunnelser = VedtaksperiodeMedBegrunnelser(
        vedtak = vedtak, fom = periode.fom, tom = periode.tom?.sisteDagIMåned(), type = Vedtaksperiodetype.AVSLAG
    ).apply {
        begrunnelser.addAll(
            avslagsbegrunnelser.map { begrunnelse ->
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = this, begrunnelse = begrunnelse
                )
            }
        )
    }
}

private fun leggTilAvslagsbegrunnelseForUregistrertBarn(
    avslagsperioder: List<VedtaksperiodeMedBegrunnelser>,
    vedtak: Vedtak,
    uregistrerteBarn: List<BarnMedOpplysningerDto>
): List<VedtaksperiodeMedBegrunnelser> {
    val avslagsperioderMedTomPeriode = if (avslagsperioder.none { it.fom == null && it.tom == null }) {
        avslagsperioder + VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak, fom = null, tom = null, type = Vedtaksperiodetype.AVSLAG
        )
    } else {
        avslagsperioder
    }

    return avslagsperioderMedTomPeriode.map {
        if (it.fom == null && it.tom == null && uregistrerteBarn.isNotEmpty()) {
            it.apply {
                begrunnelser.add(
                    Vedtaksbegrunnelse(
                        vedtaksperiodeMedBegrunnelser = this, begrunnelse = Begrunnelse.AVSLAG_UREGISTRERT_BARN
                    )
                )
            }
        } else {
            it
        }
    }.toList()
}
