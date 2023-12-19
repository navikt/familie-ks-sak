package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSenereEnnInneværendeMåned
import no.nav.familie.ks.sak.common.util.storForbokstav
import no.nav.familie.ks.sak.common.util.tilMånedÅr
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser.hentUtbetalingsperioder
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvVilkårResultater
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelserForPeriodeContext
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
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
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val integrasjonClient: IntegrasjonClient,
    private val refusjonEøsRepository: RefusjonEøsRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val kompetanseService: KompetanseService,
) {
    fun oppdaterVedtaksperiodeMedFritekster(
        vedtaksperiodeId: Long,
        fritekster: List<String>,
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        vedtaksperiodeMedBegrunnelser.settFritekster(
            fritekster.map {
                tilVedtaksbegrunnelseFritekst(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    fritekst = it,
                )
            },
        )

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperiodeMedBegrunnelser(
        vedtaksperiodeId: Long,
        begrunnelserFraFrontend: List<Begrunnelse>,
        eøsBegrunnelserFraFrontend: List<EØSBegrunnelse> = emptyList(),
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling

        val persongrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            begrunnelserFraFrontend.map {
                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
            },
        )

        if (
            begrunnelserFraFrontend.any { it.begrunnelseType == BegrunnelseType.ENDRET_UTBETALING }
        ) {
            val andelerTilkjentYtelse =
                andelerTilkjentYtelseOgEndreteUtbetalingerService
                    .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

            validerEndretUtbetalingsbegrunnelse(vedtaksperiodeMedBegrunnelser, andelerTilkjentYtelse, persongrunnlag)
        }

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun skalHaÅrligKontroll(vedtak: Vedtak): Boolean {
        return vedtak.behandling.kategori == BehandlingKategori.EØS &&
            hentPersisterteVedtaksperioder(vedtak).any { it.tom?.erSenereEnnInneværendeMåned() != false }
    }

    private fun validerEndretUtbetalingsbegrunnelse(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        persongrunnlag: PersonopplysningGrunnlag,
    ) {
        try {
            vedtaksperiodeMedBegrunnelser.hentUtbetalingsperiodeDetaljer(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag,
            )
        } catch (e: Exception) {
            throw FunksjonellFeil(
                "Begrunnelse for endret utbetaling er ikke gyldig for vedtaksperioden",
            )
        }
    }

    fun finnSisteVedtaksperiodeVisningsdatoForBehandling(behandlingId: Long): LocalDate? {
        val listeAvVilkårSomAlltidSkalKunneBegrunnes = listOf(Vilkår.BARNETS_ALDER, Vilkår.BARNEHAGEPLASS)

        val vilkårsvurdering =
            vilkårsvurderingRepository.finnAktivForBehandling(behandlingId = behandlingId) ?: return null

        return vilkårsvurdering.personResultater.mapNotNull { personResultat ->

            val vilkårResultaterForAktørSomAlltidSkalKunneBegrunnes =
                personResultat.vilkårResultater.filter { listeAvVilkårSomAlltidSkalKunneBegrunnes.contains(it.vilkårType) && it.periodeFom != null }

            val vilkårResultaterForAktørMapSomAlltidSkalKunneBegrunnes =
                vilkårResultaterForAktørSomAlltidSkalKunneBegrunnes
                    .groupByTo(mutableMapOf()) { it.vilkårType }
                    .mapValues { it.value }

            vilkårResultaterForAktørMapSomAlltidSkalKunneBegrunnes.flatMap { (vilkårType, vilkårResultater) ->
                forskyvVilkårResultater(vilkårType, vilkårResultater).tilTidslinje().tilPerioderIkkeNull()
            }.mapNotNull { it.tom }.maxOfOrNull { it }
        }.maxOfOrNull { it }
    }

    @Transactional
    fun oppdaterVedtakMedVedtaksperioder(
        vedtak: Vedtak,
        skalOverstyreFortsattInnvilget: Boolean = false,
    ) {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)
        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET && !skalOverstyreFortsattInnvilget) {
            vedtaksperiodeHentOgPersisterService.lagre(
                VedtaksperiodeMedBegrunnelser(
                    fom = null,
                    tom = null,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET,
                ),
            )
        } else {
            vedtaksperiodeHentOgPersisterService.lagre(
                genererVedtaksperioderMedBegrunnelser(
                    vedtak,
                    gjelderFortsattInnvilget = skalOverstyreFortsattInnvilget,
                ),
            )
        }
    }

    fun genererVedtaksperioderMedBegrunnelser(
        vedtak: Vedtak,
        gjelderFortsattInnvilget: Boolean = false,
        manueltOverstyrtEndringstidspunkt: LocalDate? = null,
    ): List<VedtaksperiodeMedBegrunnelser> {
        val endredeUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                vedtak.behandling.id,
            )
        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(vedtak.behandling.id)

        val iverksatteBehandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsakId = vedtak.behandling.fagsak.id)
        val forrigeIverksatteBehandling =
            iverksatteBehandlinger
                .filter { it.opprettetTidspunkt.isBefore(vedtak.behandling.opprettetTidspunkt) && it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }
                .maxByOrNull { it.opprettetTidspunkt }
        val personopplysningGrunnlagForrigeBehandling = forrigeIverksatteBehandling?.let { personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(forrigeIverksatteBehandling.id) }
        val andelerMedEndringerForrigeBehandling = forrigeIverksatteBehandling?.let { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(forrigeIverksatteBehandling.id) } ?: emptyList()

        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = vedtak.behandling.id)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)

        val sisteVedtatteBehandling = hentSisteBehandlingSomErVedtatt(vedtak.behandling.fagsak.id)

        // ^ Henter data
        val opphørsperioder =
            hentOpphørsperioder(
                behandling = vedtak.behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlagForrigeBehandling = personopplysningGrunnlagForrigeBehandling,
                forrigeAndelerMedEndringer = andelerMedEndringerForrigeBehandling,
            ).map { it.tilVedtaksperiodeMedBegrunnelse(vedtak) }

        val utbetalingsperioder =
            hentUtbetalingsperioder(
                vedtak = vedtak,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
                kompetanser = kompetanseService.hentKompetanser(vedtak.behandling.behandlingId),
            )

        val avslagsperioder =
            hentAvslagsperioderMedBegrunnelser(
                vedtak = vedtak,
                endredeUtbetalinger = endredeUtbetalinger,
                søknadGrunnlag = søknadGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
            )

        val utbetalingsperioderUtenOverlappMedAvslagsperioder =
            filtrerUtUtbetalingsperioderMedSammeDatoSomAvslagsperioder(
                utbetalingsperioder,
                avslagsperioder,
            )

        return filtrerUtPerioderBasertPåEndringstidspunkt(
            vedtaksperioderMedBegrunnelser = (utbetalingsperioderUtenOverlappMedAvslagsperioder + opphørsperioder),
            manueltOverstyrtEndringstidspunkt = manueltOverstyrtEndringstidspunkt,
            gjelderFortsattInnvilget = gjelderFortsattInnvilget,
            sisteVedtatteBehandling = sisteVedtatteBehandling,
            andelerTilkjentYtelseForBehandling = andelerTilkjentYtelse,
            andelerTilkjentYtelseForForrigeBehandling = andelerMedEndringerForrigeBehandling,
        ) + avslagsperioder
    }

    private fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? =
        behandlingRepository.finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.opprettetTidspunkt }

    @Transactional
    fun genererVedtaksperiodeForOverstyrtEndringstidspunkt(
        behandlingId: Long,
        overstyrtEndringstidspunkt: LocalDate,
    ) {
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)

        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) {
            oppdaterVedtakMedVedtaksperioder(vedtak)
        } else {
            vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)
            val vedtaksperioder =
                genererVedtaksperioderMedBegrunnelser(
                    vedtak = vedtak,
                    manueltOverstyrtEndringstidspunkt = overstyrtEndringstidspunkt,
                )
            vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioder.sortedBy { it.fom })
        }
        lagreNedOverstyrtEndringstidspunkt(vedtak.behandling.id, overstyrtEndringstidspunkt)
    }

    private fun lagreNedOverstyrtEndringstidspunkt(
        behandlingId: Long,
        overstyrtEndringstidspunkt: LocalDate,
    ) {
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)
        behandling.overstyrtEndringstidspunkt = overstyrtEndringstidspunkt

        behandlingRepository.save(behandling)
    }

    fun kopierOverVedtaksperioder(
        deaktivertVedtak: Vedtak,
        aktivtVedtak: Vedtak,
    ) {
        val gamleVedtaksperioderMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioderMedBegrunnelser.forEach { vedtaksperiodeMedBegrunnelser ->
            val nyVedtaksperiodeMedBegrunnelser =
                vedtaksperiodeHentOgPersisterService.lagre(
                    VedtaksperiodeMedBegrunnelser(
                        vedtak = aktivtVedtak,
                        fom = vedtaksperiodeMedBegrunnelser.fom,
                        tom = vedtaksperiodeMedBegrunnelser.tom,
                        type = vedtaksperiodeMedBegrunnelser.type,
                    ),
                )

            nyVedtaksperiodeMedBegrunnelser.settBegrunnelser(
                vedtaksperiodeMedBegrunnelser.begrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                },
            )
            nyVedtaksperiodeMedBegrunnelser.settFritekster(
                vedtaksperiodeMedBegrunnelser.fritekster.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                },
            )

            vedtaksperiodeHentOgPersisterService.lagre(nyVedtaksperiodeMedBegrunnelser)
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> =
        vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(vedtakId = vedtak.id)

    fun hentUtvidetVedtaksperiodeMedBegrunnelser(vedtaksperiodeId: Long): UtvidetVedtaksperiodeMedBegrunnelser {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId = vedtaksperiodeId)

        val behandlingId = vedtaksperiodeMedBegrunnelser.vedtak.behandling.id

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandlingId)

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId = behandlingId,
            )

        return vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag,
            andelerTilkjentYtelse,
        )
    }

    fun hentUtvidetVedtaksperioderMedBegrunnelser(vedtak: Vedtak): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)

        val utvidetVedtaksperioderMedBegrunnelser =
            vedtaksperioderMedBegrunnelser.map {
                it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                )
            }

        val skalSendeMedGyldigeBegrunnelser =
            behandling.status == BehandlingStatus.UTREDES && utvidetVedtaksperioderMedBegrunnelser.isNotEmpty()

        return if (skalSendeMedGyldigeBegrunnelser) {
            hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
                behandling = behandling,
                utvidedeVedtaksperioderMedBegrunnelser = utvidetVedtaksperioderMedBegrunnelser,
                persongrunnlag = personopplysningGrunnlag,
            )
        } else {
            utvidetVedtaksperioderMedBegrunnelser
        }
    }

    private fun hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
        behandling: Behandling,
        utvidedeVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        persongrunnlag: PersonopplysningGrunnlag,
    ): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vilkårsvurdering =
            vilkårsvurderingRepository.finnAktivForBehandling(behandling.id)
                ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val endreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                behandling.id,
            )
                .map { it.endretUtbetalingAndel }

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
                .map { it.andel }

        return utvidedeVedtaksperioderMedBegrunnelser
            .sortedBy { it.fom }
            .mapNotNull { utvidetVedtaksperiodeMedBegrunnelser ->

                val erFørsteVedtaksperiodePåFagsak =
                    !andelerTilkjentYtelse.any {
                        it.stønadFom.isBefore(
                            utvidetVedtaksperiodeMedBegrunnelser.fom?.toYearMonth() ?: TIDENES_MORGEN.toYearMonth(),
                        )
                    }

                utvidetVedtaksperiodeMedBegrunnelser.copy(
                    gyldigeBegrunnelser =
                        BegrunnelserForPeriodeContext(
                            utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                            sanityBegrunnelser = sanityBegrunnelser,
                            personopplysningGrunnlag = persongrunnlag,
                            personResultater = vilkårsvurdering.personResultater.toList(),
                            endretUtbetalingsandeler = endreteUtbetalinger,
                            erFørsteVedtaksperiode = erFørsteVedtaksperiodePåFagsak,
                        ).hentGyldigeBegrunnelserForVedtaksperiode(),
                )
            }
    }

    fun beskrivPerioderMedRefusjonEøs(
        behandling: Behandling,
        avklart: Boolean,
    ): Set<String>? {
        val målform = personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id)
        val landkoderISO2 = integrasjonClient.hentLandkoderISO2()

        return refusjonEøsRepository.finnRefusjonEøsForBehandling(behandling.id)
            .filter { it.refusjonAvklart == avklart }.map {
                val (fom, tom) = it.fom.tilMånedÅr() to it.tom.tilMånedÅr()
                val land =
                    landkoderISO2[it.land]?.storForbokstav() ?: throw Feil("Fant ikke navn for landkode ${it.land}")
                val beløp = it.refusjonsbeløp

                when (målform) {
                    Målform.NN -> {
                        if (avklart) {
                            "Frå $fom til $tom blir etterbetaling på $beløp kroner per måned utbetalt til myndighetene i $land."
                        } else {
                            "Frå $fom til $tom blir ikkje etterbetaling på $beløp kroner per månad utbetalt no sidan det er utbetalt barnetrygd i $land."
                        }
                    }

                    else -> {
                        if (avklart) {
                            "Fra $fom til $tom blir etterbetaling på $beløp kroner per måned utbetalt til myndighetene i $land."
                        } else {
                            "Fra $fom til $tom blir ikke etterbetaling på $beløp kroner per måned utbetalt nå siden det er utbetalt barnetrygd i $land."
                        }
                    }
                }
            }.toSet().takeIf { it.isNotEmpty() }
    }
}
