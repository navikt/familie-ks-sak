package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.TriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVilkårResultat

/**
 * Funksjonen henter personer som trigger den gitte vedtaksperioden ved å hente vilkårResultater
 * basert på de attributter som definerer om en vedtaksbegrunnelse er trigget for en periode.
 *
 * @param brevPersonResultater - Resultatene fra vilkårsvurderingen for hver person
 * @param vedtaksperiode - Perioden det skal sjekkes for
 * @param oppdatertBegrunnelseType - Begrunnelsestype det skal sjekkes for
 * @param aktuellePersonerForVedtaksperiode - Personer på behandlingen som er aktuelle for vedtaksperioden
 * @param triggesAv -  Hva som trigger en vedtaksbegrynnelse.
 * @param erFørsteVedtaksperiodePåFagsak - Om vedtaksperioden er første periode på fagsak.
 *        Brukes for opphør som har egen logikk dersom det er første periode.
 * @return List med personene det trigges endring på
 */
fun hentPersonerForAlleUtgjørendeVilkår(
    brevPersonResultater: List<BrevPersonResultat>,
    vedtaksperiode: Periode,
    oppdatertBegrunnelseType: VedtakBegrunnelseType,
    aktuellePersonerForVedtaksperiode: List<BrevPerson>,
    triggesAv: TriggesAv,
    erFørsteVedtaksperiodePåFagsak: Boolean
): Set<BrevPerson> {
    return triggesAv.vilkår.fold(setOf()) { acc, vilkår ->
        acc + hentPersonerMedUtgjørendeVilkår(
            brevPersonResultater = brevPersonResultater,
            vedtaksperiode = vedtaksperiode,
            begrunnelseType = oppdatertBegrunnelseType,
            vilkårGjeldendeForBegrunnelse = vilkår,
            aktuellePersonerForVedtaksperiode = aktuellePersonerForVedtaksperiode,
            triggesAv = triggesAv,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
        )
    }
}

private fun hentPersonerMedUtgjørendeVilkår(
    brevPersonResultater: List<BrevPersonResultat>,
    vedtaksperiode: Periode,
    begrunnelseType: VedtakBegrunnelseType,
    vilkårGjeldendeForBegrunnelse: Vilkår,
    aktuellePersonerForVedtaksperiode: List<BrevPerson>,
    triggesAv: TriggesAv,
    erFørsteVedtaksperiodePåFagsak: Boolean
): List<BrevPerson> {
    val aktuellePersonidenter = aktuellePersonerForVedtaksperiode.map { it.aktivPersonIdent }

    return brevPersonResultater
        .filter { aktuellePersonidenter.contains(it.personIdent) }
        .fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkårResultat =
                personResultat.minimerteVilkårResultater
                    .filter { it.vilkårType == vilkårGjeldendeForBegrunnelse }
                    .firstOrNull { minimertVilkårResultat ->
                        val nesteMinimerteVilkårResultatAvSammeType: BrevVilkårResultat? =
                            personResultat.minimerteVilkårResultater.finnEtterfølgende(minimertVilkårResultat)
                        erVilkårResultatUtgjørende(
                            brevVilkårResultat = minimertVilkårResultat,
                            nesteMinimerteVilkårResultat = nesteMinimerteVilkårResultatAvSammeType,
                            begrunnelseType = begrunnelseType,
                            triggesAv = triggesAv,
                            vedtaksperiode = vedtaksperiode,
                            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
                        )
                    }

            val person = aktuellePersonerForVedtaksperiode.firstOrNull { person ->
                person.aktivPersonIdent == personResultat.personIdent
            }

            if (utgjørendeVilkårResultat != null && person != null) {
                acc.add(person)
            }
            acc
        }
}

private fun List<BrevVilkårResultat>.finnEtterfølgende(
    brevVilkårResultat: BrevVilkårResultat
): BrevVilkårResultat? =
    brevVilkårResultat.periodeTom?.let { tom -> this.find { it.periodeFom?.isEqual(tom.plusDays(1)) == true } }

private fun erVilkårResultatUtgjørende(
    brevVilkårResultat: BrevVilkårResultat,
    nesteMinimerteVilkårResultat: BrevVilkårResultat?,
    begrunnelseType: VedtakBegrunnelseType,
    triggesAv: TriggesAv,
    vedtaksperiode: Periode,
    erFørsteVedtaksperiodePåFagsak: Boolean
): Boolean {
    if (brevVilkårResultat.periodeFom == null && begrunnelseType != VedtakBegrunnelseType.AVSLAG) {
        return false
    }

    return when (begrunnelseType) {
        VedtakBegrunnelseType.INNVILGET ->
            erInnvilgetVilkårResultatUtgjørende(
                triggesAv,
                brevVilkårResultat,
                vedtaksperiode
            )

        VedtakBegrunnelseType.OPPHØR -> if (triggesAv.gjelderFørstePeriode) {
            erFørstePeriodeOgVilkårIkkeOppfylt(
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv,
                vilkårResultat = brevVilkårResultat
            )
        } else {
            erOpphørResultatUtgjøreneForPeriode(
                brevVilkårResultat = brevVilkårResultat,
                triggesAv = triggesAv,
                vedtaksperiode = vedtaksperiode
            )
        }

        VedtakBegrunnelseType.REDUKSJON -> {
            erReduksjonResultatUtgjøreneForPeriode(
                vilkårSomAvsluttesRettFørDennePerioden = brevVilkårResultat,
                triggesAv = triggesAv,
                vedtaksperiode = vedtaksperiode,
                vilkårSomStarterIDennePerioden = nesteMinimerteVilkårResultat
            )
        }

        VedtakBegrunnelseType.AVSLAG ->
            vilkårResultatPasserForAvslagsperiode(brevVilkårResultat, vedtaksperiode)

        else -> throw Feil("Henting av personer med utgjørende vilkår when: Ikke implementert")
    }
}

private fun vilkårResultatPasserForAvslagsperiode(
    brevVilkårResultat: BrevVilkårResultat,
    vedtaksperiode: Periode
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


private fun erOpphørResultatUtgjøreneForPeriode(
    brevVilkårResultat: BrevVilkårResultat,
    triggesAv: TriggesAv,
    vedtaksperiode: Periode
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
    vedtaksperiode: Periode,
    vilkårSomStarterIDennePerioden: BrevVilkårResultat?
): Boolean {
    if (vilkårSomAvsluttesRettFørDennePerioden.periodeTom == null) {
        return false
    }

    val erOppfyltTomMånedEtter = erOppfyltTomMånedEtter(vilkårSomAvsluttesRettFørDennePerioden)

    val erStartPåDeltBosted =
        vilkårSomAvsluttesRettFørDennePerioden.vilkårType == Vilkår.BOR_MED_SØKER &&
                !vilkårSomAvsluttesRettFørDennePerioden.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) &&
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
    brevVilkårResultat: BrevVilkårResultat,
    vedtaksperiode: Periode
): Boolean {
    val vilkårResultatFomMåned = brevVilkårResultat.periodeFom!!.toYearMonth()
    val vedtaksperiodeFomMåned = vedtaksperiode.fom.toYearMonth()

    return triggesAv.erUtdypendeVilkårsvurderingOppfylt(brevVilkårResultat) &&
            vilkårResultatFomMåned == vedtaksperiodeFomMåned.minusMonths(1) &&
            brevVilkårResultat.resultat == Resultat.OPPFYLT
}

fun erFørstePeriodeOgVilkårIkkeOppfylt(
    erFørsteVedtaksperiodePåFagsak: Boolean,
    vedtaksperiode: Periode,
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
