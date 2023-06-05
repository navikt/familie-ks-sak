package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.klipp
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.integrasjon.sanity.domene.inneholderGjelderFørstePeriodeTrigger
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvVilkårResultater
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilFørskjøvetVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

class BegrunnelserForPeriodeContext(
    private val utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val personopplysningGrunnlag: PersonopplysningGrunnlag,
    private val personResultater: List<PersonResultat>,
    private val endretUtbetalingsandeler: List<EndretUtbetalingAndel>,
    private val erFørsteVedtaksperiode: Boolean
) {

    private val aktørIderMedUtbetaling =
        utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.map { it.person.aktør.aktørId }

    private val vedtaksperiode = Periode(
        fom = utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
        tom = utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
    )

    fun hentGyldigeBegrunnelserForVedtaksperiode(): List<Begrunnelse> {
        val tillateBegrunnelserForVedtakstype = Begrunnelse.values()
            .filter {
                utvidetVedtaksperiodeMedBegrunnelser
                    .type
                    .tillatteBegrunnelsestyper
                    .contains(it.begrunnelseType)
            }

        return when (utvidetVedtaksperiodeMedBegrunnelser.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET,
            Vedtaksperiodetype.AVSLAG -> tillateBegrunnelserForVedtakstype

            Vedtaksperiodetype.UTBETALING,
            Vedtaksperiodetype.OPPHØR -> tillateBegrunnelserForVedtakstype.filtrerPasserVedtaksperiode()
        }
    }

    private fun List<Begrunnelse>.filtrerPasserVedtaksperiode(): List<Begrunnelse> {
        val begrunnelserSomTriggesForVedtaksperiode =
            filter { it.begrunnelseType != BegrunnelseType.FORTSATT_INNVILGET }
                .filter { it.triggesForVedtaksperiode() }

        val fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget =
            utvidetVedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.UTBETALING && begrunnelserSomTriggesForVedtaksperiode.isEmpty()

        return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
            filter { it.begrunnelseType == BegrunnelseType.FORTSATT_INNVILGET }
        } else {
            begrunnelserSomTriggesForVedtaksperiode
        }
    }

    private fun Begrunnelse.triggesForVedtaksperiode(): Boolean {
        val sanityBegrunnelse = this.tilSanityBegrunnelse(sanityBegrunnelser) ?: return false

        return when {
            sanityBegrunnelse.skalAlltidVises -> true
            sanityBegrunnelse.endretUtbetalingsperiode.isNotEmpty() -> erEtterEndretPeriodeAvSammeÅrsak(
                sanityBegrunnelse
            )

            else -> hentPersonerMedVilkårResultaterSomPasserMedBegrunnelseOgPeriode(
                this,
                sanityBegrunnelse
            ).isNotEmpty()
        }
    }

    private fun erEtterEndretPeriodeAvSammeÅrsak(begrunnelse: SanityBegrunnelse) =
        endretUtbetalingsandeler.any { endretUtbetalingAndel ->

            val endringsperiodeErDagenEtterVedtaksperiode =
                endretUtbetalingAndel.tom?.sisteDagIInneværendeMåned()
                    ?.erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom) ?: false

            val endringsperiodeGjelderSammePersonSomVedtaksperiode =
                personResultater.any { person -> person.aktør.aktørId == endretUtbetalingAndel.person?.aktør?.aktørId }

            val begrunnelseHarSammeÅrsakSomEndringsperiode =
                begrunnelse.endringsårsaker.contains(endretUtbetalingAndel.årsak)

            endringsperiodeErDagenEtterVedtaksperiode &&
                endringsperiodeGjelderSammePersonSomVedtaksperiode &&
                begrunnelseHarSammeÅrsakSomEndringsperiode
        }

    fun hentPersonerMedVilkårResultaterSomPasserMedBegrunnelseOgPeriode(
        begrunnelse: Begrunnelse,
        sanityBegrunnelse: SanityBegrunnelse
    ): Set<Person> {
        val erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger =
            erFørsteVedtaksperiode && sanityBegrunnelse.inneholderGjelderFørstePeriodeTrigger()

        val hentVilkårResultaterSomOverlapperVedtaksperiode =
            hentVilkårResultaterSomOverlapperVedtaksperiode(
                begrunnelse,
                erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger
            )

        val filtrerPersonerUtenUtbetalingVedInnvilget = hentVilkårResultaterSomOverlapperVedtaksperiode
            .filtrerPersonerUtenUtbetalingVedInnvilget(begrunnelse.begrunnelseType)

        val filtrerPåVilkårType = filtrerPersonerUtenUtbetalingVedInnvilget
            .filtrerPåVilkårType(sanityBegrunnelse.vilkår)

        val filtrerPåTriggere = filtrerPåVilkårType
            .filtrerPåTriggere(
                sanityBegrunnelse.triggere,
                sanityBegrunnelse.type,
                erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger,
                begrunnelse.begrunnelseType
            )

        val filtrerPåUtdypendeVilkårsvurdering = filtrerPåTriggere
            .filtrerPåUtdypendeVilkårsvurdering(
                sanityBegrunnelse.utdypendeVilkårsvurderinger,
                sanityBegrunnelse.type
            )

        val filtrerPåVilkårResultaterSomPasserMedVedtaksperiodeDatoEllerSanityBegrunnelseType =
            filtrerPåUtdypendeVilkårsvurdering
                .filtrerPåVilkårResultaterSomPasserMedVedtaksperiodeDatoEllerSanityBegrunnelseType(
                    begrunnelse.finnVilkårResultatIderSomPasserMedVedtaksperiodeDato(),
                    sanityBegrunnelse.type
                )

        val personerMedVilkårResultaterSomPasserVedtaksperioden: Map<Person, List<VilkårResultat>> =
            filtrerPåVilkårResultaterSomPasserMedVedtaksperiodeDatoEllerSanityBegrunnelseType

        return personerMedVilkårResultaterSomPasserVedtaksperioden.keys
    }

    fun hentPersonerMedEndretUtbetalingerSomPasserMedVedtaksperiode(sanityBegrunnelse: SanityBegrunnelse): Set<Person> =
        endretUtbetalingsandeler.filter { endretUtbetalingAndel ->
            endretUtbetalingAndel.periode.tom.sisteDagIInneværendeMåned()
                .erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom) &&
                sanityBegrunnelse.endringsårsaker.contains(endretUtbetalingAndel.årsak)
        }.mapNotNull { it.person }.toSet()

    private fun Map<Person, List<VilkårResultat>>.filtrerPåVilkårResultaterSomPasserMedVedtaksperiodeDatoEllerSanityBegrunnelseType(
        vilkårResultaterSomPasserMedVedtaksperiodeDato: List<Long>,
        begrunnelseType: SanityBegrunnelseType
    ) = this.mapValues {
        it.value.filter { vilkårResultat ->
            vilkårResultaterSomPasserMedVedtaksperiodeDato.contains(
                vilkårResultat.id
            ) || begrunnelseType == SanityBegrunnelseType.STANDARD
        }
    }.filterValues { it.isNotEmpty() }

    private fun Begrunnelse.finnVilkårResultatIderSomPasserMedVedtaksperiodeDato() =
        when (this.begrunnelseType) {
            BegrunnelseType.REDUKSJON,
            BegrunnelseType.EØS_INNVILGET,
            BegrunnelseType.AVSLAG,
            BegrunnelseType.ENDRET_UTBETALING,
            BegrunnelseType.INNVILGET -> finnVilkårResultaterSomStarterSamtidigSomPeriode()

            BegrunnelseType.EØS_OPPHØR,
            BegrunnelseType.ETTER_ENDRET_UTBETALING,
            BegrunnelseType.OPPHØR -> finnVilkårResultaterSomSlutterFørPeriode()

            BegrunnelseType.FORTSATT_INNVILGET -> throw Feil("FORTSATT_INNVILGET skal være filtrert bort.")
        }

    private fun finnVilkårResultaterSomStarterSamtidigSomPeriode() =
        personResultater.flatMap { personResultat ->
            val vilkårTilVilkårResultaterMap = personResultat.vilkårResultater.groupBy { it.vilkårType }

            vilkårTilVilkårResultaterMap.mapValues { (vilkår, vilkårResultater) ->
                forskyvVilkårResultater(vilkår, vilkårResultater).filter { it.fom == vedtaksperiode.fom }
                    .map { it.verdi.id }
            }.filterValues { it.isNotEmpty() }.flatMap { it.value }
        }

    private fun finnVilkårResultaterSomSlutterFørPeriode() =
        personResultater.flatMap { personResultat ->
            val vilkårTilVilkårResultaterMap = personResultat.vilkårResultater.groupBy { it.vilkårType }

            vilkårTilVilkårResultaterMap.mapValues { (vilkår, vilkårResultater) ->
                forskyvVilkårResultater(vilkår, vilkårResultater).filter { it.tom?.plusDays(1) == vedtaksperiode.fom }
                    .map { it.verdi.id }
            }.filterValues { it.isNotEmpty() }.flatMap { it.value }
        }

    private fun hentVilkårResultaterSomOverlapperVedtaksperiode(
        standardBegrunnelse: Begrunnelse,
        erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger: Boolean
    ) =
        when (standardBegrunnelse.begrunnelseType) {
            BegrunnelseType.REDUKSJON,
            BegrunnelseType.EØS_INNVILGET,
            BegrunnelseType.ENDRET_UTBETALING,
            BegrunnelseType.INNVILGET -> finnPersonerMedVilkårResultaterSomGjelderIPeriode()

            BegrunnelseType.AVSLAG -> finnPersonerSomHarIkkeOppfylteVilkårResultaterSomStarterSamtidigSomPeriode()

            BegrunnelseType.EØS_OPPHØR,
            BegrunnelseType.ETTER_ENDRET_UTBETALING,
            BegrunnelseType.OPPHØR -> {
                if (erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger) finnPersonerMedVilkårResultatIFørsteVedtaksperiodeSomIkkeErOppfylt() else finnPersonerMedVilkårResultaterSomGjelderRettFørPeriode()
            }

            BegrunnelseType.FORTSATT_INNVILGET -> throw Feil("FORTSATT_INNVILGET skal være filtrert bort.")
        }

    private fun finnPersonerSomHarIkkeOppfylteVilkårResultaterSomStarterSamtidigSomPeriode(): Map<Person, List<VilkårResultat>> =
        personResultater.mapNotNull { personResultat ->
            val person = personopplysningGrunnlag.personer.find { it.aktør == personResultat.aktør }

            val vilkårResultater = personResultat.vilkårResultater
            val ikkeOppfylteVilkårSomStarterISammePeriode = vilkårResultater
                .filter { it.resultat == Resultat.IKKE_OPPFYLT }
                .filter {
                    (it.periodeFom ?: TIDENES_MORGEN).toYearMonth() == vedtaksperiode.fom.toYearMonth()
                }

            if (person != null && ikkeOppfylteVilkårSomStarterISammePeriode.isNotEmpty()) {
                Pair(person, ikkeOppfylteVilkårSomStarterISammePeriode)
            } else {
                null
            }
        }.toMap()

    private fun finnPersonerMedVilkårResultatIFørsteVedtaksperiodeSomIkkeErOppfylt(): Map<Person, List<VilkårResultat>> =
        personResultater.tilFørskjøvetVilkårResultatTidslinjeMap(personopplysningGrunnlag)
            .mapNotNull { (aktør, vilkårResultatTidslinjeForPerson) ->

                val person =
                    personopplysningGrunnlag.personer.find { it.aktør.aktivFødselsnummer() == aktør.aktivFødselsnummer() }

                val forskøvedeVilkårResultaterSomIkkeErOppfyltEllerOppfyltRettEtterPeriode =
                    vilkårResultatTidslinjeForPerson.klipp(vedtaksperiode.fom, vedtaksperiode.tom).tilPerioderIkkeNull()
                        .filter { vilkårResultat ->
                            val vilkårResultatIkkeOppfyltForPeriode = vilkårResultat.verdi.any { !it.erOppfylt() }
                            val vilkårResultatOppfyltRettEtterPeriode =
                                vilkårResultat.verdi.any { it.erOppfylt() && it.periodeTom?.toYearMonth() == vedtaksperiode.fom.toYearMonth() }

                            vilkårResultatIkkeOppfyltForPeriode || vilkårResultatOppfyltRettEtterPeriode
                        }.flatMap { it.verdi }

                if (person != null && forskøvedeVilkårResultaterSomIkkeErOppfyltEllerOppfyltRettEtterPeriode.isNotEmpty()) {
                    Pair(person, forskøvedeVilkårResultaterSomIkkeErOppfyltEllerOppfyltRettEtterPeriode)
                } else {
                    null
                }
            }.toMap().filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomGjelderIPeriode(): Map<Person, List<VilkårResultat>> =
        personResultater.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag)
            .mapNotNull { (aktør, vilkårResultatTidslinjeForPerson) ->

                val person =
                    personopplysningGrunnlag.personer.find { it.aktør.aktivFødselsnummer() == aktør.aktivFødselsnummer() }
                val forskøvedeVilkårResultaterMedSammeFom =
                    vilkårResultatTidslinjeForPerson.klipp(vedtaksperiode.fom, vedtaksperiode.tom).tilPerioderIkkeNull()
                        .singleOrNull {
                            it.fom == vedtaksperiode.fom
                        }?.verdi
                if (person != null && forskøvedeVilkårResultaterMedSammeFom != null) {
                    Pair(person, forskøvedeVilkårResultaterMedSammeFom)
                } else {
                    null
                }
            }.toMap().filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomGjelderRettFørPeriode(): Map<Person, List<VilkårResultat>> =
        personResultater.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag)
            .mapNotNull { (aktør, tidslinje) ->
                val person =
                    personopplysningGrunnlag.personer.find { it.aktør.aktivFødselsnummer() == aktør.aktivFødselsnummer() }
                val forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode =
                    tidslinje.tilPerioderIkkeNull().singleOrNull {
                        it.tom?.plusDays(1) == vedtaksperiode.fom
                    }?.verdi

                if (person != null && forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode != null) {
                    Pair(person, forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode)
                } else {
                    null
                }
            }.toMap().filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPersonerUtenUtbetalingVedInnvilget(begrunnelseType: BegrunnelseType) =
        this.filterKeys {
            begrunnelseType != BegrunnelseType.INNVILGET ||
                aktørIderMedUtbetaling.contains(it.aktør.aktørId) ||
                it.type == PersonType.SØKER
        }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåVilkårType(vilkårTyperFraSanity: List<Vilkår>) =
        this.mapValues { (_, vilkårResultaterForPerson) ->
            vilkårResultaterForPerson.filter { vilkårTyperFraSanity.contains(it.vilkårType) }
        }.filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåTriggere(
        triggereFraSanity: List<Trigger>,
        sanityBegrunnelseType: SanityBegrunnelseType,
        erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger: Boolean,
        begrunnelseType: BegrunnelseType
    ) = this.filter { (person, vilkårResultaterForPerson) ->
        val oppfylteTriggereIBehandling =
            Trigger.values().filter {
                it.erOppfylt(
                    vilkårResultaterForPerson,
                    person,
                    erFørsteVedtaksperiodeOgBegrunnelseInneholderGjelderFørstePeriodeTrigger
                )
            }

        // Strengere logikk for Standardbegrunnelsene for innvilgelse
        if (sanityBegrunnelseType == SanityBegrunnelseType.STANDARD && begrunnelseType == BegrunnelseType.INNVILGET) {
            oppfylteTriggereIBehandling == triggereFraSanity
        } else {
            triggereFraSanity.all { oppfylteTriggereIBehandling.contains(it) }
        }
    }.filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåUtdypendeVilkårsvurdering(
        utdypendeVilkårFraSanity: List<UtdypendeVilkårsvurdering>,
        sanityBegrunnelseType: SanityBegrunnelseType
    ) =
        this.filterValues { vilkårResultaterForPerson ->
            val utdypendeVilkårIBehandling =
                vilkårResultaterForPerson.flatMap { it.utdypendeVilkårsvurderinger }.toSet()

            if (sanityBegrunnelseType == SanityBegrunnelseType.STANDARD) {
                utdypendeVilkårIBehandling == utdypendeVilkårFraSanity.toSet()
            } else {
                utdypendeVilkårFraSanity.all { utdypendeVilkårIBehandling.contains(it) }
            }
        }
}
