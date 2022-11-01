package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.TriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertRestPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertUregistrertBarn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.time.LocalDate

data class BrevBegrunnelseGrunnlagMedPersoner(
    val standardbegrunnelse: Standardbegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
    val personIdenter: List<String>,
    val avtaletidspunktDeltBosted: LocalDate? = null
) {
    fun hentAntallBarnForBegrunnelse(
        uregistrerteBarn: List<MinimertUregistrertBarn>,
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
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        gjelderSøker: Boolean,
        personerIBehandling: List<MinimertRestPerson>,
        personerPåBegrunnelse: List<MinimertRestPerson>,
        personerMedUtbetaling: List<MinimertRestPerson>
    ) = when {
        this.standardbegrunnelse == Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN ->
            uregistrerteBarn.mapNotNull { it.fødselsdato }

        gjelderSøker && this.vedtakBegrunnelseType != VedtakBegrunnelseType.ENDRET_UTBETALING && this.vedtakBegrunnelseType != VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> {
            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG) {
                personerIBehandling
                    .filter { it.type == PersonType.BARN }
                    .map { it.fødselsdato } +
                    uregistrerteBarn.mapNotNull { it.fødselsdato }
            } else {
                (personerMedUtbetaling + personerPåBegrunnelse).toSet()
                    .filter { it.type == PersonType.BARN }
                    .map { it.fødselsdato }
            }
        }
        else ->
            personerPåBegrunnelse
                .filter { it.type == PersonType.BARN }
                .map { it.fødselsdato }
    }
}
