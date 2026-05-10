package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ks.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service

@Service
class OpprettGrunnlagOgSignaturDataService(
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val saksbehandlerContext: SaksbehandlerContext,
) {
    fun opprett(vedtak: Vedtak): GrunnlagOgSignaturData {
        val behandling = vedtak.behandling
        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)
        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(behandlingId = behandling.id)
        val innloggetSaksbehandlerNavn = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

        val (saksbehandler, beslutter) = utledSaksbehandlerOgBeslutterSignatur(totrinnskontroll, behandling, innloggetSaksbehandlerNavn)

        val enhetNavn = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id).behandlendeEnhetNavn
        return GrunnlagOgSignaturData(
            grunnlag = personopplysningGrunnlag,
            saksbehandler = saksbehandler ?: innloggetSaksbehandlerNavn,
            beslutter = beslutter ?: "Beslutter",
            enhet = enhetNavn,
        )
    }

    private fun utledSaksbehandlerOgBeslutterSignatur(
        totrinnskontroll: Totrinnskontroll?,
        behandling: Behandling,
        innloggetSaksbehandlerNavn: String,
    ): Pair<String?, String?> =
        when {
            behandling.skalBehandlesAutomatisk() -> {
                Pair(SikkerhetContext.SYSTEM_NAVN, SikkerhetContext.SYSTEM_NAVN)
            }

            totrinnskontroll?.godkjent == true -> {
                Pair(totrinnskontroll.saksbehandler, totrinnskontroll.beslutter)
            }

            behandling.steg.sekvens > BehandlingSteg.BESLUTTE_VEDTAK.sekvens -> {
                Pair(innloggetSaksbehandlerNavn, "Beslutter")
            }

            behandling.steg == BehandlingSteg.BESLUTTE_VEDTAK -> {
                val beslutter =
                    if (totrinnskontroll?.saksbehandler == innloggetSaksbehandlerNavn) {
                        "Beslutter"
                    } else {
                        innloggetSaksbehandlerNavn
                    }
                Pair(totrinnskontroll?.saksbehandler, beslutter)
            }

            else -> {
                throw Feil("Kunne ikke utlede signatur for behandling ${behandling.id}")
            }
        }
}

class GrunnlagOgSignaturData(
    val grunnlag: PersonopplysningGrunnlag,
    val saksbehandler: String,
    val beslutter: String,
    val enhet: String,
)
