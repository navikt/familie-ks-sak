package no.nav.familie.ks.sak.kjerne.totrinnskontroll

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TotrinnskontrollService(private val totrinnskontrollRepository: TotrinnskontrollRepository) {

    fun finnAktivForBehandling(behandlingId: Long): Totrinnskontroll? =
        totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)

    fun hentAktivForBehandling(behandlingId: Long): Totrinnskontroll =
        totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)
            ?: throw Feil("Fant ikke aktiv totrinnskontroll for behandling $behandlingId")

    fun opprettTotrinnskontrollMedSaksbehandler(
        behandling: Behandling,
        saksbehandler: String = SikkerhetContext.hentSaksbehandlerNavn(),
        saksbehandlerId: String = SikkerhetContext.hentSaksbehandler(),
    ): Totrinnskontroll = lagreOgDeaktiverGammel(
        Totrinnskontroll(
            behandling = behandling,
            saksbehandler = saksbehandler,
            saksbehandlerId = saksbehandlerId,
        ),
    )

    fun besluttTotrinnskontroll(
        behandlingId: Long,
        beslutter: String,
        beslutterId: String,
        beslutning: Beslutning,
        kontrollerteSider: List<String> = emptyList(),
    ): Totrinnskontroll {
        val totrinnskontroll = hentAktivForBehandling(behandlingId)

        totrinnskontroll.beslutter = beslutter
        totrinnskontroll.beslutterId = beslutterId
        totrinnskontroll.godkjent = beslutning.erGodkjent()
        totrinnskontroll.kontrollerteSider = kontrollerteSider

        if (totrinnskontroll.erUgyldig()) {
            throw FunksjonellFeil(
                melding = "Samme saksbehandler kan ikke foreslå og beslutte iverksetting på samme vedtak",
                frontendFeilmelding = "Du kan ikke godkjenne ditt eget vedtak",
            )
        }

        totrinnskontrollRepository.save(totrinnskontroll)

        return totrinnskontroll
    }

    fun lagreOgDeaktiverGammel(totrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        val aktivTotrinnskontroll = finnAktivForBehandling(totrinnskontroll.behandling.id)

        if (aktivTotrinnskontroll != null && aktivTotrinnskontroll.id != totrinnskontroll.id) {
            totrinnskontrollRepository.saveAndFlush(aktivTotrinnskontroll.also { it.aktiv = false })
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter totrinnskontroll $totrinnskontroll")
        return totrinnskontrollRepository.save(totrinnskontroll)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TotrinnskontrollService::class.java)
    }
}
