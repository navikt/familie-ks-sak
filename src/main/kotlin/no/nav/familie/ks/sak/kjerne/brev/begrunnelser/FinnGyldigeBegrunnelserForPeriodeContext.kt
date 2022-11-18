package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.TriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.erTriggereOppfyltForEndretUtbetaling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPersonResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVilkårResultat

class FinnGyldigeBegrunnelserForPeriodeContext(
    private val brevVedtaksPeriode: BrevVedtaksPeriode,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val brevPersoner: List<BrevPerson>,
    private val brevPersonResultater: List<BrevPersonResultat>,
    private val aktørIderMedUtbetaling: List<String>,
    private val brevEndretUtbetalingAndeler: List<BrevEndretUtbetalingAndel>,
    private val erFørsteVedtaksperiodePåFagsak: Boolean,
    private val ytelserForrigePerioder: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
) {
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

    private fun hentPersonFraAktørId(aktørId: String) = brevPersoner.find { it.aktørId == aktørId }

    private fun Standardbegrunnelse.triggesForPeriode(): Boolean {
        val sanityBegrunnelse = this.tilSanityBegrunnelse(sanityBegrunnelser) ?: return false

        val personer: Map<String, BrevVilkårResultat> = when (this.vedtakBegrunnelseType) {
            VedtakBegrunnelseType.REDUKSJON -> TODO()
            VedtakBegrunnelseType.INNVILGET -> finnPersonerForInnvilgelse(sanityBegrunnelse)
            VedtakBegrunnelseType.EØS_INNVILGET -> TODO()
            VedtakBegrunnelseType.AVSLAG -> TODO()
            VedtakBegrunnelseType.OPPHØR -> TODO()
            VedtakBegrunnelseType.EØS_OPPHØR -> TODO()
            VedtakBegrunnelseType.FORTSATT_INNVILGET -> TODO()
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> TODO()
            VedtakBegrunnelseType.ENDRET_UTBETALING -> TODO()
        }
        return personer.isNotEmpty()
    }

    private fun finnPersonerForInnvilgelse(sanityBegrunnelse: SanityBegrunnelse): Map<String, BrevVilkårResultat> =
        brevPersonResultater.flatMap { brevPersonResultat ->
            brevPersonResultat.brevVilkårResultater.filter {
                it.periodeFom == vedtaksperiode.fom
            }.associate { Pair(brevPersonResultat.aktør.aktivFødselsnummer(), it) }
        }

    fun hentPersonerForAlleUtgjørendeVilkår(
        triggesAv: TriggesAv,
        begrunnelsetype: VedtakBegrunnelseType,
        aktuellePersoner: List<BrevPerson>
    ): Set<BrevPerson> =
        triggesAv.vilkår.fold(setOf()) { acc, vilkår ->
            acc + hentPersonerMedUtgjørendeVilkår(
                begrunnelseType = begrunnelsetype,
                vilkårGjeldendeForBegrunnelse = vilkår,
                aktuellePersonerForVedtaksperiode = aktuellePersoner,
                triggesAv = triggesAv
            )
        }

    private fun hentPersonerMedUtgjørendeVilkår(
        begrunnelseType: VedtakBegrunnelseType,
        vilkårGjeldendeForBegrunnelse: Vilkår,
        aktuellePersonerForVedtaksperiode: List<BrevPerson>,
        triggesAv: TriggesAv
    ): List<BrevPerson> {
        val aktuellePersonidenter = aktuellePersonerForVedtaksperiode.map { it.aktivPersonIdent }

        triggesAv.vilkår.fold(setOf()) { acc, vilkår ->
            acc + brevPersonResultater.filter { aktuellePersonidenter.contains(it.aktør) }
                .map { brevPersonResultat ->
                    val utgjørendeVilkårResultat = brevPersonResultat.brevVilkårResultater
                        .filter { vilkårResultat -> vilkårResultat.vilkårType == vilkårGjeldendeForBegrunnelse }
                        .firstOrNull { brevVilkårResultat ->
                            val nesteBrevVilkårResultatAvSammeType: BrevVilkårResultat? =
                                brevPersonResultat.brevVilkårResultater.finnEtterfølgende(brevVilkårResultat)
                            erVilkårResultatUtgjørende(
                                brevVilkårResultat = brevVilkårResultat,
                                nesteBrevVilkårResultat = nesteBrevVilkårResultatAvSammeType,
                                begrunnelseType = begrunnelseType,
                                triggesAv = triggesAv
                            )
                        }
                    val person = aktuellePersonerForVedtaksperiode.firstOrNull { person ->
                        person.aktivPersonIdent == brevPersonResultat.aktør
                    }

                    if (utgjørendeVilkårResultat != null && person != null) {
                        acc.add(person)
                    }
                }
        }

        return brevPersonResultater
            .filter { aktuellePersonidenter.contains(it.aktør) }
            .fold(mutableListOf()) { acc, personResultat ->
                val utgjørendeVilkårResultat =
                    personResultat.brevVilkårResultater
                        .filter { it.vilkårType == vilkårGjeldendeForBegrunnelse }
                        .firstOrNull { brevVilkårResultat ->
                            val nesteBrevVilkårResultatAvSammeType: BrevVilkårResultat? =
                                personResultat.brevVilkårResultater.finnEtterfølgende(brevVilkårResultat)
                            erVilkårResultatUtgjørende(
                                brevVilkårResultat = brevVilkårResultat,
                                nesteBrevVilkårResultat = nesteBrevVilkårResultatAvSammeType,
                                begrunnelseType = begrunnelseType,
                                triggesAv = triggesAv
                            )
                        }

                val person = aktuellePersonerForVedtaksperiode.firstOrNull { person ->
                    person.aktivPersonIdent == personResultat.aktør
                }

                if (utgjørendeVilkårResultat != null && person != null) {
                    acc.add(person)
                }
                acc
            }
    }

    private fun erVilkårResultatUtgjørende(
        brevVilkårResultat: BrevVilkårResultat,
        nesteBrevVilkårResultat: BrevVilkårResultat?,
        begrunnelseType: VedtakBegrunnelseType,
        triggesAv: TriggesAv
    ): Boolean {
        if (brevVilkårResultat.periodeFom == null && begrunnelseType != VedtakBegrunnelseType.AVSLAG) {
            return false
        }

        return when (begrunnelseType) {
            VedtakBegrunnelseType.INNVILGET ->
                erInnvilgetVilkårResultatUtgjørende(
                    triggesAv,
                    brevVilkårResultat
                )

            VedtakBegrunnelseType.OPPHØR -> if (triggesAv.gjelderFørstePeriode) {
                erFørstePeriodeOgVilkårIkkeOppfylt(
                    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                    triggesAv = triggesAv,
                    vilkårResultat = brevVilkårResultat
                )
            } else {
                erOpphørResultatUtgjøreneForPeriode(
                    brevVilkårResultat = brevVilkårResultat,
                    triggesAv = triggesAv
                )
            }

            VedtakBegrunnelseType.REDUKSJON -> {
                erReduksjonResultatUtgjøreneForPeriode(
                    vilkårSomAvsluttesRettFørDennePerioden = brevVilkårResultat,
                    triggesAv = triggesAv,
                    vilkårSomStarterIDennePerioden = nesteBrevVilkårResultat
                )
            }

            VedtakBegrunnelseType.AVSLAG ->
                vilkårResultatPasserForAvslagsperiode(brevVilkårResultat)

            else -> throw Feil("Henting av personer med utgjørende vilkår when: Ikke implementert")
        }
    }

    private fun List<BrevVilkårResultat>.finnEtterfølgende(
        brevVilkårResultat: BrevVilkårResultat
    ): BrevVilkårResultat? =
        brevVilkårResultat.periodeTom?.let { tom -> this.find { it.periodeFom?.isEqual(tom.plusDays(1)) == true } }

    private fun erEtterEndretPeriodeAvSammeÅrsak(
        endretUtbetalingAndeler: List<BrevEndretUtbetalingAndel>,
        brevVedtaksPeriode: BrevVedtaksPeriode,
        aktuellePersoner: List<BrevPerson>,
        triggesAv: TriggesAv
    ) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
        endretUtbetalingAndel.månedPeriode().tom.sisteDagIInneværendeMåned()
            .erDagenFør(brevVedtaksPeriode.fom) &&
            aktuellePersoner.any { person -> person.aktørId == endretUtbetalingAndel.aktørId } &&
            triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
    }

    private fun erEndretTriggerErOppfylt(
        triggesAv: TriggesAv,
        brevEndretUtbetalingAndel: List<BrevEndretUtbetalingAndel>,
        brevVedtaksPeriode: BrevVedtaksPeriode
    ): Boolean {
        val endredeAndelerSomOverlapperVedtaksperiode = brevVedtaksPeriode
            .finnEndredeAndelerISammePeriode(brevEndretUtbetalingAndel)

        return endredeAndelerSomOverlapperVedtaksperiode.any {
            triggesAv.erTriggereOppfyltForEndretUtbetaling(
                brevEndretUtbetalingAndel = it
            )
        }
    }

    private fun erOpphørResultatUtgjøreneForPeriode(
        brevVilkårResultat: BrevVilkårResultat,
        triggesAv: TriggesAv
    ): Boolean {
        val erOppfyltTomMånedEtter = erOppfyltTomMånedEtter(brevVilkårResultat)

        val vilkårsluttForForrigePeriode = vedtaksperiode.fom.minusMonths(
            if (erOppfyltTomMånedEtter) 1 else 0
        )
        return triggesAv.erUtdypendeVilkårsvurderingOppfylt(brevVilkårResultat) &&
            brevVilkårResultat.periodeTom != null &&
            brevVilkårResultat.resultat == Resultat.OPPFYLT &&
            brevVilkårResultat.periodeTom.toYearMonth() == vilkårsluttForForrigePeriode.toYearMonth()
    }

    private fun erReduksjonResultatUtgjøreneForPeriode(
        vilkårSomAvsluttesRettFørDennePerioden: BrevVilkårResultat,
        triggesAv: TriggesAv,
        vilkårSomStarterIDennePerioden: BrevVilkårResultat?
    ): Boolean {
        if (vilkårSomAvsluttesRettFørDennePerioden.periodeTom == null) {
            return false
        }

        val erOppfyltTomMånedEtter = erOppfyltTomMånedEtter(vilkårSomAvsluttesRettFørDennePerioden)

        val erStartPåDeltBosted =
            vilkårSomAvsluttesRettFørDennePerioden.vilkårType == Vilkår.BOR_MED_SØKER &&
                !vilkårSomAvsluttesRettFørDennePerioden.utdypendeVilkårsvurderinger.contains(
                    UtdypendeVilkårsvurdering.DELT_BOSTED
                ) &&
                vilkårSomStarterIDennePerioden?.utdypendeVilkårsvurderinger?.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) == true &&
                triggesAv.deltbosted

        val startNestePeriodeEtterVilkår = vilkårSomAvsluttesRettFørDennePerioden.periodeTom
            .plusDays(if (erStartPåDeltBosted) 1 else 0)
            .plusMonths(if (erOppfyltTomMånedEtter) 1 else 0)

        return vilkårSomAvsluttesRettFørDennePerioden.resultat == Resultat.OPPFYLT && startNestePeriodeEtterVilkår.toYearMonth() == vedtaksperiode.fom.toYearMonth()
    }

    private fun erOppfyltTomMånedEtter(brevVilkårResultat: BrevVilkårResultat) =
        brevVilkårResultat.periodeTom == brevVilkårResultat.periodeTom?.sisteDagIMåned()

    private fun erInnvilgetVilkårResultatUtgjørende(
        triggesAv: TriggesAv,
        brevVilkårResultat: BrevVilkårResultat
    ): Boolean {
        val vilkårResultatFomMåned = brevVilkårResultat.periodeFom!!.toYearMonth()
        val vedtaksperiodeFomMåned = vedtaksperiode.fom.toYearMonth()

        return triggesAv.erUtdypendeVilkårsvurderingOppfylt(brevVilkårResultat) &&
            vilkårResultatFomMåned == vedtaksperiodeFomMåned.minusMonths(1) &&
            brevVilkårResultat.resultat == Resultat.OPPFYLT
    }

    private fun erFørstePeriodeOgVilkårIkkeOppfylt(
        erFørsteVedtaksperiodePåFagsak: Boolean,
        triggesAv: TriggesAv,
        vilkårResultat: BrevVilkårResultat
    ): Boolean {
        val vilkårIkkeOppfyltForPeriode =
            vilkårResultat.resultat == Resultat.IKKE_OPPFYLT &&
                vilkårResultat.toPeriode().overlapperHeltEllerDelvisMed(vedtaksperiode)

        val vilkårOppfyltRettEtterPeriode =
            vilkårResultat.resultat == Resultat.OPPFYLT &&
                vedtaksperiode.tom.toYearMonth() == vilkårResultat.periodeFom!!.toYearMonth()

        return erFørsteVedtaksperiodePåFagsak &&
            triggesAv.erUtdypendeVilkårsvurderingOppfylt(vilkårResultat) &&
            (vilkårIkkeOppfyltForPeriode || vilkårOppfyltRettEtterPeriode)
    }

    private fun vilkårResultatPasserForAvslagsperiode(
        brevVilkårResultat: BrevVilkårResultat
    ): Boolean {
        val erAvslagUtenFomDato = brevVilkårResultat.periodeFom == null

        val fomVilkår =
            if (erAvslagUtenFomDato) {
                TIDENES_MORGEN.toYearMonth()
            } else {
                brevVilkårResultat.periodeFom!!.toYearMonth()
            }

        return fomVilkår == vedtaksperiode.fom.toYearMonth() &&
            brevVilkårResultat.resultat == Resultat.IKKE_OPPFYLT
    }

    private val vedtaksperiode = Periode(
        fom = brevVedtaksPeriode.fom ?: TIDENES_MORGEN,
        tom = brevVedtaksPeriode.tom ?: TIDENES_ENDE
    )
}
