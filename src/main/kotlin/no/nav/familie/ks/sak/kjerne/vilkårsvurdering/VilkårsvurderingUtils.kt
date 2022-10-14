package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.Null
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.ks.sak.common.tidslinje.Verdi
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilTidslinjePerioderMedDato
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.tilTriggesAv
import no.nav.familie.ks.sak.kjerne.vedtak.EØSStandardbegrunnelse
import no.nav.familie.ks.sak.kjerne.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.vedtak.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.vedtak.tilSanityEØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

fun standardbegrunnelserTilNedtrekksmenytekster(
    sanityBegrunnelser: List<SanityBegrunnelse>
) =
    Standardbegrunnelse
        .values()
        .groupBy { it.vedtakBegrunnelseType }
        .mapValues { begrunnelseGruppe ->
            begrunnelseGruppe.value
                .flatMap { vedtakBegrunnelse ->
                    vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                        sanityBegrunnelser,
                        vedtakBegrunnelse
                    )
                }
        }

fun eøsStandardbegrunnelserTilNedtrekksmenytekster(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>
) = EØSStandardbegrunnelse.values().groupBy { it.vedtakBegrunnelseType }
    .mapValues { begrunnelseGruppe ->
        begrunnelseGruppe.value.flatMap { vedtakBegrunnelse ->
            eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                sanityEØSBegrunnelser,
                vedtakBegrunnelse
            )
        }
    }

fun vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    vedtakBegrunnelse: Standardbegrunnelse
): List<VedtakBegrunnelseTilknyttetVilkårResponseDto> {
    val sanityBegrunnelse = vedtakBegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser) ?: return emptyList()

    val triggesAv = sanityBegrunnelse.tilTriggesAv()
    val visningsnavn = sanityBegrunnelse.navnISystem

    return if (triggesAv.vilkår.isEmpty()) {
        listOf(
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = null
            )
        )
    } else {
        triggesAv.vilkår.map {
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = it
            )
        }
    }
}

fun eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
    vedtakBegrunnelse: EØSStandardbegrunnelse
): List<VedtakBegrunnelseTilknyttetVilkårResponseDto> {
    val eøsSanityBegrunnelse = vedtakBegrunnelse.tilSanityEØSBegrunnelse(sanityEØSBegrunnelser) ?: return emptyList()

    return listOf(
        VedtakBegrunnelseTilknyttetVilkårResponseDto(
            id = vedtakBegrunnelse,
            navn = eøsSanityBegrunnelse.navnISystem,
            vilkår = null
        )
    )
}

/**
 * Funksjon som tar inn endret vilkår og lager nye vilkårresultater til å få plass til den endrede perioden.
 * @param[personResultat] Person med vilkår som eventuelt justeres
 * @param[restVilkårResultat] Det endrede vilkårresultatet
 * @return VilkårResultater før og etter mutering
 */
fun endreVilkårResultat(
    vilkårResultater: List<VilkårResultat>,
    endretVilkårResultatDto: VilkårResultatDto
): List<VilkårResultat> {
    validerAvslagUtenPeriodeMedLøpende(
        vilkårResultater = vilkårResultater,
        endretVilkårResultat = endretVilkårResultatDto
    )

    val endretVilkårResultat =
        endretVilkårResultatDto.tilVilkårResultat(vilkårResultater.single { it.id == endretVilkårResultatDto.id })

    val (vilkårResultaterSomSkalTilpasses, vilkårResultaterSomIkkeSkalTilpasses) = vilkårResultater.partition {
        !it.erAvslagUtenPeriode() || it.id == endretVilkårResultatDto.id
    }

    val tilpassetVilkårResultater = vilkårResultaterSomSkalTilpasses
        .flatMap {
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = endretVilkårResultatDto.id,
                eksisterendeVilkårResultat = it,
                endretVilkårResultat = endretVilkårResultat
            )
        }

    return tilpassetVilkårResultater + vilkårResultaterSomIkkeSkalTilpasses
}

/**
 * Funksjon som forsøker å legge til en periode på et vilkår.
 * Dersom det allerede finnes en uvurdet periode med samme vilkårstype
 * skal det kastes en feil.
 */
fun opprettNyttVilkårResultat(personResultat: PersonResultat, vilkårType: Vilkår): VilkårResultat {
    val nyttVilkårResultat = VilkårResultat(
        personResultat = personResultat,
        vilkårType = vilkårType,
        resultat = Resultat.IKKE_VURDERT,
        begrunnelse = "",
        behandlingId = personResultat.vilkårsvurdering.behandling.id
    )

    if (harUvurdertePerioderForVilkårType(personResultat, vilkårType)) {
        throw FunksjonellFeil(
            melding = "Det finnes allerede uvurderte vilkår av samme vilkårType",
            frontendFeilmelding = "Du må ferdigstille vilkårsvurderingen på en periode som allerede er påbegynt, før du kan legge til en ny periode"
        )
    }

    return nyttVilkårResultat
}

private fun harUvurdertePerioderForVilkårType(personResultat: PersonResultat, vilkårType: Vilkår): Boolean =
    personResultat.vilkårResultater
        .filter { it.vilkårType == vilkårType }
        .find { it.resultat == Resultat.IKKE_VURDERT } != null

fun List<VilkårResultat>.tilTidslinje(): Tidslinje<VilkårResultat> {
    return map {
        TidslinjePeriodeMedDato(
            verdi = it,
            fom = it.periodeFom,
            tom = it.periodeTom
        )
    }.tilTidslinje()
}

/**
 * @param [personResultat] person vilkårresultatet tilhører
 * @param [eksisterendeVilkårResultat] vilkårresultat som skal oppdaters på person
 * @param [endretVilkårResultatDto] oppdatert resultat fra frontend
 */
fun tilpassVilkårForEndretVilkår(
    endretVilkårResultatId: Long,
    eksisterendeVilkårResultat: VilkårResultat,
    endretVilkårResultat: VilkårResultat
): List<VilkårResultat> {
    if (eksisterendeVilkårResultat.id == endretVilkårResultatId) {
        return listOf(endretVilkårResultat)
    }

    if (eksisterendeVilkårResultat.vilkårType != endretVilkårResultat.vilkårType || endretVilkårResultat.erAvslagUtenPeriode()) {
        return listOf(eksisterendeVilkårResultat)
    }

    val eksisterendeVilkårResultatTidslinje = listOf(eksisterendeVilkårResultat).tilTidslinje()
    val endretVilkårResultatTidslinje = listOf(endretVilkårResultat).tilTidslinje()

    return eksisterendeVilkårResultatTidslinje
        .kombinerMed(endretVilkårResultatTidslinje) { eksisterendeVilkår, endretVilkår ->
            if (endretVilkår is Verdi) {
                Null()
            } else {
                eksisterendeVilkår
            }
        }.tilTidslinjePerioderMedDato()
        .mapNotNull {
            it.tilVilkårResultatMedOppdatertPeriodeOgBehandlingsId(nyBehandlingsId = endretVilkårResultat.behandlingId)
        }
}

private fun TidslinjePeriodeMedDato<VilkårResultat>.tilVilkårResultatMedOppdatertPeriodeOgBehandlingsId(
    nyBehandlingsId: Long
): VilkårResultat? {
    val vilkårResultat = this.periodeVerdi.verdi

    val fom = this.fom.tilLocalDateEllerNull()
    val tom = this.tom.tilLocalDateEllerNull()

    val vilkårsdatoErUendret = fom == vilkårResultat?.periodeFom &&
        tom == vilkårResultat?.periodeTom

    return if (vilkårsdatoErUendret) {
        vilkårResultat
    } else {
        vilkårResultat?.kopierMedNyPeriode(
            fom = fom,
            tom = tom,
            behandlingId = nyBehandlingsId
        )
    }
}

private fun validerAvslagUtenPeriodeMedLøpende(
    vilkårResultater: List<VilkårResultat>,
    endretVilkårResultat: VilkårResultatDto
) {
    val filtrerteVilkårResultater =
        vilkårResultater.filter { it.vilkårType == endretVilkårResultat.vilkårType && it.id != endretVilkårResultat.id }

    when {
        // For bor med søker-vilkåret kan avslag og innvilgelse være overlappende, da man kan f.eks. avslå full kontantstøtte, men innvilge delt
        endretVilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER -> return

        endretVilkårResultat.erAvslagUtenPeriode() && filtrerteVilkårResultater.any { it.resultat == Resultat.OPPFYLT && it.harFremtidigTom() } ->
            throw FunksjonellFeil(
                "Finnes løpende oppfylt ved forsøk på å legge til avslag uten periode ",
                "Du kan ikke legge til avslag uten datoer fordi det finnes oppfylt løpende periode på vilkåret."
            )

        endretVilkårResultat.harFremtidigTom() && filtrerteVilkårResultater.any { it.erAvslagUtenPeriode() } ->
            throw FunksjonellFeil(
                "Finnes avslag uten periode ved forsøk på å legge til løpende oppfylt",
                "Du kan ikke legge til løpende periode fordi det er vurdert avslag uten datoer på vilkåret."
            )
    }
}
