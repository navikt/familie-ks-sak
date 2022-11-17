package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.TriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import java.time.LocalDate

data class BrevBegrunnelseGrunnlagMedPersoner(
    val standardbegrunnelse: Standardbegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
    val personIdenter: List<String>,
    val avtaletidspunktDeltBosted: LocalDate? = null
) {
    fun hentAntallBarnForBegrunnelse(
        uregistrerteBarn: List<BrevtUregistrertBarn>,
        gjelderSøker: Boolean,
        barnasFødselsdatoer: List<LocalDate>
    ): Int {
        val erAvslagUregistrerteBarn =
            this.standardbegrunnelse == Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN

        return when {
            erAvslagUregistrerteBarn -> uregistrerteBarn.size
            gjelderSøker && this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG -> 0
            else -> barnasFødselsdatoer.size
        }
    }

    fun hentBarnasFødselsdagerForBegrunnelse(
        uregistrerteBarn: List<BrevtUregistrertBarn>,
        personerIBehandling: List<BrevtUregistrertBarn>,
        personerPåBegrunnelse: List<BrevtUregistrertBarn>,
        personerMedUtbetaling: List<BrevtUregistrertBarn>
    ) = when {
        this.standardbegrunnelse == Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN ->
            uregistrerteBarn.mapNotNull { it.fødselsdato }

        this.vedtakBegrunnelseType != VedtakBegrunnelseType.ENDRET_UTBETALING && this.vedtakBegrunnelseType != VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> {
            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG) {
                personerIBehandling
                    .map { it.fødselsdato } +
                    uregistrerteBarn.mapNotNull { it.fødselsdato }
            } else {
                (personerMedUtbetaling + personerPåBegrunnelse).toSet()
                    .map { it.fødselsdato }
            }
        }

        else ->
            personerPåBegrunnelse
                .map { it.fødselsdato }
    }
}
