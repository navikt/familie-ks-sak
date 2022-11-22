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
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

class FinnGyldigeBegrunnelserForPeriodeContext(
    private val utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val persongrunnlag: PersonopplysningGrunnlag,
    private val personResultater: Map<Aktør, Tidslinje<List<VilkårResultat>>>,
    private val aktørIderMedUtbetaling: List<String>
) {

    private val vedtaksperiode = Periode(
        fom = utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
        tom = utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
    )

    fun hentGyldigeBegrunnelserForVedtaksperiode(): List<Standardbegrunnelse> {
        val tillateBegrunnelserForVedtakstype = Standardbegrunnelse.values()
            .filter {
                utvidetVedtaksperiodeMedBegrunnelser
                    .type
                    .tillatteBegrunnelsestyper
                    .contains(it.vedtakBegrunnelseType)
            }

        return when (utvidetVedtaksperiodeMedBegrunnelser.type) {
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
            utvidetVedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.UTBETALING && standardbegrunnelser.isEmpty()

        return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
            filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
        } else {
            standardbegrunnelser
        }
    }

    private fun Standardbegrunnelse.triggesForPeriode(): Boolean {
        val sanityBegrunnelse = this.tilSanityBegrunnelse(sanityBegrunnelser) ?: return false

        val vilkårResultaterSomPasserVedtaksperioden: Map<Person, List<VilkårResultat>> =
            this.filtrerPåPeriodeGittVedtakBegrunnelseType()
                .filtrerPersonerUtenUtbetalingVedInnvilget(this.vedtakBegrunnelseType)
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

    private fun finnPersonerMedVilkårResultaterSomStarterSamtidigSomPeriode(): Map<Person, List<VilkårResultat>> =

        personResultater.mapNotNull { (aktør, vilkårResultatTidslinjeForPerson) ->
            val person = persongrunnlag.personer.find { it.aktør.aktivFødselsnummer() == aktør.aktivFødselsnummer() }
            val forskøvedeVilkårResultaterMedSammeFom =
                vilkårResultatTidslinjeForPerson.tilPerioderIkkeNull().singleOrNull {
                    it.fom == vedtaksperiode.fom
                }?.verdi
            if (person != null && forskøvedeVilkårResultaterMedSammeFom != null) {
                Pair(person, forskøvedeVilkårResultaterMedSammeFom)
            } else {
                null
            }
        }.toMap().filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomSlutterFørPeriode(): Map<Person, List<VilkårResultat>> =
        personResultater.mapNotNull { (aktør, tidsjlinje) ->
            val person = persongrunnlag.personer.find { it.aktør.aktivFødselsnummer() == aktør.aktivFødselsnummer() }
            val forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode =
                tidsjlinje.tilPerioderIkkeNull().singleOrNull {
                    it.tom?.plusDays(1) == vedtaksperiode.fom
                }?.verdi

            if (person != null && forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode != null) {
                Pair(person, forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode)
            } else {
                null
            }
        }.toMap().filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPersonerUtenUtbetalingVedInnvilget(vedtakBegrunnelseType: VedtakBegrunnelseType) =
        this.filterKeys {
            if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
                aktørIderMedUtbetaling.contains(it.aktør.aktørId) || it.type == PersonType.SØKER
            } else {
                true
            }
        }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåVilkårType(vilkårTyperFraSanity: List<Vilkår>) =
        this.mapValues { (_, vilkårResultaterForPerson) ->
            vilkårResultaterForPerson.filter { vilkårTyperFraSanity.contains(it.vilkårType) }
        }.filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåTriggere(
        triggereFraSanity: List<Trigger>
    ) = this.filter { (person, vilkårResultaterForPerson) ->
        Trigger.values().filter { it.erOppfylt(vilkårResultaterForPerson, person) } == triggereFraSanity
    }.filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåUtdypendeVilkårsvurdering(utdypendeVilkårFraSanity: List<UtdypendeVilkårsvurdering>) =
        this.filterValues { vilkårResultaterForPerson ->
            val utdypendeVilkårIBehandling =
                vilkårResultaterForPerson.flatMap { it.utdypendeVilkårsvurderinger }.toSet()

            if (utdypendeVilkårIBehandling.isEmpty()) {
                utdypendeVilkårFraSanity.toSet() == utdypendeVilkårIBehandling
            } else {
                utdypendeVilkårIBehandling.all { utdypendeVilkårFraSanity.contains(it) }
            }
        }
}
