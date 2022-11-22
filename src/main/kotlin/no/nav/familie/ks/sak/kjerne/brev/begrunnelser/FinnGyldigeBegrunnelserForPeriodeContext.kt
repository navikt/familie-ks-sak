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
            Vedtaksperiodetype.OPPHØR -> tillateBegrunnelserForVedtakstype.filtrerPasserVedtaksperiode()
        }
    }

    private fun List<Standardbegrunnelse>.filtrerPasserVedtaksperiode(): List<Standardbegrunnelse> {
        val standardbegrunnelser =
            filter { it.vedtakBegrunnelseType != VedtakBegrunnelseType.FORTSATT_INNVILGET }
                .filter { it.triggesForPeriode() }

        val fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget =
            brevVedtaksPeriode.type == Vedtaksperiodetype.UTBETALING && standardbegrunnelser.isEmpty()

        return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
            filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
        } else {
            standardbegrunnelser
        }
    }

    private fun Standardbegrunnelse.triggesForPeriode(): Boolean {
        val sanityBegrunnelse = this.tilSanityBegrunnelse(sanityBegrunnelser) ?: return false

        val vilkårResultaterSomPasserVedtaksperioden: Map<String, List<VilkårResultat>> =
            this.filtrerPåPeriodeGittVedtakBegrunnelseType()
                .filtrerPåVilkårType(sanityBegrunnelse.vilkår)
                .filtrerPåTriggere(sanityBegrunnelse.triggere)
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

    private fun Map<String, List<VilkårResultat>>.filtrerPåVilkårType(vilkårTyperFraSanity: List<Vilkår>) =
        this.mapValues { (_, vilkårResultaterForPerson) ->
            vilkårResultaterForPerson.filter { vilkårTyperFraSanity.contains(it.vilkårType) }
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
        triggereFraSanity: List<Trigger>
    ) = this.filter { (aktivtFødselsnummer, vilkårResultaterForPerson) ->
        val person = brevPersoner.find { it.aktivPersonIdent == aktivtFødselsnummer }!!

        Trigger.values().filter { it.erOppfylt(vilkårResultaterForPerson, person) } == triggereFraSanity
    }.filterValues { it.isNotEmpty() }

    private fun Map<String, List<VilkårResultat>>.filtrerPåUtdypendeVilkårsvurdering(utdypendeVilkårFraSanity: List<UtdypendeVilkårsvurdering>): Map<String, List<VilkårResultat>> =
        this.filterValues { vilkårResultaterForPerson ->
            val utdypendeVilkårIBehandling =
                vilkårResultaterForPerson.flatMap { it.utdypendeVilkårsvurderinger }.toSet()

            utdypendeVilkårFraSanity.toSet().all { utdypendeVilkårIBehandling.contains(it) }
        }
}
