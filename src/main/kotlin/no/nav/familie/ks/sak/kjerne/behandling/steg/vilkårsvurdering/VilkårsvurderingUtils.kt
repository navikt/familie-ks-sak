package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.IkkeNullbarPeriode
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erBack2BackIMånedsskifte
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate
import java.time.Month

fun standardbegrunnelserTilNedtrekksmenytekster(sanityBegrunnelser: List<SanityBegrunnelse>): Map<BegrunnelseType, List<VedtakBegrunnelseTilknyttetVilkårResponseDto>> {
    return (NasjonalEllerFellesBegrunnelse.entries + EØSBegrunnelse.entries)
        .groupBy { it.begrunnelseType }
        .mapValues { begrunnelseGruppe ->
            begrunnelseGruppe.value
                .flatMap { vedtakBegrunnelse ->
                    vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                        sanityBegrunnelser,
                        vedtakBegrunnelse,
                    )
                }
        }
}

fun vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    vedtakBegrunnelse: IBegrunnelse,
): List<VedtakBegrunnelseTilknyttetVilkårResponseDto> {
    val sanityBegrunnelse = vedtakBegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser) ?: return emptyList()
    val visningsnavn = sanityBegrunnelse.navnISystem

    return if (sanityBegrunnelse.vilkår.isEmpty()) {
        listOf(
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = null,
            ),
        )
    } else {
        sanityBegrunnelse.vilkår.map {
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = it,
            )
        }
    }
}

/**
 * Funksjon som tar inn et endret vilkår og lager nye vilkårresultater til å få plass til den endrede perioden.
 * @param[eksisterendeVilkårResultater] Eksisterende vilkårresultater
 * @param[endretVilkårResultatDto] Endret vilkårresultat
 * @return VilkårResultater før og etter mutering
 */
fun endreVilkårResultat(
    eksisterendeVilkårResultater: List<VilkårResultat>,
    endretVilkårResultatDto: VilkårResultatDto,
): List<VilkårResultat> {
    validerAvslagUtenPeriodeMedLøpende(
        eksisterendeVilkårResultater = eksisterendeVilkårResultater,
        endretVilkårResultat = endretVilkårResultatDto,
    )

    val endretVilkårResultat =
        endretVilkårResultatDto.tilVilkårResultat(eksisterendeVilkårResultater.single { it.id == endretVilkårResultatDto.id })

    val (vilkårResultaterSomSkalTilpasses, vilkårResultaterSomIkkeTrengerTilpassning) =
        eksisterendeVilkårResultater.partition {
            !it.erAvslagUtenPeriode() || it.id == endretVilkårResultatDto.id
        }

    val tilpassetVilkårResultater =
        vilkårResultaterSomSkalTilpasses
            .flatMap {
                tilpassVilkårForEndretVilkår(
                    endretVilkårResultatId = endretVilkårResultatDto.id,
                    eksisterendeVilkårResultat = it,
                    endretVilkårResultat = endretVilkårResultat,
                )
            }

    return tilpassetVilkårResultater + vilkårResultaterSomIkkeTrengerTilpassning
}

/**
 * Funksjon som forsøker å legge til en periode på et vilkår.
 * Dersom det allerede finnes en uvurdet periode med samme vilkårstype
 * skal det kastes en feil.
 */
fun opprettNyttVilkårResultat(
    personResultat: PersonResultat,
    vilkårType: Vilkår,
): VilkårResultat {
    if (harUvurdertePerioderForVilkårType(personResultat, vilkårType)) {
        throw FunksjonellFeil(
            melding = "Det finnes allerede uvurderte vilkår av samme vilkårType",
            frontendFeilmelding = "Du må ferdigstille vilkårsvurderingen på en periode som allerede er påbegynt, før du kan legge til en ny periode",
        )
    }

    return VilkårResultat(
        personResultat = personResultat,
        vilkårType = vilkårType,
        resultat = Resultat.IKKE_VURDERT,
        begrunnelse = "",
        behandlingId = personResultat.vilkårsvurdering.behandling.id,
    )
}

/**
 * @param [endretVilkårResultatId] id til VilkårResultat som er endret
 * @param [eksisterendeVilkårResultat] vilkårresultat som skal oppdaters på person
 * @param [endretVilkårResultat] endret VilkårResultat
 */
fun tilpassVilkårForEndretVilkår(
    endretVilkårResultatId: Long,
    eksisterendeVilkårResultat: VilkårResultat,
    endretVilkårResultat: VilkårResultat,
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
            if (endretVilkår != null) {
                null
            } else {
                eksisterendeVilkår
            }
        }.tilPerioderIkkeNull()
        .map {
            it.tilVilkårResultatMedOppdatertPeriodeOgBehandlingsId(nyBehandlingsId = endretVilkårResultat.behandlingId)
        }
}

private fun harUvurdertePerioderForVilkårType(
    personResultat: PersonResultat,
    vilkårType: Vilkår,
): Boolean = personResultat.vilkårResultater.any { it.vilkårType == vilkårType && it.resultat == Resultat.IKKE_VURDERT }

private fun validerAvslagUtenPeriodeMedLøpende(
    eksisterendeVilkårResultater: List<VilkårResultat>,
    endretVilkårResultat: VilkårResultatDto,
) {
    val filtrerteVilkårResultater =
        eksisterendeVilkårResultater.filter { it.vilkårType == endretVilkårResultat.vilkårType && it.id != endretVilkårResultat.id }

    when {
        // For bor med søker-vilkåret kan avslag og innvilgelse være overlappende, da man kan f.eks. avslå full kontantstøtte, men innvilge delt
        endretVilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER -> return

        endretVilkårResultat.erAvslagUtenPeriode() && filtrerteVilkårResultater.any { it.resultat == Resultat.OPPFYLT && it.harFremtidigTom() } ->
            throw FunksjonellFeil(
                "Finnes løpende oppfylt ved forsøk på å legge til avslag uten periode ",
                "Du kan ikke legge til avslag uten datoer fordi det finnes oppfylt løpende periode på vilkåret.",
            )

        endretVilkårResultat.harFremtidigTom() && filtrerteVilkårResultater.any { it.erAvslagUtenPeriode() } ->
            throw FunksjonellFeil(
                "Finnes avslag uten periode ved forsøk på å legge til løpende oppfylt",
                "Du kan ikke legge til løpende periode fordi det er vurdert avslag uten datoer på vilkåret.",
            )
    }
}

fun List<VilkårResultat>.tilTidslinje(): Tidslinje<VilkårResultat> {
    return map {
        Periode(
            verdi = it,
            fom = it.periodeFom,
            tom = it.periodeTom,
        )
    }.tilTidslinje()
}

private fun Periode<VilkårResultat>.tilVilkårResultatMedOppdatertPeriodeOgBehandlingsId(nyBehandlingsId: Long): VilkårResultat {
    val vilkårResultat = this.verdi

    val vilkårsdatoErUendret = this.fom == vilkårResultat.periodeFom && this.tom == vilkårResultat.periodeTom

    return if (vilkårsdatoErUendret) {
        vilkårResultat
    } else {
        vilkårResultat.kopierMedNyPeriodeOgBehandling(
            fom = this.fom,
            tom = this.tom,
            behandlingId = nyBehandlingsId,
        )
    }
}

fun finnTilOgMedDato(
    tilOgMed: LocalDate?,
    vilkårResultater: List<VilkårResultat>,
): LocalDate {
    // LocalDateTimeline krasjer i isTimelineOutsideInterval funksjonen dersom vi sender med TIDENES_ENDE,
    // så bruker tidenes ende minus én dag.
    if (tilOgMed == null) return TIDENES_ENDE.minusDays(1)
    val skalVidereføresEnMndEkstra =
        vilkårResultater.any { vilkårResultat ->
            erBack2BackIMånedsskifte(tilOgMed = tilOgMed, fraOgMed = vilkårResultat.periodeFom)
        }

    return if (skalVidereføresEnMndEkstra) tilOgMed.plusMonths(1).sisteDagIMåned() else tilOgMed.sisteDagIMåned()
}

fun validerAtDatoErKorrektIBarnasVilkår(
    vilkårsvurdering: Vilkårsvurdering,
    barna: List<Person>,
) {
    val funksjonelleFeil = mutableListOf<String>()

    barna.map { barn ->
        vilkårsvurdering.personResultater
            .flatMap { it.vilkårResultater }
            .filter { it.personResultat?.aktør == barn.aktør }
            .forEach { vilkårResultat ->
                val fødselsdato = barn.fødselsdato.tilDagMånedÅr()
                val vilkårType = vilkårResultat.vilkårType
                if (vilkårResultat.resultat == Resultat.OPPFYLT && vilkårResultat.periodeFom == null) {
                    funksjonelleFeil.add("Vilkår $vilkårType for barn med fødselsdato $fødselsdato mangler fom dato.")
                }
                if (vilkårResultat.periodeFom != null &&
                    vilkårType != Vilkår.MEDLEMSKAP_ANNEN_FORELDER &&
                    vilkårResultat.lagOgValiderPeriodeFraVilkår().fom.isBefore(barn.fødselsdato)
                ) {
                    funksjonelleFeil.add(
                        "Vilkår $vilkårType for barn med fødselsdato $fødselsdato " +
                            "har fom dato før barnets fødselsdato.",
                    )
                }
                if (vilkårResultat.periodeFom != null &&
                    vilkårResultat.erEksplisittAvslagPåSøknad != true &&
                    vilkårResultat.vilkårType == Vilkår.BARNETS_ALDER
                ) {
                    vilkårResultat.validerVilkårBarnetsAlder(
                        vilkårResultat.lagOgValiderPeriodeFraVilkår(),
                        barn,
                    )?.let { funksjonelleFeil.add(it) }
                }
            }
    }

    if (funksjonelleFeil.isNotEmpty()) {
        throw FunksjonellFeil(funksjonelleFeil.joinToString(separator = "\n"))
    }
}

private fun VilkårResultat.lagOgValiderPeriodeFraVilkår(): IkkeNullbarPeriode<Long> =
    when {
        periodeFom !== null -> {
            IkkeNullbarPeriode(verdi = behandlingId, fom = checkNotNull(periodeFom), tom = periodeTom ?: TIDENES_ENDE)
        }

        erEksplisittAvslagPåSøknad == true && periodeTom == null -> {
            IkkeNullbarPeriode(verdi = behandlingId, fom = TIDENES_MORGEN, tom = TIDENES_ENDE)
        }

        else -> {
            throw FunksjonellFeil("Ugyldig periode. Periode må ha t.o.m.-dato eller være et avslag uten datoer.")
        }
    }

private fun VilkårResultat.validerVilkårBarnetsAlder(
    periode: IkkeNullbarPeriode<Long>,
    barn: Person,
): String? {
    val periodeFomBarnetsAlderLov2024 = barn.fødselsdato.plusMonths(13)
    val periodeTomBarnetsAlderLov2024 = barn.fødselsdato.plusMonths(19)

    val periodeFomBarnetsAlderLov2021 = barn.fødselsdato.plusYears(1)
    val periodeTomBarnetsAlderLov2021 = barn.fødselsdato.plusYears(2)

    val erTruffetAvRegelverk2021 = periodeFomBarnetsAlderLov2021.isBefore(DATO_LOVENDRING_2024)
    val erTruffetAvRegelverk2024 = periodeTomBarnetsAlderLov2024.erSammeEllerEtter(DATO_LOVENDRING_2024)

    return when {
        erTruffetAvRegelverk2021 && erTruffetAvRegelverk2024 -> "Barnets alder vilkåret må splittes i to perioder fordi barnet fyller 1 år før og 19 måneder etter 01.08.24. Periodene må være som følgende: [$periodeFomBarnetsAlderLov2021 - ${minOf(periodeTomBarnetsAlderLov2021, DATO_LOVENDRING_2024.minusMonths(1).sisteDagIMåned())}, ${maxOf(periodeFomBarnetsAlderLov2024, DATO_LOVENDRING_2024)} - $periodeTomBarnetsAlderLov2024]"
        erTruffetAvRegelverk2021 -> validerBarnetsAlderIHenholdTilLovI2021(periode, barn, periodeFomBarnetsAlderLov2021, periodeTomBarnetsAlderLov2021)
        erTruffetAvRegelverk2024 -> validerBarnetsAlderIHenholdTilLovI2024(periode, barn, periodeFomBarnetsAlderLov2024, periodeTomBarnetsAlderLov2024)
        else -> null
    }
}

private fun VilkårResultat.validerBarnetsAlderIHenholdTilLovI2024(
    periode: IkkeNullbarPeriode<Long>,
    barn: Person,
    periodeFomBarnetsAlderLov2024: LocalDate,
    periodeTomBarnetsAlderLov2024: LocalDate,
) = when {
    this.erAdopsjonOppfylt() &&
        periode.tom.isAfter(barn.fødselsdato.plusYears(6).withMonth(Month.AUGUST.value).sisteDagIMåned()) ->
        "Du kan ikke sette en t.o.m dato som er etter august året barnet fyller 6 år."

    this.erAdopsjonOppfylt() && periode.fom.plusMonths(7) < periode.tom ->
        "Differansen mellom f.o.m datoen og t.o.m datoen kan ikke være mer enn 7 måneder. "

    !this.erAdopsjonOppfylt() && !periode.fom.isEqual(maxOf(periodeFomBarnetsAlderLov2024, DATO_LOVENDRING_2024)) ->
        "F.o.m datoen må være lik datoen barnet fyller 13 måneder eller 01.08.24 dersom barnet fyller 13 måneder før 01.08.24."

    !this.erAdopsjonOppfylt() && !periode.tom.isEqual(periodeTomBarnetsAlderLov2024) && periode.tom != barn.dødsfall?.dødsfallDato ->
        "T.o.m datoen må være lik datoen barnet fyller 19 måneder. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall."

    else -> null
}

private fun VilkårResultat.validerBarnetsAlderIHenholdTilLovI2021(
    periode: IkkeNullbarPeriode<Long>,
    barn: Person,
    periodeFomBarnetsAlderLov2021: LocalDate,
    periodeTomBarnetsAlderLov2021: LocalDate,
) = when {
    this.erAdopsjonOppfylt() &&
        periode.tom.isAfter(barn.fødselsdato.plusYears(6).withMonth(Month.AUGUST.value).sisteDagIMåned()) ->
        "Du kan ikke sette en t.o.m dato som er etter august året barnet fyller 6 år."

    // Ved adopsjon skal det være lov å ha en differanse på 1 år slik at man får 11 måned med kontantstøtte.
    this.erAdopsjonOppfylt() && periode.fom.plusYears(1) < periode.tom ->
        "Differansen mellom f.o.m datoen og t.o.m datoen kan ikke være mer enn 1 år."

    !this.erAdopsjonOppfylt() && !periode.fom.isEqual(periodeFomBarnetsAlderLov2021) ->
        "F.o.m datoen må være lik barnets 1 års dag."

    !this.erAdopsjonOppfylt() && !periode.tom.isEqual(minOf(periodeTomBarnetsAlderLov2021, DATO_LOVENDRING_2024.minusMonths(1).sisteDagIMåned())) && periode.tom != barn.dødsfall?.dødsfallDato ->
        "T.o.m datoen må være lik barnets 2 års dag eller 31.07.24 på grunn av lovendring fra og med 01.08.24. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall."

    else -> null
}

fun genererInitiellVilkårsvurdering(
    behandling: Behandling,
    forrigeVilkårsvurdering: Vilkårsvurdering?,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    behandlingSkalFølgeNyeLovendringer2024: Boolean,
): Vilkårsvurdering {
    return Vilkårsvurdering(behandling = behandling).apply {
        personResultater =
            personopplysningGrunnlag.personer.map { person ->
                val personResultat = PersonResultat(vilkårsvurdering = this, aktør = person.aktør)

                val forrigeBehandlingHaddeEøsSpesifikkeVilkår = forrigeVilkårsvurdering?.personResultater?.flatMap { it.vilkårResultater }?.any { it.vilkårType.eøsSpesifikt } ?: false
                val behandlingKategoriErEøs = behandling.kategori == BehandlingKategori.EØS

                val skalHenteEøsSpesifikkeVilkår = behandlingKategoriErEøs || forrigeBehandlingHaddeEøsSpesifikkeVilkår

                val vilkårForPerson = Vilkår.hentVilkårFor(person.type, skalHenteEøsSpesifikkeVilkår)
                val regelsett = if (behandlingSkalFølgeNyeLovendringer2024) VilkårRegelsett.LOV_AUGUST_2024 else VilkårRegelsett.LOV_AUGUST_2021

                val vilkårResultater =
                    vilkårForPerson.map { vilkår ->
                        // prefyller diverse vilkår automatisk basert på type
                        when (vilkår) {
                            Vilkår.BARNETS_ALDER -> {
                                val periodeFomForBarnetsAlder =
                                    when (regelsett) {
                                        VilkårRegelsett.LOV_AUGUST_2021 -> person.fødselsdato.plusYears(1)
                                        VilkårRegelsett.LOV_AUGUST_2024 -> person.fødselsdato.plusMonths(13)
                                    }

                                val periodeTomForBarnetsAlder =
                                    when (regelsett) {
                                        VilkårRegelsett.LOV_AUGUST_2021 -> person.fødselsdato.plusYears(2)
                                        VilkårRegelsett.LOV_AUGUST_2024 -> person.fødselsdato.plusMonths(19)
                                    }

                                VilkårResultat(
                                    personResultat = personResultat,
                                    erAutomatiskVurdert = true,
                                    resultat = Resultat.OPPFYLT,
                                    vilkårType = vilkår,
                                    begrunnelse = "Vurdert og satt automatisk",
                                    behandlingId = behandling.id,
                                    periodeFom = periodeFomForBarnetsAlder,
                                    periodeTom = periodeTomForBarnetsAlder,
                                )
                            }

                            Vilkår.MEDLEMSKAP ->
                                VilkårResultat(
                                    personResultat = personResultat,
                                    erAutomatiskVurdert = false,
                                    resultat = Resultat.IKKE_VURDERT,
                                    vilkårType = vilkår,
                                    begrunnelse = "",
                                    periodeFom = person.fødselsdato.plusYears(5),
                                    behandlingId = behandling.id,
                                )

                            Vilkår.BARNEHAGEPLASS -> {
                                val periodeFomForBarnehageplass =
                                    when (regelsett) {
                                        VilkårRegelsett.LOV_AUGUST_2021 -> person.fødselsdato
                                        VilkårRegelsett.LOV_AUGUST_2024 -> person.fødselsdato.plusMonths(13)
                                    }

                                VilkårResultat(
                                    personResultat = personResultat,
                                    erAutomatiskVurdert = false,
                                    resultat = Resultat.OPPFYLT,
                                    vilkårType = vilkår,
                                    begrunnelse = "",
                                    periodeFom = periodeFomForBarnehageplass,
                                    behandlingId = behandling.id,
                                )
                            }

                            else ->
                                VilkårResultat(
                                    personResultat = personResultat,
                                    erAutomatiskVurdert = false,
                                    resultat = Resultat.IKKE_VURDERT,
                                    vilkårType = vilkår,
                                    begrunnelse = "",
                                    periodeFom = null,
                                    behandlingId = behandling.id,
                                )
                        }
                    }.toSortedSet(VilkårResultat.VilkårResultatComparator)

                personResultat.setSortedVilkårResultater(vilkårResultater)

                personResultat
            }.toSet()
    }
}

fun Vilkårsvurdering.oppdaterMedDødsdatoer(personopplysningGrunnlag: PersonopplysningGrunnlag) {
    this.personResultater.forEach { personResultat ->
        val dødsDato = personopplysningGrunnlag.personer.single { it.aktør == personResultat.aktør }.dødsfall?.dødsfallDato

        val vikårResultaterOppdatertMedDødsdato =
            if (dødsDato != null) {
                personResultat.vilkårResultater
                    .map {
                        val erDødsfallFørVilkårStarter = (it.periodeFom ?: TIDENES_MORGEN).isAfter(dødsDato)
                        val erDødsfallFørVilkårSlutter = (it.periodeTom ?: TIDENES_ENDE).isAfter(dødsDato)

                        when {
                            // Ønsker ikke å fjerne vilkår resultater,
                            // så lar saksbehandleren avgjøre hva som skjer når vilkået saterter før person dør
                            erDødsfallFørVilkårStarter -> it
                            erDødsfallFørVilkårSlutter -> it.kopier(periodeTom = dødsDato, begrunnelse = "Dødsfall")
                            else -> it
                        }
                    }
            } else {
                personResultat.vilkårResultater
            }

        personResultat.setSortedVilkårResultater(vikårResultaterOppdatertMedDødsdato.toSet())
    }
}

fun Vilkårsvurdering.kopierResultaterFraForrigeBehandling(vilkårsvurderingForrigeBehandling: Vilkårsvurdering) {
    personResultater.forEach { initieltPersonResultat ->
        val personResultatForrigeBehandling =
            vilkårsvurderingForrigeBehandling.personResultater.find {
                it.aktør == initieltPersonResultat.aktør
            }

        val oppdaterteVilkårResultater =
            if (personResultatForrigeBehandling == null) {
                initieltPersonResultat.vilkårResultater
            } else {
                initieltPersonResultat.vilkårResultater
                    .overskrivMedVilkårResultaterFraForrigeBehandling(
                        kunForGodkjenteVilkår = behandling.type != BehandlingType.FØRSTEGANGSBEHANDLING,
                        vilkårResultaterFraForrigeBehandling = personResultatForrigeBehandling.vilkårResultater,
                        nyttPersonResultat = initieltPersonResultat,
                    )
            }

        initieltPersonResultat.setSortedVilkårResultater(oppdaterteVilkårResultater.toSet())
    }
}

private fun Collection<VilkårResultat>.overskrivMedVilkårResultaterFraForrigeBehandling(
    vilkårResultaterFraForrigeBehandling: Collection<VilkårResultat>,
    nyttPersonResultat: PersonResultat,
    kunForGodkjenteVilkår: Boolean,
) = flatMap { initeltVilkårResultat ->
    val vilkårResultaterForrigeBehandlingSomViØnskerÅTaMed =
        vilkårResultaterFraForrigeBehandling
            .filter { it.vilkårType == initeltVilkårResultat.vilkårType }
            .filter { !kunForGodkjenteVilkår || it.resultat in listOf(Resultat.IKKE_AKTUELT, Resultat.OPPFYLT) }
            .map { it.kopier(personResultat = nyttPersonResultat) }

    vilkårResultaterForrigeBehandlingSomViØnskerÅTaMed.ifEmpty {
        listOf(initeltVilkårResultat)
    }
}
