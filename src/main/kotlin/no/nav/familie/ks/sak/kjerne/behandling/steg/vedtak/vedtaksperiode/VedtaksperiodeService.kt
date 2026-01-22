package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.common.util.erSenereEnnInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.storForbokstav
import no.nav.familie.ks.sak.common.util.tilMånedÅr
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.avslagsperiode.AvslagsperiodeGenerator
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.opphørsperiode.OpphørsperiodeGenerator
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.opphørsperiode.tilVedtaksperiodeMedBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiode.UtbetalingsperiodeGenerator
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.forskyvVilkårResultater
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelserForPeriodeContext
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse.REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringstidspunktService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
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
    private val overgangsordningAndelService: OvergangsordningAndelService,
    private val sanityService: SanityService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val integrasjonKlient: IntegrasjonKlient,
    private val refusjonEøsRepository: RefusjonEøsRepository,
    private val kompetanseService: KompetanseService,
    private val adopsjonService: AdopsjonService,
    private val endringstidspunktService: EndringstidspunktService,
    private val opphørsperiodeGenerator: OpphørsperiodeGenerator,
    private val utbetalingsperiodeGenerator: UtbetalingsperiodeGenerator,
    private val avslagsperiodeGenerator: AvslagsperiodeGenerator,
) {
    fun oppdaterVedtaksperiodeMedFritekster(
        vedtaksperiodeId: Long,
        fritekster: List<String>,
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

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
        begrunnelserFraFrontend: List<NasjonalEllerFellesBegrunnelse>,
        eøsBegrunnelserFraFrontend: List<EØSBegrunnelse> = emptyList(),
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling

        val persongrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        validerReduksjonFramtidigOpphørBarnehageplassIkkeFjernet(vedtaksperiodeMedBegrunnelser, begrunnelserFraFrontend)

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            begrunnelserFraFrontend.map {
                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
            },
        )

        vedtaksperiodeMedBegrunnelser.settEøsBegrunnelser(
            eøsBegrunnelserFraFrontend.map {
                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
            },
        )

        if (begrunnelserFraFrontend.any { it.begrunnelseType == BegrunnelseType.ENDRET_UTBETALING }) {
            val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

            validerEndretUtbetalingsbegrunnelse(vedtaksperiodeMedBegrunnelser, andelerTilkjentYtelse, persongrunnlag)
        }

        val alleBegrunnelserStøtterFritekst = behandling.erRevurderingKlage()

        if (!vedtaksperiodeMedBegrunnelser.støtterFritekst(sanityBegrunnelser, alleBegrunnelserStøtterFritekst)) {
            vedtaksperiodeMedBegrunnelser.fritekster.clear()
        }

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    private fun validerReduksjonFramtidigOpphørBarnehageplassIkkeFjernet(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        begrunnelserFraFrontend: List<NasjonalEllerFellesBegrunnelse>,
    ) {
        if (vedtaksperiodeMedBegrunnelser.begrunnelser
                .map { it.nasjonalEllerFellesBegrunnelse }
                .contains(REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS) &&
            begrunnelserFraFrontend.none { it == REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS }
        ) {
            throw FunksjonellFeil("Reduksjonsbegrunnelsen Framtidig opphør barnehageplass ble lagt til automatisk, og skal ikke fjernes. Hvis du er uenig, ta kontakt med brukerstøtte.")
        }
    }

    fun skalHaÅrligKontroll(vedtak: Vedtak): Boolean = vedtak.behandling.kategori == BehandlingKategori.EØS && hentPersisterteVedtaksperioder(vedtak).any { it.tom?.erSenereEnnInneværendeMåned() != false }

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

        val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandlingId = behandlingId) ?: return null

        val sisteTomPåOvergangsordningSomAlltidSkalKunneBegrunnes = overgangsordningAndelService.hentOvergangsordningAndeler(behandlingId).mapNotNull { it.tom?.toLocalDate()?.sisteDagIMåned() }.maxOrNull() ?: TIDENES_MORGEN

        val sisteTomPåVilkårSomAlltidSkalKunneBegrunnes =
            vilkårsvurdering.personResultater
                .mapNotNull { personResultat ->
                    personResultat
                        .forskyvVilkårResultater()
                        .filterKeys { listeAvVilkårSomAlltidSkalKunneBegrunnes.contains(it) }
                        .values
                        .flatten()
                        .mapNotNull { it.verdi.periodeTom }
                        .maxOrNull()
                }.maxOfOrNull { it } ?: TIDENES_MORGEN

        return maxOf(
            sisteTomPåOvergangsordningSomAlltidSkalKunneBegrunnes,
            sisteTomPåVilkårSomAlltidSkalKunneBegrunnes,
        ).takeIf {
            it != TIDENES_MORGEN
        }
    }

    @Transactional
    fun oppdaterVedtakMedVedtaksperioder(
        vedtak: Vedtak,
    ) {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)
        val genererteVedtaksperioderMedBegrunnelser = genererVedtaksperioderMedBegrunnelser(vedtak)
        vedtaksperiodeHentOgPersisterService.lagre(genererteVedtaksperioderMedBegrunnelser)
    }

    fun genererVedtaksperioderMedBegrunnelser(
        vedtak: Vedtak,
        manueltOverstyrtEndringstidspunkt: LocalDate? = null,
    ): List<VedtaksperiodeMedBegrunnelser> {
        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) {
            return listOf(
                VedtaksperiodeMedBegrunnelser(
                    fom = null,
                    tom = null,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET,
                ),
            )
        }

        val opphørsperioder =
            if (vedtak.behandling.resultat == Behandlingsresultat.AVSLÅTT) {
                emptyList()
            } else {
                opphørsperiodeGenerator.genererOpphørsperioder(vedtak.behandling).map { it.tilVedtaksperiodeMedBegrunnelse(vedtak) }
            }

        val utbetalingsperioder = utbetalingsperiodeGenerator.genererUtbetalingsperioder(vedtak)
        val avslagsperioder = avslagsperiodeGenerator.genererAvslagsperioder(vedtak)

        val vedtaksperioderMedBegrunnelser =
            filtrerUtPerioderBasertPåEndringstidspunkt(
                vedtaksperioderMedBegrunnelser = (utbetalingsperioder + opphørsperioder),
                behandling = vedtak.behandling,
                manueltOverstyrtEndringstidspunkt = manueltOverstyrtEndringstidspunkt,
            ) + avslagsperioder

        return vedtaksperioderMedBegrunnelser
            .settRiktigTomPåAvslagsperioder()
            .filtrerUtPerioderMedSammeDatoSomAvslagsperioder()
    }

    fun filtrerUtPerioderBasertPåEndringstidspunkt(
        vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
        behandling: Behandling,
        manueltOverstyrtEndringstidspunkt: LocalDate? = null,
    ): List<VedtaksperiodeMedBegrunnelser> {
        val endringstidspunkt =
            manueltOverstyrtEndringstidspunkt ?: endringstidspunktService.finnEndringstidspunktForBehandling(behandling)

        return vedtaksperioderMedBegrunnelser.filter {
            (it.tom ?: TIDENES_ENDE).erSammeEllerEtter(endringstidspunkt)
        }
    }

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
        val gamleVedtaksperioderMedBegrunnelser = vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

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

            nyVedtaksperiodeMedBegrunnelser.settEøsBegrunnelser(
                vedtaksperiodeMedBegrunnelser.eøsBegrunnelser.map {
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

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> = vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(vedtakId = vedtak.id)

    fun hentUtvidetVedtaksperiodeMedBegrunnelser(vedtaksperiodeId: Long): UtvidetVedtaksperiodeMedBegrunnelser {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId = vedtaksperiodeId)

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling

        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId = behandling.id,
            )

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val alleBegrunnelserStøtterFritekst = behandling.erRevurderingKlage()

        return vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            sanityBegrunnelser = sanityBegrunnelser,
            alleBegrunnelserStøtterFritekst = alleBegrunnelserStøtterFritekst,
        )
    }

    fun hentUtvidetVedtaksperioderMedBegrunnelser(vedtak: Vedtak): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val alleBegrunnelserStøtterFritekst = behandling.erRevurderingKlage()

        val utvidetVedtaksperioderMedBegrunnelser =
            vedtaksperioderMedBegrunnelser.map {
                it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    sanityBegrunnelser = sanityBegrunnelser,
                    alleBegrunnelserStøtterFritekst = alleBegrunnelserStøtterFritekst,
                )
            }

        val skalSendeMedGyldigeBegrunnelser = behandling.status == BehandlingStatus.UTREDES && utvidetVedtaksperioderMedBegrunnelser.isNotEmpty()

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
        val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandling.id) ?: throw Feil("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val endreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                    behandling.id,
                ).map { it.endretUtbetalingAndel }

        val andeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.id,
            )

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id).map { it.andel }

        val utfylteKompetanser = kompetanseService.hentUtfylteKompetanser(behandling.behandlingId)

        return utvidedeVedtaksperioderMedBegrunnelser.sortedBy { it.fom }.map { utvidetVedtaksperiodeMedBegrunnelser ->

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
                        kompetanser = utfylteKompetanser,
                        personopplysningGrunnlag = persongrunnlag,
                        adopsjonerIBehandling = adopsjonService.hentAlleAdopsjonerForBehandling(BehandlingId(behandling.id)),
                        overgangsordningAndeler = overgangsordningAndelService.hentOvergangsordningAndeler(behandling.id),
                        personResultater = vilkårsvurdering.personResultater.toList(),
                        endretUtbetalingsandeler = endreteUtbetalinger,
                        erFørsteVedtaksperiode = erFørsteVedtaksperiodePåFagsak,
                        andelerTilkjentYtelse = andeler,
                    ).hentGyldigeBegrunnelserForVedtaksperiode(),
            )
        }
    }

    fun beskrivPerioderMedRefusjonEøs(
        behandling: Behandling,
        avklart: Boolean,
    ): Set<String>? {
        val målform = personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id)
        val landkoderISO2 = integrasjonKlient.hentLandkoderISO2()

        return refusjonEøsRepository
            .finnRefusjonEøsForBehandling(behandling.id)
            .filter { it.refusjonAvklart == avklart }
            .map {
                val (fom, tom) = it.fom.tilMånedÅr() to it.tom.tilMånedÅr()
                val land = landkoderISO2[it.land]?.storForbokstav() ?: throw Feil("Fant ikke navn for landkode ${it.land}")
                val beløp = it.refusjonsbeløp

                when (målform) {
                    Målform.NN -> {
                        if (avklart) {
                            "Frå $fom til $tom blir etterbetaling på $beløp kroner per måned utbetalt til myndighetene i $land."
                        } else {
                            "Frå $fom til $tom blir ikkje etterbetaling på $beløp kroner per månad utbetalt no sidan det er utbetalt kontantstøtte i $land."
                        }
                    }

                    else -> {
                        if (avklart) {
                            "Fra $fom til $tom blir etterbetaling på $beløp kroner per måned utbetalt til myndighetene i $land."
                        } else {
                            "Fra $fom til $tom blir ikke etterbetaling på $beløp kroner per måned utbetalt nå siden det er utbetalt kontantstøtte i $land."
                        }
                    }
                }
            }.toSet()
            .takeIf { it.isNotEmpty() }
    }
}

private fun List<VedtaksperiodeMedBegrunnelser>.filtrerUtPerioderMedSammeDatoSomAvslagsperioder(): List<VedtaksperiodeMedBegrunnelser> {
    val avslagsperioder = this.filter { it.type == Vedtaksperiodetype.AVSLAG }

    return this.filter { vedtaksperiode ->
        avslagsperioder.none { avslagsperiode ->
            avslagsperiode.fom == vedtaksperiode.fom &&
                avslagsperiode.tom == vedtaksperiode.tom &&
                vedtaksperiode.type != Vedtaksperiodetype.AVSLAG
        }
    }
}

private fun List<VedtaksperiodeMedBegrunnelser>.settRiktigTomPåAvslagsperioder(): List<VedtaksperiodeMedBegrunnelser> {
    val senesteVedtaksperiodeFom = this.maxByOrNull { it.fom ?: LocalDate.MIN }?.fom ?: return this

    return this.map { vedtaksperiode ->
        if (vedtaksperiode.fom == senesteVedtaksperiodeFom && vedtaksperiode.type == Vedtaksperiodetype.AVSLAG && vedtaksperiode.tom != null) {
            val begrunnelserBruktIAvslagsperiode = vedtaksperiode.begrunnelser.map { it.nasjonalEllerFellesBegrunnelse }

            vedtaksperiode
                .copy(
                    tom = null,
                ).apply {
                    begrunnelser.clear()
                    begrunnelser.addAll(
                        begrunnelserBruktIAvslagsperiode.map { begrunnelse ->
                            NasjonalEllerFellesBegrunnelseDB(
                                vedtaksperiodeMedBegrunnelser = this,
                                nasjonalEllerFellesBegrunnelse = begrunnelse,
                            )
                        },
                    )
                }
        } else {
            vedtaksperiode
        }
    }
}
