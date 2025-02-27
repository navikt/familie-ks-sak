package no.nav.familie.ks.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ks.sak.common.domeneparser.BrevPeriodeParser
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ks.sak.common.domeneparser.norskDatoFormatter
import no.nav.familie.ks.sak.common.domeneparser.parseEnum
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriBoolean
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriEnum
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriInt
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriString
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDtoMedData
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelseMedKompetanseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelseUtenKompetanseDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalOgFellesBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import java.time.LocalDate

enum class Begrunnelsetype {
    EØS,
    STANDARD,
}

fun parseBegrunnelser(dataTable: DataTable): List<BegrunnelseDtoMedData> =
    dataTable.asMaps().map { rad: Tabellrad ->

        val type =
            parseValgfriEnum<Begrunnelsetype>(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.TYPE,
                rad,
            ) ?: Begrunnelsetype.STANDARD

        when (type) {
            Begrunnelsetype.STANDARD -> parseNasjonalEllerFellesBegrunnelse(rad)
            Begrunnelsetype.EØS -> parseEøsBegrunnelse(rad)
        }
    }

fun parseNasjonalEllerFellesBegrunnelse(rad: Tabellrad): BegrunnelseDtoMedData {
    val begrunnelse =
        parseEnum<NasjonalEllerFellesBegrunnelse>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BEGRUNNELSE,
            rad,
        )

    return NasjonalOgFellesBegrunnelseDataDto(
        vedtakBegrunnelseType = begrunnelse.begrunnelseType,
        apiNavn = begrunnelse.sanityApiNavn,
        sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
        gjelderSoker = parseValgfriBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_SØKER, rad) ?: false,
        gjelderAndreForelder = parseValgfriBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_ANDRE_FORELDER, rad) ?: false,
        barnasFodselsdatoer =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BARNAS_FØDSELSDATOER,
                rad,
            ) ?: "",
        antallBarn = parseValgfriInt(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_BARN, rad) ?: 0,
        maanedOgAarBegrunnelsenGjelderFor =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅNED_OG_ÅR_BEGRUNNELSEN_GJELDER_FOR,
                rad,
            ),
        maalform =
            (parseValgfriEnum<Målform>(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM, rad) ?: Målform.NB)
                .tilSanityFormat(),
        belop = parseValgfriString(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BELØP, rad)?.replace(' ', ' ') ?: "",
        antallTimerBarnehageplass = parseValgfriString(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_TIMER_BARNEHAGEPLASS, rad) ?: "",
        soknadstidspunkt =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.SØKNADSTIDSPUNKT,
                rad,
            ) ?: "",
        maanedOgAarFoerVedtaksperiode =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅNED_OG_ÅR_FØR_VEDTAKSPERIODE,
                rad,
            ),
    )
}

fun parseEøsBegrunnelse(rad: Tabellrad): EØSBegrunnelseDto {
    val gjelderSoker = parseValgfriBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_SØKER, rad)

    val annenForeldersAktivitet =
        parseValgfriEnum<KompetanseAktivitet>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET,
            rad,
        )

    val antallTimerBarnehageplass = parseValgfriString(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_TIMER_BARNEHAGEPLASS, rad) ?: ""

    val annenForeldersAktivitetsland =
        parseValgfriString(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND,
            rad,
        )
    val barnetsBostedsland =
        parseValgfriString(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND,
            rad,
        )
    val søkersAktivitet =
        parseValgfriEnum<KompetanseAktivitet>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET,
            rad,
        )
    val søkersAktivitetsland =
        parseValgfriString(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND,
            rad,
        )

    val begrunnelse =
        parseEnum<EØSBegrunnelse>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BEGRUNNELSE,
            rad,
        )

    val barnasFodselsdatoer =
        parseValgfriString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BARNAS_FØDSELSDATOER,
            rad,
        ) ?: ""

    val antallBarn = parseValgfriInt(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_BARN, rad) ?: -1

    val målform = (parseValgfriEnum<Målform>(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM, rad) ?: Målform.NB).tilSanityFormat()

    return if (gjelderSoker == null) {
        if (annenForeldersAktivitet == null ||
            annenForeldersAktivitetsland == null ||
            barnetsBostedsland == null ||
            søkersAktivitet == null ||
            søkersAktivitetsland == null
        ) {
            error("For EØS-begrunnelser må enten 'Gjelder søker' eller kompetansefeltene settes")
        }

        EØSBegrunnelseMedKompetanseDto(
            vedtakBegrunnelseType = begrunnelse.begrunnelseType,
            apiNavn = begrunnelse.sanityApiNavn,
            barnasFodselsdatoer = barnasFodselsdatoer,
            antallBarn = antallBarn,
            maalform = målform,
            annenForeldersAktivitet = annenForeldersAktivitet,
            annenForeldersAktivitetsland = annenForeldersAktivitetsland,
            barnetsBostedsland = barnetsBostedsland,
            sokersAktivitet = søkersAktivitet,
            sokersAktivitetsland = søkersAktivitetsland,
            sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
            antallTimerBarnehageplass = antallTimerBarnehageplass,
        )
    } else {
        EØSBegrunnelseUtenKompetanseDto(
            vedtakBegrunnelseType = begrunnelse.begrunnelseType,
            apiNavn = begrunnelse.sanityApiNavn,
            barnasFodselsdatoer = barnasFodselsdatoer,
            antallBarn = antallBarn,
            maalform = målform,
            gjelderSoker = gjelderSoker,
            sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
            antallTimerBarnehageplass = antallTimerBarnehageplass,
        )
    }
}

fun parseNullableDato(fom: String) =
    if (fom.uppercase() in listOf("NULL", "-", "")) {
        null
    } else {
        LocalDate.parse(
            fom,
            norskDatoFormatter,
        )
    }
