package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erBack2BackIMånedsskifte
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate

fun standardbegrunnelserTilNedtrekksmenytekster(sanityBegrunnelser: List<SanityBegrunnelse>): Map<BegrunnelseType, List<VedtakBegrunnelseTilknyttetVilkårResponseDto>> =
    (NasjonalEllerFellesBegrunnelse.entries + EØSBegrunnelse.entries)
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

    val eksisterendeVilkårsResultat =
        eksisterendeVilkårResultater.singleOrNull { it.id == endretVilkårResultatDto.id }
            ?: throw FunksjonellFeil(
                melding = "Fant ikke eksisterende vilkårsresultat med id: ${endretVilkårResultatDto.id}. Mulig knyttet til problem med flere faner",
                frontendFeilmelding = "Vilkårene har endret seg siden sist gang du lastet inn siden. Vennligst forsøk å laste inn siden på nytt",
            )

    val endretVilkårResultat = endretVilkårResultatDto.tilVilkårResultat(eksisterendeVilkårsResultat)

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

        endretVilkårResultat.erAvslagUtenPeriode() && filtrerteVilkårResultater.any { it.resultat == Resultat.OPPFYLT } ->
            throw FunksjonellFeil(
                "Finnes oppfylte perioder ved forsøk på å legge til avslag uten periode ",
                "Du kan ikke legge til avslagperiode uten datoer fordi det finnes oppfylte perioder på vilkåret. Disse må fjernes først.",
            )

        endretVilkårResultat.resultat == Resultat.OPPFYLT && filtrerteVilkårResultater.any { it.erAvslagUtenPeriode() } ->
            throw FunksjonellFeil(
                "Finnes avslag uten periode ved forsøk på å legge til løpende oppfylt",
                "Du kan ikke legge til perioden fordi det er vurdert avslag uten datoer på vilkåret. Denne må fjernes først.",
            )
    }
}

fun List<VilkårResultat>.tilTidslinje(): Tidslinje<VilkårResultat> =
    map {
        Periode(
            verdi = it,
            fom = it.periodeFom,
            tom = it.periodeTom,
        )
    }.tilTidslinje()

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

fun genererInitiellVilkårsvurdering(
    behandling: Behandling,
    forrigeVilkårsvurdering: Vilkårsvurdering?,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): Vilkårsvurdering =
    Vilkårsvurdering(behandling = behandling).apply {
        personResultater =
            personopplysningGrunnlag.personer
                .map { person ->
                    val personResultat = PersonResultat(vilkårsvurdering = this, aktør = person.aktør)

                    val forrigeBehandlingHaddeEøsSpesifikkeVilkår =
                        forrigeVilkårsvurdering
                            ?.personResultater
                            ?.flatMap { it.vilkårResultater }
                            ?.any { it.vilkårType.eøsSpesifikt } ?: false
                    val behandlingKategoriErEøs = behandling.kategori == BehandlingKategori.EØS

                    val skalHenteEøsSpesifikkeVilkår = behandlingKategoriErEøs || forrigeBehandlingHaddeEøsSpesifikkeVilkår

                    val vilkårForPerson = Vilkår.hentVilkårFor(person.type, skalHenteEøsSpesifikkeVilkår)

                    val vilkårResultater =
                        vilkårForPerson
                            .flatMap { vilkår ->
                                // prefyller diverse vilkår automatisk basert på type
                                when (vilkår) {
                                    Vilkår.BARNETS_ALDER -> {
                                        lagAutomatiskGenererteVilkårForBarnetsAlder(
                                            personResultat = personResultat,
                                            behandlingId = behandling.id,
                                            fødselsdato = person.fødselsdato,
                                            adopsjonsdato = adopsjonerIBehandling.firstOrNull { it.aktør == personResultat.aktør }?.adopsjonsdato,
                                        )
                                    }

                                    Vilkår.MEDLEMSKAP ->
                                        listOf(
                                            VilkårResultat(
                                                personResultat = personResultat,
                                                erAutomatiskVurdert = false,
                                                resultat = Resultat.IKKE_VURDERT,
                                                vilkårType = vilkår,
                                                begrunnelse = "",
                                                periodeFom = person.fødselsdato.plusYears(5),
                                                behandlingId = behandling.id,
                                            ),
                                        )

                                    Vilkår.BARNEHAGEPLASS -> {
                                        listOf(
                                            VilkårResultat(
                                                personResultat = personResultat,
                                                erAutomatiskVurdert = false,
                                                resultat = Resultat.OPPFYLT,
                                                vilkårType = vilkår,
                                                begrunnelse = "",
                                                periodeFom = person.fødselsdato,
                                                behandlingId = behandling.id,
                                            ),
                                        )
                                    }

                                    else ->
                                        listOf(
                                            VilkårResultat(
                                                personResultat = personResultat,
                                                erAutomatiskVurdert = false,
                                                resultat = Resultat.IKKE_VURDERT,
                                                vilkårType = vilkår,
                                                begrunnelse = "",
                                                periodeFom = null,
                                                behandlingId = behandling.id,
                                            ),
                                        )
                                }
                            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

                    personResultat.setSortedVilkårResultater(vilkårResultater)

                    personResultat
                }.toSet()
    }

fun Vilkårsvurdering.oppdaterMedDødsdatoer(personopplysningGrunnlag: PersonopplysningGrunnlag) {
    this.personResultater.forEach { personResultat ->
        val dødsDato =
            personopplysningGrunnlag.personer
                .single { it.aktør == personResultat.aktør }
                .dødsfall
                ?.dødsfallDato

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

fun Vilkårsvurdering.kopierResultaterFraForrigeBehandling(
    vilkårsvurderingForrigeBehandling: Vilkårsvurdering,
) {
    personResultater.forEach { initieltPersonResultat ->
        val personResultatForrigeBehandling =
            vilkårsvurderingForrigeBehandling.personResultater.find {
                it.aktør == initieltPersonResultat.aktør
            }

        val oppdaterteVilkårResultater =
            if (personResultatForrigeBehandling == null) {
                initieltPersonResultat.vilkårResultater
            } else {
                initieltPersonResultat
                    .overskrivMedVilkårResultaterFraForrigeBehandling(
                        vilkårResultaterFraForrigeBehandling = personResultatForrigeBehandling.vilkårResultater,
                    )
            }

        initieltPersonResultat.setSortedVilkårResultater(oppdaterteVilkårResultater.toSet())
    }
}

private fun PersonResultat.overskrivMedVilkårResultaterFraForrigeBehandling(
    vilkårResultaterFraForrigeBehandling: Collection<VilkårResultat>,
): List<VilkårResultat> {
    val vilkårForPerson = this.vilkårResultater.map { it.vilkårType }.toSet()

    return vilkårForPerson.flatMap { vilkårType ->
        val vilkårResultaterAvSammeType: List<VilkårResultat> =
            this.vilkårResultater.filter { it.vilkårType == vilkårType }

        val vilkårResultaterAvSammeTypeIForrigeBehandling =
            vilkårResultaterFraForrigeBehandling
                .filter { it.vilkårType == vilkårType }
                .map { it.kopier(personResultat = this) }

        val vilkårResultaterForrigeBehandlingSomViØnskerÅTaMed: List<VilkårResultat> =
            when (vilkårType) {
                Vilkår.BARNEHAGEPLASS -> {
                    /* *
                     * Ønsker å dra med vilkårresultatene som er avslått og opphørt i forrige behandling
                     * for barnehagevilkåret fordi vi krever at alle peridene skal være vurdert, også de med opphør
                     *
                     * kopierer ikke med eksplisitt avslag på søknad for dette ikke vil validere med
                     * validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag ved revurdering av
                     * en sak som har hatt eksplisitt avslag i forrige behandling.
                     * */
                    vilkårResultaterAvSammeTypeIForrigeBehandling.filter { it.erEksplisittAvslagPåSøknad == true && it.vilkårType == Vilkår.BARNEHAGEPLASS }.forEach { it.erEksplisittAvslagPåSøknad = null }
                    vilkårResultaterAvSammeTypeIForrigeBehandling
                }

                Vilkår.BARNETS_ALDER -> {
                    /* *
                     * Barnets alder vilkåret settes automatisk og bør ikke endres med mindre det er snakk om adopsjon.
                     * Kopierer derfor kun vilkåret ved adopsjon og tar med som er generert på denne behandlingen ellers.
                     * På denne måten kan vi få med regelendringer som endrer vilkåret på revurderinger.
                     * */
                    vilkårResultaterAvSammeTypeIForrigeBehandling
                        .filter { it.erAdopsjonOppfylt() }
                        .filter { it.resultat in listOf(Resultat.IKKE_AKTUELT, Resultat.OPPFYLT) }
                        .forkortTomTilGyldigLengde()
                        .splittOppOmKrysserRegelverksendring()
                }

                else ->
                    vilkårResultaterAvSammeTypeIForrigeBehandling
                        .filter { it.resultat in listOf(Resultat.IKKE_AKTUELT, Resultat.OPPFYLT) }
            }

        if (vilkårResultaterForrigeBehandlingSomViØnskerÅTaMed.isNotEmpty()) {
            vilkårResultaterForrigeBehandlingSomViØnskerÅTaMed
        } else {
            vilkårResultaterAvSammeType
        }
    }
}

fun Collection<VilkårResultat>.splittOppOmKrysserRegelverksendring(): List<VilkårResultat> =
    this.flatMap {
        if (it.krysserRegelendring()) {
            listOf(
                it.kopier(periodeTom = DATO_LOVENDRING_2024.minusDays(1)),
                it.kopier(periodeFom = DATO_LOVENDRING_2024),
            )
        } else {
            listOf(it)
        }
    }

private fun VilkårResultat.krysserRegelendring() =
    (periodeFom ?: TIDENES_MORGEN).isBefore(DATO_LOVENDRING_2024) &&
        (periodeTom ?: TIDENES_ENDE).erSammeEllerEtter(DATO_LOVENDRING_2024)

fun Collection<VilkårResultat>.forkortTomTilGyldigLengde(): List<VilkårResultat> =
    this.flatMap {
        it.periodeFom ?: throw IllegalStateException("Barnets alder vilkår kan ikke begynne tidenes morgen")
        it.periodeTom ?: throw IllegalStateException("Barnets alder vilkår kan ikke ende ved tidenes ende")

        val lengdePåPeriode = it.periodeFom!!.until(it.periodeTom).toTotalMonths()
        val fomTilLovendringsDato = it.periodeFom!!.until(DATO_LOVENDRING_2024).toTotalMonths()

        when {
            fomTilLovendringsDato < 7 && lengdePåPeriode > 7 && it.periodeTom!! >= DATO_LOVENDRING_2024 -> {
                listOf(it.kopier(periodeTom = it.periodeFom!!.plusMonths(7)))
            }
            fomTilLovendringsDato > 7 && it.periodeTom!! >= DATO_LOVENDRING_2024 -> {
                listOf(it.kopier(periodeTom = DATO_LOVENDRING_2024.minusDays(1)))
            }
            else -> {
                listOf(it)
            }
        }
    }
