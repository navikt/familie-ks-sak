package no.nav.familie.ba.sak.internal.vedtak.begrunnelser

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilddMMyyyy
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.UtfyltEndretUtbetalingAndelDeltBosted
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDate

fun lagBrevTest(
    behandling: Behandling,
    forrigeBehandling: Behandling?,
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
    personResultater: Set<PersonResultat>,
    personResultaterForrigeBehandling: Set<PersonResultat>?,
    andeler: List<AndelTilkjentYtelse>,
    andelerForrigeBehandling: List<AndelTilkjentYtelse>?,
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    endredeUtbetalingerForrigeBehandling: List<EndretUtbetalingAndel>?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
): String {
    val test =
        """
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - ${RandomStringUtils.randomAlphanumeric(10)}

  Bakgrunn:""" +
            hentTekstForFagsak() +
            hentTekstForBehandlinger(behandling, forrigeBehandling) +
            hentTekstForPersongrunnlag(persongrunnlag, persongrunnlagForrigeBehandling) +
            """
      
  Scenario: Plassholdertekst for scenario - ${RandomStringUtils.randomAlphanumeric(10)}
    Og følgende dagens dato ${LocalDate.now().tilddMMyyyy()}""" +
            hentTekstForVilkårresultater(
                personResultaterForrigeBehandling?.sorterPåFødselsdato(persongrunnlagForrigeBehandling!!),
                forrigeBehandling?.id,
            ) +
            hentTekstForVilkårresultater(personResultater.sorterPåFødselsdato(persongrunnlag), behandling.id) +
            hentTekstForTilkjentYtelse(andeler, persongrunnlag, andelerForrigeBehandling, persongrunnlagForrigeBehandling, behandling.id) +
            hentTekstForEndretUtbetaling(endredeUtbetalinger, endredeUtbetalingerForrigeBehandling) +
            hentTekstForKompetanse(kompetanse, kompetanseForrigeBehandling) +
            hentTekstForVedtaksperioder(behandling.id, vedtaksperioder) + """
    
    Når vedtaksperiodene genereres for behandling ${behandling.id}"""
    // + hentTekstForGyligeBegrunnelserForVedtaksperiodene(vedtaksperioder) +
    // hentTekstValgteBegrunnelser(behandling.id, vedtaksperioder) +
    // hentTekstBrevPerioder(behandling.id, vedtaksperioder) +
    // hentEØSBrevBegrunnelseTekster(behandling.id, vedtaksperioder) +
    // hentBrevBegrunnelseTekster(behandling.id, vedtaksperioder) + """
// """
    return test.anonymiser(persongrunnlag, persongrunnlagForrigeBehandling, forrigeBehandling, behandling)
}

fun String.anonymiser(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
    forrigeBehandling: Behandling?,
    behandling: Behandling,
): String {
    val personerSomTestes: Set<Person> =
        persongrunnlag.personer.toSet() + (persongrunnlagForrigeBehandling?.personer?.toSet() ?: emptySet())
    val aktørIder = personerSomTestes.sortedBy { it.fødselsdato }.map { it.aktør.aktørId }

    val behandlinger = listOfNotNull(forrigeBehandling?.id, behandling.id)

    val testMedAnonymeAktørIder =
        aktørIder.foldIndexed(this) { index, acc, aktørId ->
            acc.replace(aktørId, (index + 1).toString())
        }
    return behandlinger.foldIndexed(testMedAnonymeAktørIder) { index, acc, behandlingId ->
        acc.replace(behandlingId.toString(), (index + 1).toString())
    }
}

fun hentTekstForFagsak() =
    """
    Gitt følgende fagsaker
      | FagsakId | 
      | 1        | """

fun hentTekstForBehandlinger(
    behandling: Behandling,
    forrigeBehandling: Behandling?,
) =
    """

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak  | Behandlingskategori |${
        forrigeBehandling?.let {
            """ 
      | ${it.id} | 1 |           | ${it.resultat} | ${it.opprettetÅrsak}  | ${it.kategori} |"""
        } ?: ""
    }
      | ${behandling.id} | 1 | ${forrigeBehandling?.id ?: ""} |${behandling.resultat} | ${behandling.opprettetÅrsak}  | ${behandling.kategori} |"""

fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) =
    """
    
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |""" +
        hentPersongrunnlagRader(persongrunnlagForrigeBehandling) +
        hentPersongrunnlagRader(persongrunnlag)

private fun hentPersongrunnlagRader(persongrunnlag: PersonopplysningGrunnlag?): String =
    persongrunnlag?.personer?.sortedBy { it.fødselsdato }?.joinToString("") {
        """
      | ${persongrunnlag.behandlingId} |${it.aktør.aktørId}|${it.type}|${it.fødselsdato.tilddMMyyyy()}|"""
    } ?: ""

fun hentTekstForVilkårresultater(
    personResultater: List<PersonResultat>?,
    behandlingId: Long?,
): String {
    if (personResultater == null || behandlingId == null) {
        return ""
    }

    return """
        
    Og følgende vilkårresultater for behandling $behandlingId
      | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter |""" +
        tilVilkårResultatRader(personResultater)
}

private fun Set<PersonResultat>.sorterPåFødselsdato(persongrunnlag: PersonopplysningGrunnlag) =
    this.sortedBy { personresultat -> persongrunnlag.personer.single { personresultat.aktør == it.aktør }.fødselsdato }

data class VilkårResultatRad(
    val aktørId: String,
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val resultat: Resultat,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val standardbegrunnelser: List<IBegrunnelse>,
    val vurderesEtter: Regelverk?,
)

private fun tilVilkårResultatRader(personResultater: List<PersonResultat>?) =
    personResultater?.joinToString("\n") { personResultat ->
        personResultat.vilkårResultater
            .sortedBy { it.periodeFom }
            .groupBy {
                VilkårResultatRad(
                    personResultat.aktør.aktørId,
                    it.utdypendeVilkårsvurderinger.toSet(),
                    it.periodeFom,
                    it.periodeTom,
                    it.resultat,
                    it.erEksplisittAvslagPåSøknad,
                    it.begrunnelser,
                    it.vurderesEtter,
                )
            }.toList().joinToString("") { (vilkårResultatRad, vilkårResultater) ->
                "\n | ${vilkårResultatRad.aktørId} " +
                    "| ${vilkårResultater.map { it.vilkårType }.joinToString(",")} " +
                    "| ${vilkårResultatRad.utdypendeVilkårsvurderinger.joinToString(",")} " +
                    "| ${vilkårResultatRad.fom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.tom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.resultat} " +
                    "| ${if (vilkårResultatRad.erEksplisittAvslagPåSøknad == true) "Ja" else "Nei"} " +
                    "| ${vilkårResultatRad.standardbegrunnelser.joinToString(",")}" +
                    "| ${vilkårResultatRad.vurderesEtter ?: ""} " +
                    "| "
            }
    } ?: ""

fun hentTekstForTilkjentYtelse(
    andeler: List<AndelTilkjentYtelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    andelerForrigeBehandling: List<AndelTilkjentYtelse>?,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
    behandlingId: Long,
) =
    """
    ${"\n" + andelerForrigeBehandling?.let { hentAndelRader(andelerForrigeBehandling, persongrunnlagForrigeBehandling) + "\n" }} 
    Og andeler er beregnet for behandling $behandlingId
    
    Så forvent følgende andeler tilkjent ytelse for behandling
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | """ +
        hentAndelRader(andeler, persongrunnlag)

private fun hentAndelRader(
    andeler: List<AndelTilkjentYtelse>?,
    persongrunnlag: PersonopplysningGrunnlag?,
): String =
    andeler
        ?.sortedWith(
            compareBy(
                { persongrunnlag?.personer?.single { person -> person.aktør == it.aktør }?.fødselsdato },
                { it.stønadFom },
                { it.stønadTom },
            ),
        )
        ?.joinToString("") {
            """
      | ${it.aktør.aktørId} |${it.behandlingId}|${
                it.stønadFom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.stønadTom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.kalkulertUtbetalingsbeløp}| ${it.type} | ${it.prosent} | ${it.sats} | """
        } ?: ""

fun hentTekstForEndretUtbetaling(
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    endredeUtbetalingerForrigeBehandling: List<EndretUtbetalingAndel>?,
): String {
    val rader =
        hentEndretUtbetalingRader(endredeUtbetalingerForrigeBehandling) +
            hentEndretUtbetalingRader(endredeUtbetalinger)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med endrede utbetalinger
      | AktørId  | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |""" +
            hentEndretUtbetalingRader(endredeUtbetalingerForrigeBehandling) +
            hentEndretUtbetalingRader(endredeUtbetalinger)
    }
}

private fun hentEndretUtbetalingRader(endredeUtbetalinger: List<EndretUtbetalingAndel>?): String =
    endredeUtbetalinger
        ?.map { it.tilIEndretUtbetalingAndel() }
        ?.filterIsInstance<IUtfyltEndretUtbetalingAndel>()
        ?.joinToString("") {
            """
      | ${it.person.aktør.aktørId} |${it.behandlingId}|${
                it.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.tom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.årsak} | ${it.prosent} | ${it.søknadstidspunkt.tilddMMyyyy()} | ${if (it is UtfyltEndretUtbetalingAndelDeltBosted) it.avtaletidspunktDeltBosted else ""} |"""
        } ?: ""

fun hentTekstForKompetanse(
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
): String {
    val rader =
        hentKompetanseRader(kompetanseForrigeBehandling) +
            hentKompetanseRader(kompetanse)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med kompetanser
      | AktørId | Fra dato | Til dato | Resultat | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |""" +
            rader
    }
}

private fun hentKompetanseRader(kompetanser: Collection<Kompetanse>?): String =
    kompetanser
        ?.map { it.tilIKompetanse() }
        ?.filterIsInstance<UtfyltKompetanse>()
        ?.joinToString("") { kompetanse ->
            """
      | ${
                kompetanse.barnAktører.joinToString(", ") { it.aktørId }
            } |${
                kompetanse.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                kompetanse.tom?.sisteDagIInneværendeMåned()?.tilddMMyyyy() ?: ""
            }|${
                kompetanse.resultat
            }|${
                kompetanse.behandlingId
            }|${
                kompetanse.søkersAktivitet
            }|${
                kompetanse.annenForeldersAktivitet
            }|${
                kompetanse.søkersAktivitetsland
            }|${
                kompetanse.annenForeldersAktivitetsland ?: ""
            }|${
                kompetanse.barnetsBostedsland
            } |"""
        } ?: ""

private fun hentTekstForVedtaksperioder(
    behandlingId: Long,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """
        
    Og vedtaksperioder er laget for behandling $behandlingId        
        
    Så forvent følgende vedtaksperioder på behandling $behandlingId
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |""" +
        hentVedtaksperiodeRader(vedtaksperioder)

private fun hentVedtaksperiodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") {
        """
      | ${it.fom?.tilddMMyyyy() ?: ""} |${it.tom?.tilddMMyyyy() ?: ""} |${it.type} |               |"""
    }

/*fun hentTekstForGyligeBegrunnelserForVedtaksperiodene(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |""" +
        hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder)

fun hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | | ${vedtaksperiode.begrunnelser.joinToString { it.begrunnelse.name }} | |""" +
            if (
                // TODO bytt ut med vedtaksperiode.eøsBegrunnelser
                emptyList<EØSBegrunnelse>().isNotEmpty()
            ) {
                """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | EØS_FORORDNINGEN | ${
                    // vedtaksperiode.eøsBegrunnelser
                    emptyList<EØSBegrunnelse>().joinToString { it.name }
                } | |
                """
            } else {
                ""
            }
    }

fun hentTekstValgteBegrunnelser(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """

    Og når disse begrunnelsene er valgt for behandling $behandlingId
        | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |""" +
        hentValgteBegrunnelserRader(vedtaksperioder)

fun hentValgteBegrunnelserRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} | ${vedtaksperiode.begrunnelser.joinToString { it.begrunnelse.name }} | ${
            ""
            // vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }
        } | ${vedtaksperiode.fritekster.joinToString()} |"""
    }

fun hentTekstBrevPerioder(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """

    Så forvent følgende brevperioder for behandling $behandlingId
        | Brevperiodetype  | Fra dato   | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |""" +
        hentBrevPeriodeRader(vedtaksperioder)

fun hentBrevPeriodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | | ${vedtaksperiode.fom?.tilMånedÅr() ?: ""} | ${vedtaksperiode.tom?.tilMånedÅr() ?: ""} | | | | |"""
    }

fun hentBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String {
    return vedtaksperioder.filter { (it.begrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
        | Begrunnelse | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |""" +
            vedtaksperiode.begrunnelser.map { it.begrunnelse }.joinToString("") {
                """
        | $it | STANDARD |               |                      |             |                                      |         |       |                  |                         |                               |"""
            }
    }
}*/

/*fun hentEØSBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String {
    return vedtaksperioder.filter { (it.eøsBegrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
        | Begrunnelse | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | """ +
            vedtaksperiode.eøsBegrunnelser.map { it.begrunnelse }.joinToString("") {
                """
        | $it | EØS | | | | | | | | |"""
            }
    }
}*/