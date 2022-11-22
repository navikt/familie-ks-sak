package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
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
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersoner
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import tilFørskjøvetVilkårResultatTidslinjeMap
import java.math.BigDecimal

class FinnGyldigeBegrunnelserForPeriodeContext(
    private val brevVedtaksPeriode: BrevVedtaksPeriode,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val brevPersoner: List<BrevPerson>,
    private val brevPersonResultater: Map<Aktør, Tidslinje<List<VilkårResultat>>>,
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
        aktørIderMedUtbetaling: List<String>
    ) : this(
        brevVedtaksPeriode = utvidetVedtaksperiodeMedBegrunnelser.tilBrevVedtaksPeriode(),
        sanityBegrunnelser = sanityBegrunnelser,
        brevPersoner = persongrunnlag.tilBrevPersoner(),
        brevPersonResultater = vilkårsvurdering.personResultater.tilFørskjøvetVilkårResultatTidslinjeMap(persongrunnlag),
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

        val vilkårResultaterForVedtaksperiode = this.filtrerPåPeriodeGittVedtakBegrunnelseType()

        val erDeltid =
            vilkårResultaterForVedtaksperiode.mapValues { entry ->
                entry.value.mapNotNull { it.antallTimer }.maxByOrNull { it }?.let {
                    it in BigDecimal.valueOf(0.01)..BigDecimal.valueOf(
                        32.99
                    )
                } ?: false
            }

        val vilkårResultaterSomPasserVedtaksperioden: Map<String, List<VilkårResultat>> =
            vilkårResultaterForVedtaksperiode
                .filtrerPåVilkårType(sanityBegrunnelse.vilkår)
                .filtrerPåTriggere(sanityBegrunnelse.triggere, erDeltid)
                .filtrerPåUtdypendeVilkårsvurdering(sanityBegrunnelse.utdypendeVilkårsvurderinger)

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

    private fun Map<String, List<VilkårResultat>>.filtrerPåVilkårType(vilkårTyper: List<Vilkår>) =
        this.mapValues { (_, value) ->
            value.filter { vilkårTyper.contains(it.vilkårType) }
        }.filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomStarterSamtidigSomPeriode(): Map<String, List<VilkårResultat>> =

        brevPersonResultater.mapNotNull { (aktør, vilkårResultatTidslinjeForPerson) ->
            val forskøvedeVilkårResultaterMedSammeFom =
                vilkårResultatTidslinjeForPerson.tilPerioderIkkeNull().singleOrNull {
                    it.fom == vedtaksperiode.fom
                }?.verdi

            forskøvedeVilkårResultaterMedSammeFom?.let {
                Pair(
                    aktør.aktivFødselsnummer(),
                    forskøvedeVilkårResultaterMedSammeFom
                )
            }
        }.toMap().filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomSlutterFørPeriode(): Map<String, List<VilkårResultat>> =
        brevPersonResultater.mapNotNull { (aktør, tidsjlinje) ->
            val forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode =
                tidsjlinje.tilPerioderIkkeNull().singleOrNull {
                    it.tom?.plusDays(1) == vedtaksperiode.fom
                }?.verdi

            forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode?.let {
                Pair(
                    aktør.aktivFødselsnummer(),
                    forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode
                )
            }
        }.toMap().filterValues { it.isNotEmpty() }

    private fun Map<String, List<VilkårResultat>>.filtrerPåTriggere(
        triggere: List<Trigger>,
        erDeltidMapPerson: Map<String, Boolean>
    ) =
        this.mapValues { (aktivtFødselsnummer, vilkårResultater) ->
            val person = brevPersoner.find { it.aktivPersonIdent == aktivtFødselsnummer }!!

            vilkårResultater.filter { vilkårResultat ->
                Trigger.values().filter { it.erOppfylt(erDeltidMapPerson[aktivtFødselsnummer]!!, person) } == triggere
            }
        }.filterValues { it.isNotEmpty() }

    private fun Map<String, List<VilkårResultat>>.filtrerPåUtdypendeVilkårsvurdering(utdypendeVilkårsvurdering: List<UtdypendeVilkårsvurdering>): Map<String, List<VilkårResultat>> =
        this.filterValues { value ->
            value.flatMap { it.utdypendeVilkårsvurderinger }.toSet() == utdypendeVilkårsvurdering.toSet()
        }
}
