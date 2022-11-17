package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevBehandlingsGrunnlag
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevtUregistrertBarn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform

data class GrunnlagForBrevperiode(
    val brevBehandlingsGrunnlag: BrevBehandlingsGrunnlag,
    val erFørsteVedtaksperiodePåFagsak: Boolean,
    val uregistrerteBarn: List<BrevtUregistrertBarn>,
    val brevMålform: Målform,
    val brevVedtaksPeriode: BrevVedtaksPeriode,
    val barnMedReduksjonFraForrigeBehandlingIdent: List<String>,
    val dødeBarnForrigePeriode: List<String>
) : Comparable<GrunnlagForBrevperiode> {

    fun hentBegrunnelserOgFritekster(): List<Begrunnelse> {
        val brevPeriodeGenereator = BrevPeriodeGenerator(this)
        // TODO lag begrunnelser og fritekster slik at de kan forhåndsvises i frontend
        return emptyList()
    }

    override fun compareTo(other: GrunnlagForBrevperiode): Int {
        val fomCompared = (this.brevVedtaksPeriode.fom ?: TIDENES_MORGEN)
            .compareTo(other.brevVedtaksPeriode.fom ?: TIDENES_MORGEN)

        return when {
            this.brevVedtaksPeriode.type == Vedtaksperiodetype.AVSLAG &&
                other.brevVedtaksPeriode.type == Vedtaksperiodetype.AVSLAG -> fomCompared

            this.brevVedtaksPeriode.type == Vedtaksperiodetype.AVSLAG -> 1
            other.brevVedtaksPeriode.type == Vedtaksperiodetype.AVSLAG -> -1
            else -> fomCompared
        }
    }
}
