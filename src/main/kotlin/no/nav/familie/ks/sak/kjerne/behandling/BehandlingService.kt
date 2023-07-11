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
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.tilEndretUtbetalingAndelResponsDto
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger

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
    private val kompetanseRepository: KompetanseRepository
) {

    fun hentBehandling(behandlingId: Long): Behandling = behandlingRepository.hentBehandling(behandlingId)
    fun hentAktivtBehandling(behandlingId: Long): Behandling = behandlingRepository.hentAktivBehandling(behandlingId)
    fun hentBehandlingerPåFagsak(fagsakId: Long): List<Behandling> = behandlingRepository.finnBehandlinger(fagsakId)

    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? = behandlingRepository.finnBehandlinger(fagsakId)
        .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
        .maxByOrNull { it.opprettetTidspunkt }

    fun oppdaterBehandling(behandling: Behandling): Behandling {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun lagBehandlingRespons(behandlingId: Long): BehandlingResponsDto {
        val behandling = hentBehandling(behandlingId)
        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId)
        val personer = personopplysningGrunnlag?.personer?.toList() ?: emptyList()
        val landKodeOgLandNavn = personer.flatMap { it.statsborgerskap }.toSet()
            .associate { it.landkode to statsborgerskapService.hentLand(it.landkode) }
        val personResponser = personer.map { lagPersonRespons(it, landKodeOgLandNavn) }

        val søknadsgrunnlag = søknadGrunnlagService.finnAktiv(behandlingId)?.tilSøknadDto()
        val personResultater =
            vilkårsvurderingService.finnAktivVilkårsvurdering(behandlingId)?.personResultater?.toList()

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        val personerMedAndelerTilkjentYtelse =
            personopplysningGrunnlag?.let {
                lagPersonerMedAndelTilkjentYtelseRespons(
                    it.personer,
                    andelerTilkjentYtelse
                )
            }
                ?: emptyList()

        val andelTilkjentYtelseMedEndreteUtbetalinger = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val utbetalingsperioder =
            personopplysningGrunnlag?.let { andelTilkjentYtelseMedEndreteUtbetalinger.tilUtbetalingsperiodeResponsDto(it) }
                ?: emptyList()

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val vedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)?.let {
            it.tilVedtakDto(
                vedtaksperioderMedBegrunnelser = if (behandling.status != BehandlingStatus.AVSLUTTET) {
                    vedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(vedtak = it)
                        .map { utvidetVedtaksperiodeMedBegrunnelser ->
                            utvidetVedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelserDto(
                                sanityBegrunnelser
                            )
                        }
                        .sortedBy { dto -> dto.fom }
                } else {
                    emptyList()
                },
                skalMinimeres = behandling.status != BehandlingStatus.UTREDES
            )
        }

        val endreteUtbetalingerMedAndeler = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)
            .map { it.tilEndretUtbetalingAndelResponsDto() }

        val totrinnskontroll =
            totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)?.tilTotrinnskontrollDto()

        val endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(
            behandling = behandling,
            sisteVedtattBehandling = hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
        )

        val sisteVedtaksperiodeVisningDato =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(behandling.id)

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandlingId)

        val feilutbetalteValuta = feilutbetaltValutaService.hentAlleFeilutbetaltValutaForBehandling(behandlingId).map {
            it.tilFeilutbetaltValutaDto()
        }

        val kompetanser = kompetanseRepository.findByBehandlingId(behandlingId)

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
            endringstidspunkt,
            tilbakekreving,
            sisteVedtaksperiodeVisningDato,
            feilutbetalteValuta,
            kompetanser
        )
    }

    fun oppdaterBehandlendeEnhet(behandlingId: Long, endreBehandlendeEnhet: EndreBehandlendeEnhetDto) =
        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(hentBehandling(behandlingId), endreBehandlendeEnhet)

    fun oppdaterBehandlingsresultat(behandlingId: Long, nyUtledetBehandlingsresultat: Behandlingsresultat): Behandling {
        val behandling = hentBehandling(behandlingId)
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} endrer resultat på behandling $behandlingId " +
                "fra ${behandling.resultat} til $nyUtledetBehandlingsresultat"
        )
        loggService.opprettVilkårsvurderingLogg(
            behandling = behandling,
            behandlingsForrigeResultat = behandling.resultat,
            behandlingsNyResultat = nyUtledetBehandlingsresultat
        )
        behandling.resultat = nyUtledetBehandlingsresultat
        return oppdaterBehandling(behandling)
    }

    fun nullstillEndringstidspunkt(behandlingId: Long) {
        val behandling = hentBehandling(behandlingId)
        behandling.overstyrtEndringstidspunkt = null
        oppdaterBehandling(behandling)
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(page: Pageable): Page<BigInteger> =
        behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker(page)

    fun hentAktivtFødselsnummerForBehandlinger(behandlingIder: List<Long>): Map<Long, String> =
        behandlingRepository.finnAktivtFødselsnummerForBehandlinger(behandlingIder).associate { it.first to it.second }

    fun hentIverksatteBehandlinger(fagsakId: Long): List<Behandling> =
        behandlingRepository.finnIverksatteBehandlinger(fagsakId = fagsakId)

    /**
     * Henter siste iverksatte behandling på fagsak
     */
    fun hentSisteBehandlingSomErIverksatt(fagsakId: Long): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)
        return hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
    }

    private fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? {
        return iverksatteBehandlinger
            .filter { it.steg == BehandlingSteg.AVSLUTT_BEHANDLING }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    @Transactional
    fun endreBehandlingstemaPåBehandling(behandlingId: Long, overstyrtKategori: BehandlingKategori): Behandling {
        val behandling = hentBehandling(behandlingId)

        if (overstyrtKategori == behandling.kategori) return behandling

        loggService.opprettEndretBehandlingstemaLogg(
            behandling = behandling,
            forrigeKategori = behandling.kategori,
            nyKategori = overstyrtKategori
        )

        behandling.kategori = overstyrtKategori
        return oppdaterBehandling(behandling)
    }

    fun hentFerdigstilteBehandlinger(fagsak: Fagsak): List<Behandling> {
        return hentBehandlingerPåFagsak(fagsakId = fagsak.id)
            .filter { it.erAvsluttet() && !it.erHenlagt() }
    }

    fun hentSisteBehandlingSomErAvsluttetEllerSendtTilØkonomiPerFagsak(fagsakIder: Set<Long>): List<Behandling> {
        val behandlingerPåFagsakene = behandlingRepository.finnBehandlinger(fagsakIder)

        return behandlingerPåFagsakene
            .groupBy { it.fagsak.id }
            .mapNotNull { (_, behandling) -> behandling.filtrerSisteBehandlingSomErAvsluttetEllerSendtTilØkonomi() }
    }

    private fun List<Behandling>.filtrerSisteBehandlingSomErAvsluttetEllerSendtTilØkonomi() =
        filter { !it.erHenlagt() && (it.status == BehandlingStatus.AVSLUTTET || it.status == BehandlingStatus.IVERKSETTER_VEDTAK) }
            .maxByOrNull { it.opprettetTidspunkt }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
