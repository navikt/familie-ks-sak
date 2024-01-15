package no.nav.familie.ks.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ks.sak.common.domeneparser.BrevPeriodeParser
import no.nav.familie.ks.sak.common.domeneparser.Domenebegrep
import no.nav.familie.ks.sak.common.domeneparser.parseEnum
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriInt
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriString
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeType

typealias Tabellrad = Map<String, String>

fun parseBrevPerioder(dataTable: DataTable): List<BrevPeriodeDto> {
    return dataTable.asMaps().map { rad: Tabellrad ->

        val beløp = parseValgfriString(BrevPeriodeParser.DomenebegrepBrevPeriode.BELØP, rad)?.replace(' ', ' ') ?: ""
        val antallBarn = parseValgfriInt(BrevPeriodeParser.DomenebegrepBrevPeriode.ANTALL_BARN, rad) ?: -1
        val barnasFodselsdager =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevPeriode.BARNAS_FØDSELSDAGER,
                rad,
            ) ?: ""
        val duEllerInstitusjonen =
            parseValgfriString(BrevPeriodeParser.DomenebegrepBrevPeriode.DU_ELLER_INSTITUSJONEN, rad) ?: "Du"

        BrevPeriodeDto(
            fom = parseValgfriString(Domenebegrep.FRA_DATO, rad) ?: "",
            tom = parseValgfriString(Domenebegrep.TIL_DATO, rad) ?: "",
            belop = beløp,
            // egen test for dette. Se `forvent følgende brevbegrunnelser for behandling i periode`()
            begrunnelser = emptyList(),
            brevPeriodeType = parseEnum<BrevPeriodeType>(BrevPeriodeParser.DomenebegrepBrevPeriode.TYPE, rad),
            antallBarn = antallBarn.toString(),
            barnasFodselsdager = barnasFodselsdager,
        )
    }
}
