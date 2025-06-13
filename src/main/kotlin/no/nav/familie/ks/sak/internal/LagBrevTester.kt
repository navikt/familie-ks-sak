package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilMånedÅr
import no.nav.familie.ks.sak.common.util.tilddMMyyyy
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.LocalDate

fun lagBrevTest(
    behandling: Behandling,
    forrigeBehandling: Behandling?,
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
    personResultater: Set<PersonResultat>,
    personResultaterForrigeBehandling: Set<PersonResultat>?,
    andeler: List<AndelTilkjentYtelse>,
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    endredeUtbetalingerForrigeBehandling: List<EndretUtbetalingAndel>?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
    utenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
    utenlandskePeriodebeløpForrigeBehandling: List<UtenlandskPeriodebeløp>?,
    valutakurser: List<Valutakurs>,
    valutakurserForrigeBehandling: List<Valutakurs>?,
): String {
    val test =
        """
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - ${SecureRandom().nextLong()}

  Bakgrunn:""" +
            hentTekstForFagsak() +
            hentTekstForBehandlinger(behandling, forrigeBehandling) +
            hentTekstForPersongrunnlag(persongrunnlag, persongrunnlagForrigeBehandling) +
            """
      
  Scenario: Plassholdertekst for scenario - ${SecureRandom().nextLong()}
    Og følgende dagens dato ${LocalDate.now().tilddMMyyyy()}""" +
            hentTekstForVilkårresultater(
                personResultaterForrigeBehandling?.sorterPåFødselsdato(persongrunnlagForrigeBehandling!!),
                forrigeBehandling?.id,
            ) +
            hentTekstForVilkårresultater(personResultater.sorterPåFødselsdato(persongrunnlag), behandling.id) +

            hentTekstForEndretUtbetaling(endredeUtbetalinger, endredeUtbetalingerForrigeBehandling) +

            hentTekstForKompetanse(kompetanse, behandling.id) +
            hentTekstForKompetanse(kompetanseForrigeBehandling, forrigeBehandling?.id) +
            hentTekstForUtenlandskPeriodebeløp(utenlandskePeriodebeløpForrigeBehandling, forrigeBehandling?.id) +
            hentTekstForUtenlandskPeriodebeløp(utenlandskePeriodebeløp, behandling.id) +
            hentTekstForValutakurs(valutakurserForrigeBehandling, forrigeBehandling?.id) +
            hentTekstForValutakurs(valutakurser, behandling.id) +

            hentTekstForTilkjentYtelse(andeler, persongrunnlag, forrigeBehandling?.id, behandling.id) +
            hentTekstForBehandlingsresultat(behandling.id, behandling.resultat) +

            hentTekstForVedtaksperioder(behandling.id, vedtaksperioder) +

            hentTekstForGyligeBegrunnelserForVedtaksperiodene(vedtaksperioder, behandling.id) +
            hentTekstValgteBegrunnelser(behandling.id, vedtaksperioder) +
            hentTekstBrevPerioder(behandling.id, vedtaksperioder) +
            hentEØSBrevBegrunnelseTekster(behandling.id, vedtaksperioder) +
            hentBrevBegrunnelseTekster(behandling.id, vedtaksperioder) + """
"""
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
) = """

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak  | Behandlingskategori | Behandlingsstatus | ${
    forrigeBehandling?.let {
        """ 
      | ${it.id} | 1 |           | ${it.opprettetÅrsak}  | ${it.kategori} | ${it.status} |"""
    } ?: ""
}
      | ${behandling.id} | 1 | ${forrigeBehandling?.id ?: ""} | ${behandling.opprettetÅrsak}  | ${behandling.kategori} | ${behandling.status} |"""

fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) = """
    
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
      | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Søker har meldt fra om barnehageplass | Antall timer |""" +
        tilVilkårResultatRader(personResultater)
}

private fun Set<PersonResultat>.sorterPåFødselsdato(persongrunnlag: PersonopplysningGrunnlag) = this.sortedBy { personresultat -> persongrunnlag.personer.single { personresultat.aktør == it.aktør }.fødselsdato }

data class VilkårResultatRad(
    val aktørId: String,
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val resultat: Resultat,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val standardbegrunnelser: List<IBegrunnelse>,
    val vurderesEtter: Regelverk?,
    val søkerHarMeldtFraOmBarnehageplass: Boolean?,
    val antallTimer: BigDecimal?,
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
                    it.søkerHarMeldtFraOmBarnehageplass,
                    it.antallTimer,
                )
            }.toList()
            .joinToString("") { (vilkårResultatRad, vilkårResultater) ->
                "\n | ${vilkårResultatRad.aktørId} " +
                    "| ${vilkårResultater.map { it.vilkårType }.joinToString(",")} " +
                    "| ${vilkårResultatRad.utdypendeVilkårsvurderinger.joinToString(",")} " +
                    "| ${vilkårResultatRad.fom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.tom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.resultat} " +
                    "| ${if (vilkårResultatRad.erEksplisittAvslagPåSøknad == true) "Ja" else "Nei"} " +
                    "| ${vilkårResultatRad.standardbegrunnelser.joinToString(",")}" +
                    "| ${vilkårResultatRad.vurderesEtter ?: ""} " +
                    "| ${if (vilkårResultatRad.søkerHarMeldtFraOmBarnehageplass == true) "Ja" else "Nei"} " +
                    "| ${vilkårResultatRad.antallTimer ?: ""} " +
                    "| "
            }
    } ?: ""

fun hentTekstForTilkjentYtelse(
    andeler: List<AndelTilkjentYtelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    forrigeBehandlingId: Long?,
    behandlingId: Long,
) = """
    ${(forrigeBehandlingId?.let { "\nOg andeler er beregnet for behandling $it\n" } ?: "")} 
    Og andeler er beregnet for behandling $behandlingId
    
    Så forvent følgende andeler tilkjent ytelse for behandling $behandlingId
      | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp | """ +
    hentAndelRader(andeler, persongrunnlag)

fun hentTekstForBehandlingsresultat(
    behandlingId: Long,
    behandlingsresultat: Behandlingsresultat,
) = """
    Og når behandlingsresultatet er utledet for behandling $behandlingId
    Så forvent at behandlingsresultatet er $behandlingsresultat på behandling $behandlingId
    """

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
        )?.joinToString("") {
            """
      | ${it.aktør.aktørId} |${
                it.stønadFom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.stønadTom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.kalkulertUtbetalingsbeløp}| ${it.type} | ${it.prosent} | ${it.sats} | ${it.nasjonaltPeriodebeløp} | ${it.differanseberegnetPeriodebeløp ?: ""} | """
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

    Og følgende endrede utbetalinger
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
      | ${it.personer.joinToString(", ") { person -> person.aktør.aktørId }} |${it.behandlingId}|${
                it.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.tom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.årsak} | ${it.prosent} | ${it.søknadstidspunkt.tilddMMyyyy()} |"""
        } ?: ""

private fun hentTekstForKompetanse(
    kompetanser: Collection<Kompetanse>?,
    behandlingId: Long?,
): String {
    val rader = hentKompetanseRader(kompetanser)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og følgende kompetanser for behandling $behandlingId
      | AktørId | Fra dato | Til dato | Resultat | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |""" +
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

private fun hentTekstForUtenlandskPeriodebeløp(
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>?,
    behandlingId: Long?,
): String {
    val rader = hentUtenlandskePeriodebeløpRader(utenlandskePeriodebeløp)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og følgende utenlandske periodebeløp for behandling $behandlingId
      | AktørId | Fra dato | Til dato | Beløp | Valuta kode | Intervall | Utbetalingsland |""" +
            rader
    }
}

private fun hentUtenlandskePeriodebeløpRader(utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>?): String =
    utenlandskePeriodebeløp
        ?.joinToString("") { valutenlandskPeriodebeløp ->
            """
      | ${
                valutenlandskPeriodebeløp.barnAktører.joinToString(", ") { it.aktørId }
            } | ${
                valutenlandskPeriodebeløp.fom?.førsteDagIInneværendeMåned()?.tilddMMyyyy() ?: ""
            } | ${
                valutenlandskPeriodebeløp.tom?.sisteDagIInneværendeMåned()?.tilddMMyyyy() ?: ""
            } | ${
                valutenlandskPeriodebeløp.beløp ?: ""
            } | ${
                valutenlandskPeriodebeløp.valutakode ?: ""
            } | ${
                valutenlandskPeriodebeløp.intervall ?: ""
            } | ${
                valutenlandskPeriodebeløp.utbetalingsland ?: ""
            }|"""
        } ?: ""

private fun hentTekstForValutakurs(
    valutakurser: Collection<Valutakurs>?,
    behandlingId: Long?,
): String {
    val rader = hentValutakursRader(valutakurser)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og følgende valutakurser for behandling $behandlingId
      | AktørId | Fra dato | Til dato | Valutakursdato | Valuta kode | Kurs |""" +
            rader
    }
}

private fun hentValutakursRader(valutakurser: Collection<Valutakurs>?): String =
    valutakurser
        ?.joinToString("") { valutakurs ->
            """
      | ${
                valutakurs.barnAktører.joinToString(", ") { it.aktørId }
            } | ${
                valutakurs.fom?.førsteDagIInneværendeMåned()?.tilddMMyyyy() ?: ""
            } | ${
                valutakurs.tom?.sisteDagIInneværendeMåned()?.tilddMMyyyy() ?: ""
            } | ${
                valutakurs.valutakursdato ?: ""
            } | ${
                valutakurs.valutakode ?: ""
            } | ${
                valutakurs.kurs ?: ""
            }|"""
        } ?: ""

private fun hentTekstForVedtaksperioder(
    behandlingId: Long,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) = """
        
    Og vedtaksperioder er laget for behandling $behandlingId        
        
    Så forvent følgende vedtaksperioder på behandling $behandlingId
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |""" +
    hentVedtaksperiodeRader(vedtaksperioder)

private fun hentVedtaksperiodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") {
        """
      | ${it.fom?.tilddMMyyyy() ?: ""} |${it.tom?.tilddMMyyyy() ?: ""} |${it.type} |               |"""
    }

fun hentTekstForGyligeBegrunnelserForVedtaksperiodene(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
    behandlingId: Long?,
) = """

    Så forvent at følgende begrunnelser er gyldige for behandling $behandlingId
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |""" +
    hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder)

fun hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | | ${vedtaksperiode.begrunnelser.joinToString { it.nasjonalEllerFellesBegrunnelse.name }} | |""" +
            if (vedtaksperiode.eøsBegrunnelser.isNotEmpty()) {
                """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | EØS_FORORDNINGEN | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | |"""
            } else {
                ""
            }
    }

fun hentTekstValgteBegrunnelser(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) = """

    Og når disse begrunnelsene er valgt for behandling $behandlingId
        | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |""" +
    hentValgteBegrunnelserRader(vedtaksperioder)

fun hentValgteBegrunnelserRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} | ${vedtaksperiode.begrunnelser.joinToString { it.nasjonalEllerFellesBegrunnelse.name }} | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | |"""
    }

fun hentTekstBrevPerioder(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) = """

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
): String =
    vedtaksperioder.filter { (it.begrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
        | Begrunnelse | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder | Målform | Måned og år før vedtaksperiode |""" +
            vedtaksperiode.begrunnelser.map { it.nasjonalEllerFellesBegrunnelse }.joinToString("") {
                """
        | $it | STANDARD |               |                      |             |                                      |         |       |                  |                         |                               |         |"""
            }
    }

fun hentEØSBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String =
    vedtaksperioder.filter { (it.eøsBegrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
        | Begrunnelse | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn | Annen forelders aktivitet | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitet | Søkers aktivitetsland | Målform |""" +
            vedtaksperiode.eøsBegrunnelser.map { it.begrunnelse }.joinToString("") {
                """
        | $it | EØS |               |                      |             |                                      |         |       |                  |                         |                               |"""
            }
    }
