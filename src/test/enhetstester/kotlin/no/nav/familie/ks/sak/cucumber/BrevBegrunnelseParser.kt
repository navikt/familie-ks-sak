package no.nav.familie.ks.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ks.sak.common.domeneparser.Domenebegrep
import no.nav.familie.ks.sak.common.domeneparser.Domenenøkkel
import no.nav.familie.ks.sak.common.domeneparser.parseEnum
import no.nav.familie.ks.sak.common.domeneparser.parseEnumListe
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriDato
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriEnum
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import java.time.LocalDate

data class SammenlignbarBegrunnelse(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val inkluderteStandardBegrunnelser: Set<IBegrunnelse>,
    val ekskluderteStandardBegrunnelser: Set<IBegrunnelse> = emptySet(),
)

object BrevBegrunnelseParser {
    fun mapBegrunnelser(dataTable: DataTable): List<SammenlignbarBegrunnelse> =
        dataTable.asMaps().map { rad ->
            val regelverkForInkluderteBegrunnelser =
                parseValgfriEnum<Regelverk>(DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.REGELVERK_INKLUDERTE_BEGRUNNELSER, rad)
                    ?: Regelverk.NASJONALE_REGLER

            val regelverkForEkskluderteBegrunnelser =
                parseValgfriEnum<Regelverk>(DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.REGELVERK_EKSKLUDERTE_BEGRUNNELSER, rad)
                    ?: regelverkForInkluderteBegrunnelser

            val inkluderteStandardBegrunnelser =
                hentForventedeBegrunnelser(
                    regelverkForInkluderteBegrunnelser,
                    DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.INKLUDERTE_BEGRUNNELSER,
                    rad,
                )
            val ekskluderteStandardBegrunnelser =
                hentForventedeBegrunnelser(
                    regelverkForEkskluderteBegrunnelser,
                    DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.EKSKLUDERTE_BEGRUNNELSER,
                    rad,
                )

            SammenlignbarBegrunnelse(
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                type = parseEnum(DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.VEDTAKSPERIODE_TYPE, rad),
                inkluderteStandardBegrunnelser = inkluderteStandardBegrunnelser,
                ekskluderteStandardBegrunnelser = ekskluderteStandardBegrunnelser,
            )
        }

    private fun hentForventedeBegrunnelser(
        vurderesEtter: Regelverk,
        inkludertEllerEkskludert: DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser,
        rad: Map<String, String>,
    ): Set<IBegrunnelse> =
        when (vurderesEtter) {
            Regelverk.NASJONALE_REGLER -> {
                parseEnumListe<NasjonalEllerFellesBegrunnelse>(
                    inkludertEllerEkskludert,
                    rad,
                ).toSet()
            }

            Regelverk.EØS_FORORDNINGEN -> {
                parseEnumListe<EØSBegrunnelse>(
                    inkludertEllerEkskludert,
                    rad,
                ).toSet()
            }
        }

    enum class DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser(
        override val nøkkel: String,
    ) : Domenenøkkel {
        VEDTAKSPERIODE_TYPE("VedtaksperiodeType"),
        INKLUDERTE_BEGRUNNELSER("Gyldige begrunnelser"),
        EKSKLUDERTE_BEGRUNNELSER("Ugyldige begrunnelser"),
        REGELVERK_INKLUDERTE_BEGRUNNELSER("Regelverk Gyldige begrunnelser"),
        REGELVERK_EKSKLUDERTE_BEGRUNNELSER("Regelverk Ugyldige begrunnelser"),
    }
}
