package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.tilFeilutbetaltValutaDto
import no.nav.familie.ks.sak.api.dto.tilTotrinnskontrollDto
import no.nav.familie.ks.sak.api.dto.tilUtbetalingsperiodeResponsDto
import no.nav.familie.ks.sak.api.dto.tilUtvidetVedtaksperiodeMedBegrunnelserDto
import no.nav.familie.ks.sak.api.dto.tilVedtakDto
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper.lagPersonRespons
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper.lagPersonerMedAndelTilkjentYtelseRespons
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.LOVENDRING_2024
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatValideringUtils
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.tilEndretUtbetalingAndelResponsDto
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringstidspunktService
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Period
import java.time.YearMonth

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val statsborgerskapService: StatsborgerskapService,
    private val loggService: LoggService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val vedtakRepository: VedtakRepository,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val sanityService: SanityService,
    private val feilutbetaltValutaService: FeilutbetaltValutaService,
    private val kompetanseRepository: KompetanseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val valutakursRepository: ValutakursRepository,
    private val refusjonEøsService: RefusjonEøsService,
    private val korrigertEtterbetalingRepository: KorrigertEtterbetalingRepository,
    private val brevmottakerService: BrevmottakerService,
    private val overgangsordningAndelService: OvergangsordningAndelService,
    private val oppgaveService: OppgaveService,
    private val sakStatistikkService: SakStatistikkService,
    private val korrigertVedtakRepository: KorrigertVedtakRepository,
    private val adopsjonService: AdopsjonService,
    private val endringstidspunktService: EndringstidspunktService,
) {
    fun hentBehandling(behandlingId: Long): Behandling = behandlingRepository.hentBehandling(behandlingId)

    fun hentAktivtBehandling(behandlingId: Long): Behandling = behandlingRepository.hentAktivBehandling(behandlingId)

    fun hentBehandlingerPåFagsak(fagsakId: Long): List<Behandling> = behandlingRepository.finnBehandlinger(fagsakId)

    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? =
        behandlingRepository
            .finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }

    fun oppdaterBehandling(behandling: Behandling): Behandling {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun lagBehandlingRespons(behandlingId: Long): BehandlingResponsDto {
        val behandling = hentBehandling(behandlingId)
        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId)
        val personer = personopplysningGrunnlag?.personer?.toList() ?: emptyList()
        val landKodeOgLandNavn =
            personer
                .flatMap { it.statsborgerskap }
                .toSet()
                .associate { it.landkode to statsborgerskapService.hentLand(it.landkode) }
        val adopsjonerIBehandling = adopsjonService.hentAlleAdopsjonerForBehandling(behandlingId = BehandlingId(behandlingId))

        val personResponser =
            personer.map { person ->
                lagPersonRespons(
                    person = person,
                    landKodeOgLandNavn = landKodeOgLandNavn,
                    adopsjon = adopsjonerIBehandling.firstOrNull { it.aktør == person.aktør },
                )
            }

        val søknadsgrunnlag = søknadGrunnlagService.finnAktiv(behandlingId)?.tilSøknadDto()
        val personResultater =
            vilkårsvurderingService.finnAktivVilkårsvurdering(behandlingId)?.personResultater?.toList()

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        val personerMedAndelerTilkjentYtelse =
            personopplysningGrunnlag?.let {
                lagPersonerMedAndelTilkjentYtelseRespons(
                    it.personer,
                    andelerTilkjentYtelse,
                )
            }
                ?: emptyList()

        val andelTilkjentYtelseMedEndreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val utbetalingsperioder =
            personopplysningGrunnlag?.let { andelTilkjentYtelseMedEndreteUtbetalinger.tilUtbetalingsperiodeResponsDto(personopplysningGrunnlag = it, adopsjonerIBehandling = adopsjonerIBehandling) }
                ?: emptyList()

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val vedtak =
            vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)?.let {
                it.tilVedtakDto(
                    vedtaksperioderMedBegrunnelser =
                        if (behandling.status != BehandlingStatus.AVSLUTTET) {
                            vedtaksperiodeService
                                .hentUtvidetVedtaksperioderMedBegrunnelser(vedtak = it)
                                .map { utvidetVedtaksperiodeMedBegrunnelser ->
                                    utvidetVedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelserDto(sanityBegrunnelser = sanityBegrunnelser, adopsjonerIBehandling = adopsjonerIBehandling)
                                }.sortedBy { dto -> dto.fom }
                        } else {
                            emptyList()
                        },
                    skalMinimeres = behandling.status != BehandlingStatus.UTREDES,
                )
            }

        val endreteUtbetalingerMedAndeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)
                .map { it.tilEndretUtbetalingAndelResponsDto() }

        val overgangsordningAndeler = overgangsordningAndelService.hentOvergangsordningAndeler(behandlingId).map { it.tilOvergangsordningAndelDto() }

        val totrinnskontroll =
            totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)?.tilTotrinnskontrollDto()

        val endringstidspunkt = endringstidspunktService.finnEndringstidspunktForBehandling(behandling)

        val sisteVedtaksperiodeVisningDato =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(behandling.id)

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandlingId)

        val feilutbetalteValuta =
            feilutbetaltValutaService.hentAlleFeilutbetaltValutaForBehandling(behandlingId).map {
                it.tilFeilutbetaltValutaDto()
            }

        val kompetanser = kompetanseRepository.findByBehandlingId(behandlingId)

        val refusjonEøs = refusjonEøsService.hentRefusjonEøsPerioder(behandlingId)

        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.findByBehandlingId(behandlingId)
        val valutakurser = valutakursRepository.findByBehandlingId(behandlingId)
        val korrigertEtterbetaling = korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandlingId)
        val korrigertVedtak = korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandlingId)

        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)

        return BehandlingMapper.lagBehandlingRespons(
            behandling,
            arbeidsfordelingPåBehandling,
            søknadsgrunnlag,
            personResponser,
            personResultater,
            personerMedAndelerTilkjentYtelse,
            utbetalingsperioder,
            vedtak,
            totrinnskontroll,
            endreteUtbetalingerMedAndeler,
            overgangsordningAndeler,
            endringstidspunkt,
            tilbakekreving,
            sisteVedtaksperiodeVisningDato,
            feilutbetalteValuta,
            kompetanser,
            refusjonEøs,
            utenlandskePeriodebeløp,
            valutakurser,
            korrigertEtterbetaling,
            korrigertVedtak,
            brevmottakere,
        )
    }

    fun oppdaterBehandlendeEnhet(
        behandlingId: Long,
        endreBehandlendeEnhet: EndreBehandlendeEnhetDto,
    ) = arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(hentBehandling(behandlingId), endreBehandlendeEnhet)

    fun oppdaterBehandlingsresultat(
        behandlingId: Long,
        nyUtledetBehandlingsresultat: Behandlingsresultat,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)
        BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling, nyUtledetBehandlingsresultat)

        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} endrer resultat på behandling $behandlingId " +
                "fra ${behandling.resultat} til $nyUtledetBehandlingsresultat",
        )
        loggService.opprettVilkårsvurderingLogg(
            behandling = behandling,
            behandlingsForrigeResultat = behandling.resultat,
            behandlingsNyResultat = nyUtledetBehandlingsresultat,
        )
        behandling.resultat = nyUtledetBehandlingsresultat
        return oppdaterBehandling(behandling)
    }

    fun nullstillEndringstidspunkt(behandlingId: Long) {
        val behandling = hentBehandling(behandlingId)
        behandling.overstyrtEndringstidspunkt = null
        oppdaterBehandling(behandling)
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(page: Pageable): Page<Long> = behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker(page)

    fun hentAktivtFødselsnummerForBehandlinger(behandlingIder: List<Long>): Map<Long, String> = behandlingRepository.finnAktivtFødselsnummerForBehandlinger(behandlingIder).associate { it.first to it.second }

    fun hentIverksatteBehandlinger(fagsakId: Long): List<Behandling> = behandlingRepository.finnIverksatteBehandlinger(fagsakId = fagsakId)

    /**
     * Henter siste iverksatte behandling på fagsak
     */
    fun hentSisteBehandlingSomErIverksatt(fagsakId: Long): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)
        return hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
    }

    private fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? =
        iverksatteBehandlinger
            .filter { it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }
            .maxByOrNull { it.aktivertTidspunkt }

    @Transactional
    fun endreBehandlingstemaPåBehandling(
        behandlingId: Long,
        overstyrtKategori: BehandlingKategori,
    ): Behandling {
        val behandling = hentBehandling(behandlingId)

        if (overstyrtKategori == behandling.kategori) return behandling

        loggService.opprettEndretBehandlingstemaLogg(
            behandling = behandling,
            forrigeKategori = behandling.kategori,
            nyKategori = overstyrtKategori,
        )

        behandling.kategori = overstyrtKategori

        return oppdaterBehandling(behandling).also {
            oppgaveService.oppdaterBehandlingstypePåOppgaverFraBehandling(it)
            sakStatistikkService.sendMeldingOmEndringAvBehandlingkategori(behandlingId, overstyrtKategori)
        }
    }

    fun erLovendringOgFremtidigOpphørOgHarFlereAndeler(
        behandling: Behandling,
    ): Boolean {
        if (behandling.opprettetÅrsak != LOVENDRING_2024) {
            return false
        }

        if (!vilkårsvurderingService.erFremtidigOpphørIBehandling(behandling)) {
            return false
        }

        val fagsakId = behandling.fagsak.id
        val sisteIverksatteBehandling = hentSisteBehandlingSomErIverksatt(fagsakId) ?: throw Feil("Fant ingen iverksatt behandling for fagsak $fagsakId")

        val andelerNåværendeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        val andelerForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id)

        val aktører = (andelerNåværendeBehandling.map { it.aktør } + andelerForrigeBehandling.map { it.aktør }).distinct()

        val erBarnSomFåNyAndelIAugust =
            aktører.any { aktør ->
                val sisteNåværendeUtbetalingForAktør =
                    andelerNåværendeBehandling.filter<AndelTilkjentYtelse> { it.aktør == aktør }.maxOfOrNull<AndelTilkjentYtelse, YearMonth> { it.stønadTom } ?: return@any false
                val sisteForrigeUtbetalingForAktør =
                    andelerForrigeBehandling.filter { it.aktør == aktør }.maxOfOrNull { it.stønadTom } ?: TIDENES_MORGEN.toYearMonth()

                val antallMånederMellomForrigeOgNåværendeUtbetalinger =
                    Period
                        .between(
                            sisteForrigeUtbetalingForAktør.toLocalDate(),
                            sisteNåværendeUtbetalingForAktør.toLocalDate(),
                        ).months

                if (antallMånederMellomForrigeOgNåværendeUtbetalinger > 1) {
                    throw Feil("Antall måneder differanse mellom forrige og nåværende utbetaling overstiger 1")
                }

                val erOpphørIAugustForForrigeUtbetaling = sisteForrigeUtbetalingForAktør == YearMonth.of(2024, 7)
                val erOpphørISeptemberForNåværendeUtbetaling = sisteNåværendeUtbetalingForAktør == YearMonth.of(2024, 8)

                erOpphørIAugustForForrigeUtbetaling && erOpphørISeptemberForNåværendeUtbetaling
            }

        val erBarnSomMisterAndel =
            aktører.any { aktør ->
                val sisteNåværendeUtbetalingForAktør =
                    andelerNåværendeBehandling.filter { it.aktør == aktør }.maxOfOrNull { it.stønadTom } ?: TIDENES_MORGEN.toYearMonth()
                val sisteForrigeUtbetalingForAktør =
                    andelerForrigeBehandling.filter { it.aktør == aktør }.maxOfOrNull { it.stønadTom } ?: TIDENES_MORGEN.toYearMonth()

                sisteNåværendeUtbetalingForAktør < sisteForrigeUtbetalingForAktør
            }

        if (erBarnSomFåNyAndelIAugust && erBarnSomMisterAndel) {
            throw Feil("LOVENDRING_2024_FLERE_BARN: Det finnes et barn som får ny andel i august og et annet barn som mister minst en andel")
        }

        if (erBarnSomFåNyAndelIAugust) {
            throw Feil(
                "LOVENDRING_2024_ANDEL_I_AUGUST: Forrige behandling har opphør i august. Nåværende behandling har opphør i september. Disse tilfellene skal ikke revurderes",
            )
        }

        return aktører.any { aktør ->
            val sisteNåværendeUtbetalingForAktør =
                andelerNåværendeBehandling.filter<AndelTilkjentYtelse> { it.aktør == aktør }.maxOfOrNull<AndelTilkjentYtelse, YearMonth> { it.stønadTom } ?: return@any false
            val sisteForrigeUtbetalingForAktør =
                andelerForrigeBehandling.filter { it.aktør == aktør }.maxOfOrNull { it.stønadTom } ?: TIDENES_MORGEN.toYearMonth()

            sisteForrigeUtbetalingForAktør < sisteNåværendeUtbetalingForAktør
        }
    }

    fun hentFerdigstilteBehandlinger(fagsakId: Long): List<Behandling> =
        hentBehandlingerPåFagsak(fagsakId = fagsakId)
            .filter { it.erAvsluttet() && !it.erHenlagt() }

    fun hentSisteBehandlingSomErAvsluttetEllerSendtTilØkonomiPerFagsak(fagsakIder: Set<Long>): List<Behandling> {
        val behandlingerPåFagsakene = behandlingRepository.finnBehandlinger(fagsakIder)

        return behandlingerPåFagsakene
            .groupBy { it.fagsak.id }
            .mapNotNull { (_, behandling) -> behandling.filtrerSisteBehandlingSomErAvsluttetEllerSendtTilØkonomi() }
    }

    private fun List<Behandling>.filtrerSisteBehandlingSomErAvsluttetEllerSendtTilØkonomi() =
        filter { !it.erHenlagt() && (it.status == BehandlingStatus.AVSLUTTET || it.status == BehandlingStatus.IVERKSETTER_VEDTAK) }
            .maxByOrNull { it.aktivertTidspunkt }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
