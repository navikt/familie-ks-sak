package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.tilBrev
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.formaterBeløp
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FeilutbetaltValuta
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.Avslag
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.Førstegangsvedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.OpphørMedEndring
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.Opphørt
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.VedtakEndring
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class GenererBrevService(
    private val brevKlient: BrevKlient,
    private val vedtakService: VedtakService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val simuleringService: SimuleringService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val brevPeriodeService: BrevPeriodeService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val sanityService: SanityService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val feilutbetaltValutaService: FeilutbetaltValutaService
) {

    fun genererManueltBrev(
        manueltBrevRequest: ManueltBrevDto,
        erForhåndsvisning: Boolean = false
    ): ByteArray {
        try {
            val brev = manueltBrevRequest.tilBrev()
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev
            )
        } catch (it: Exception) {
            if (it is Feil || it is FunksjonellFeil) {
                throw it
            } else {
                throw Feil(
                    message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal}. ${it.message}",
                    frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    throwable = it
                )
            }
        }
    }

    fun genererBrevForBehandling(behandlingId: Long): ByteArray {
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)
        try {
            if (vedtak.behandling.steg > BehandlingSteg.BESLUTTE_VEDTAK) {
                throw FunksjonellFeil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val målform = personopplysningGrunnlagService.hentSøkersMålform(vedtak.behandling.id)
            val vedtaksbrev =
                when (vedtak.behandling.opprettetÅrsak) {
                    BehandlingÅrsak.DØDSFALL -> TODO() // brevService.hentDødsfallbrevData(vedtak)
                    BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> TODO() // brevService.hentKorreksjonbrevData(vedtak)
                    else -> hentVedtaksbrevData(vedtak)
                }
            return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
        } catch (feil: Exception) {
            if (feil is FunksjonellFeil) throw feil

            throw Feil(
                message = "Klarte ikke generere vedtaksbrev på behandling ${vedtak.behandling}: ${feil.message}",
                frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = feil
            )
        }
    }

    fun hentVedtaksbrevData(vedtak: Vedtak): VedtaksbrevDto {
        val behandling = vedtak.behandling
        val brevtype = hentVedtaksbrevmal(behandling)
        val fellesdataForVedtaksbrev = lagDataForVedtaksbrev(vedtak)
        val etterbetaling = simuleringService.hentEtterbetaling(behandling.id)
            .takeIf { it > BigDecimal.ZERO }?.run { formaterBeløp(this.toInt()) }
            ?.let { Etterbetaling(it) }

        return when (brevtype) {
            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK -> {
                Førstegangsvedtak(
                    fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                    etterbetaling = etterbetaling
                )
            }

            Brevmal.VEDTAK_AVSLAG -> Avslag(fellesdataForVedtaksbrev = fellesdataForVedtaksbrev)

            Brevmal.VEDTAK_ENDRING -> VedtakEndring(
                fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                etterbetaling = etterbetaling,
                erKlage = behandling.erKlage(),
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                feilutbetaltValuta = feilutbetaltValutaService.beskrivPerioderMedFeilutbetaltValuta(behandling.id)
                    ?.let {
                        FeilutbetaltValuta(perioderMedForMyeUtbetalt = it)
                    }

            )

            Brevmal.VEDTAK_OPPHØRT -> Opphørt(
                fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id)
            )

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING -> OpphørMedEndring(
                fellesdataForVedtaksbrev = fellesdataForVedtaksbrev,
                etterbetaling = etterbetaling,
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
            )

            else -> throw Feil("Forsøker å hente vedtaksbrevdata for brevmal ${brevtype.visningsTekst}")
        }
    }

    fun lagDataForVedtaksbrev(vedtak: Vedtak): FellesdataForVedtaksbrev {
        val utvidetVedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(vedtak).filter {
                !(it.begrunnelser.isEmpty() && it.fritekster.isEmpty() && it.eøsBegrunnelser.isEmpty())
            }

        if (utvidetVedtaksperioderMedBegrunnelser.isEmpty()) {
            throw FunksjonellFeil(
                "Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet."
            )
        }

        val personopplysningsgrunnlagOgSignaturData = hentGrunnlagOgSignaturData(vedtak)

        val brevPeriodeDtoer = brevPeriodeService
            .hentBrevPeriodeDtoer(utvidetVedtaksperioderMedBegrunnelser, vedtak.behandling.id)

        val korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(vedtak.behandling.id)

        val hjemler = hentHjemler(
            behandlingId = vedtak.behandling.id,
            utvidetVedtaksperioderMedBegrunnelser = utvidetVedtaksperioderMedBegrunnelser,
            målform = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.målform,
            sanityBegrunnelser = sanityService.hentSanityBegrunnelser(),
            vedtakKorrigertHjemmelSkalMedIBrev = korrigertVedtak != null
        )

        return FellesdataForVedtaksbrev(
            enhet = personopplysningsgrunnlagOgSignaturData.enhet,
            saksbehandler = personopplysningsgrunnlagOgSignaturData.saksbehandler,
            beslutter = personopplysningsgrunnlagOgSignaturData.beslutter,
            hjemmeltekst = Hjemmeltekst(hjemler),
            søkerNavn = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.aktør.aktivFødselsnummer(),
            perioder = brevPeriodeDtoer,
            gjelder = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.navn,
            korrigertVedtakData = korrigertVedtak?.let { KorrigertVedtakData(datoKorrigertVedtak = it.vedtaksdato.tilDagMånedÅr()) }
        )
    }

    private fun hentGrunnlagOgSignaturData(vedtak: Vedtak): GrunnlagOgSignaturData {
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(vedtak.behandling.id)
        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(vedtak.behandling.id)
        val enhetNavn =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn

        return GrunnlagOgSignaturData(
            grunnlag = personopplysningGrunnlag,
            saksbehandler = totrinnskontroll?.saksbehandler ?: SikkerhetContext.hentSaksbehandlerNavn(),
            beslutter = totrinnskontroll?.beslutter ?: "Beslutter",
            enhet = enhetNavn
        )
    }

    private fun hentHjemler(
        behandlingId: Long,
        utvidetVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        målform: Målform,
        sanityBegrunnelser: List<SanityBegrunnelse>,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false
    ): String {
        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)

        val opplysningspliktHjemlerSkalMedIBrev =
            vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT

        return hentHjemmeltekst(
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            målform = målform,
            sanitybegrunnelserBruktIBrev = utvidetVedtaksperioderMedBegrunnelser.flatMap { it.begrunnelser }
                .mapNotNull { it.begrunnelse.tilSanityBegrunnelse(sanityBegrunnelser) },
            vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev
        )
    }

    private fun erFeilutbetalingPåBehandling(behandlingId: Long): Boolean =
        simuleringService.hentFeilutbetaling(behandlingId) > BigDecimal.ZERO

    private data class GrunnlagOgSignaturData(
        val grunnlag: PersonopplysningGrunnlag,
        val saksbehandler: String,
        val beslutter: String,
        val enhet: String
    )
}
