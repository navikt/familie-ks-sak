package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPersonResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVilkårResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersonResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersoner
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

class FinnGyldigeBegrunnelserForPeriodeContext(
    private val brevVedtaksPeriode: BrevVedtaksPeriode,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val brevPersoner: List<BrevPerson>,
    private val brevPersonResultater: List<BrevPersonResultat>,
    private val aktørIderMedUtbetaling: List<String>
) {

    private val vedtaksperiode = Periode(
        fom = brevVedtaksPeriode.fom ?: TIDENES_MORGEN,
        tom = brevVedtaksPeriode.tom ?: TIDENES_ENDE
    )

    constructor(
        utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
        sanityBegrunnelser: List<SanityBegrunnelse>,
        persongrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        aktørIderMedUtbetaling: List<String>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
    ) : this(
        brevVedtaksPeriode = utvidetVedtaksperiodeMedBegrunnelser.tilBrevVedtaksPeriode(),
        sanityBegrunnelser = sanityBegrunnelser,
        brevPersoner = persongrunnlag.tilBrevPersoner(),
        brevPersonResultater = vilkårsvurdering.personResultater
            .map { it.tilBrevPersonResultat() },
        aktørIderMedUtbetaling = aktørIderMedUtbetaling
    )

    fun hentGyldigeBegrunnelserForVedtaksperiode(): List<Standardbegrunnelse> {
        val tillateBegrunnelserForVedtakstype = Standardbegrunnelse.values()
            .filter {
                brevVedtaksPeriode
                    .type
                    .tillatteBegrunnelsestyper
                    .contains(it.vedtakBegrunnelseType)
            }

        return when (brevVedtaksPeriode.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET,
            Vedtaksperiodetype.AVSLAG -> tillateBegrunnelserForVedtakstype

            Vedtaksperiodetype.UTBETALING,
            Vedtaksperiodetype.OPPHØR -> velgUtbetalingsbegrunnelser(
                tillateBegrunnelserForVedtakstype
            )
        }
    }

    private fun velgUtbetalingsbegrunnelser(
        tillateBegrunnelserForVedtakstype: List<Standardbegrunnelse>
    ): List<Standardbegrunnelse> {
        val standardbegrunnelser =
            tillateBegrunnelserForVedtakstype
                .filter { it.vedtakBegrunnelseType != VedtakBegrunnelseType.FORTSATT_INNVILGET }
                .filter { it.triggesForPeriode() }

        val fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget =
            brevVedtaksPeriode.type == Vedtaksperiodetype.UTBETALING && standardbegrunnelser.isEmpty()

        return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
            tillateBegrunnelserForVedtakstype
                .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
        } else {
            standardbegrunnelser
        }
    }

    private fun Standardbegrunnelse.triggesForPeriode(): Boolean {
        val sanityBegrunnelse = this.tilSanityBegrunnelse(sanityBegrunnelser) ?: return false

        val vilkårResultaterSomPasserVedtaksperioden: Map<String, List<BrevVilkårResultat>> =
            this.filtrerPåPeriodeGittVedtakBegrunnelseType()
                .filtrerPåVilkårType(sanityBegrunnelse.vilkår)
                .filtrerPåTriggere(sanityBegrunnelse.triggere)
                .filtrerPåUtdypendeVilkårsvurdering(sanityBegrunnelse.utdypendeVilkårsvurdering)

        return vilkårResultaterSomPasserVedtaksperioden.isNotEmpty()
    }

    private fun Standardbegrunnelse.filtrerPåPeriodeGittVedtakBegrunnelseType() =
        when (this.vedtakBegrunnelseType) {
            VedtakBegrunnelseType.REDUKSJON,
            VedtakBegrunnelseType.EØS_INNVILGET,
            VedtakBegrunnelseType.AVSLAG,
            VedtakBegrunnelseType.ENDRET_UTBETALING,
            VedtakBegrunnelseType.INNVILGET -> finnPersonerMedVilkårResultaterSomStarterSamtidigSomPeriode()

            VedtakBegrunnelseType.EØS_OPPHØR,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.OPPHØR -> finnPersonerMedVilkårResultaterSomSlutterFørPeriode()

            VedtakBegrunnelseType.FORTSATT_INNVILGET -> throw Feil("FORTSATT_INNVILGET skal være filtrert bort.")
        }

    private fun Map<String, List<BrevVilkårResultat>>.filtrerPåVilkårType(vilkårTyper: List<Vilkår>) =
        this.mapValues { (_, value) ->
            value.filter { vilkårTyper.contains(it.vilkårType) }
        }.filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomStarterSamtidigSomPeriode(): Map<String, List<BrevVilkårResultat>> =

        brevPersonResultater.associate { brevPersonResultat ->
            Pair(
                brevPersonResultat.aktør.aktivFødselsnummer(),
                brevPersonResultat.brevVilkårResultater.filter {
                    it.periodeFom == vedtaksperiode.fom
                }
            )
        }.filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomSlutterFørPeriode(): Map<String, List<BrevVilkårResultat>> =
        brevPersonResultater.associate { brevPersonResultat ->
            Pair(
                brevPersonResultat.aktør.aktivFødselsnummer(),
                brevPersonResultat.brevVilkårResultater.filter {
                    it.periodeTom?.plusDays(1) == vedtaksperiode.fom
                }
            )
        }.filterValues { it.isNotEmpty() }

    private fun Map<String, List<BrevVilkårResultat>>.filtrerPåTriggere(triggere: List<Trigger>) =
        this.mapValues { (aktivtFødselsnummer, brevVilkårResultater) ->
            val person = brevPersoner.find { it.aktivPersonIdent == aktivtFødselsnummer }!!

            brevVilkårResultater.filter { brevVilkårResultat ->
                Trigger.values().filter { it.erOppfylt(brevVilkårResultat, person) } == triggere
            }
        }.filterValues { it.isNotEmpty() }

    private fun Map<String, List<BrevVilkårResultat>>.filtrerPåUtdypendeVilkårsvurdering(utdypendeVilkårsvurdering: List<UtdypendeVilkårsvurdering>) =
        this.mapValues { (_, value) -> value.filter { it.utdypendeVilkårsvurderinger == utdypendeVilkårsvurdering } }
            .filterValues { it.isNotEmpty() }
}
