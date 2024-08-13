package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service

@Service
class OpprettGrunnlagOgSignaturDataService(
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) {
    fun opprett(vedtak: Vedtak): GrunnlagOgSignaturData {
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = vedtak.behandling.id)
        val totrinnskontroll =
            totrinnskontrollService.finnAktivForBehandling(behandlingId = vedtak.behandling.id)
        val enhetNavn =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = vedtak.behandling.id).behandlendeEnhetNavn
        return GrunnlagOgSignaturData(
            grunnlag = personopplysningGrunnlag,
            saksbehandler = totrinnskontroll?.saksbehandler ?: SikkerhetContext.hentSaksbehandlerNavn(),
            beslutter = totrinnskontroll?.beslutter ?: "Beslutter",
            enhet = enhetNavn,
        )
    }
}

class GrunnlagOgSignaturData(
    val grunnlag: PersonopplysningGrunnlag,
    val saksbehandler: String,
    val beslutter: String,
    val enhet: String,
)
