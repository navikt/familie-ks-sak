package no.nav.familie.ks.sak.common.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenenøkkel
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse

object VedtaksperiodeMedBegrunnelserParser {
    fun mapForventetVedtaksperioderMedBegrunnelser(
        dataTable: DataTable,
        vedtak: Vedtak,
    ): List<VedtaksperiodeMedBegrunnelser> {
        return dataTable.asMaps().map { rad ->
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                type = parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.VEDTAKSPERIODE_TYPE, rad),
            ).also { vedtaksperiodeMedBegrunnelser ->
                val begrunnelser =
                    parseEnumListe<Begrunnelse>(DomenebegrepVedtaksperiodeMedBegrunnelser.BEGRUNNELSER, rad)

                vedtaksperiodeMedBegrunnelser.begrunnelser.addAll(
                    begrunnelser.map {
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                            begrunnelse = it,
                        )
                    },
                )
            }
        }
    }

    fun parseAktørId(rad: MutableMap<String, String>) =
        parseString(DomenebegrepPersongrunnlag.AKTØR_ID, rad).padEnd(13, '0')

    fun parseAktørIdListe(rad: MutableMap<String, String>) =
        parseStringList(DomenebegrepPersongrunnlag.AKTØR_ID, rad).map { it.padEnd(13, '0') }

    enum class DomenebegrepPersongrunnlag(override val nøkkel: String) : Domenenøkkel {
        PERSON_TYPE("Persontype"),
        FØDSELSDATO("Fødselsdato"),
        DØDSFALLDATO("Dødsfalldato"),
        AKTØR_ID("AktørId"),
        IDENT("Ident"),
        ER_INKLUDERT_I_SØKNADEN("Er inkludert i søknaden"),
        ER_FOLKEREGISTRERT("Er folkeregistrert"),
    }

    enum class DomenebegrepVedtaksperiodeMedBegrunnelser(override val nøkkel: String) : Domenenøkkel {
        VEDTAKSPERIODE_TYPE("Vedtaksperiodetype"),
        VILKÅR("Vilkår"),
        UTDYPENDE_VILKÅR("Utdypende vilkår"),
        RESULTAT("Resultat"),
        VURDERES_ETTER("Vurderes etter"),
        BELØP("Beløp"),
        SATS("Sats"),
        ER_EKSPLISITT_AVSLAG("Er eksplisitt avslag"),
        ENDRINGSTIDSPUNKT("Endringstidspunkt"),
        BEGRUNNELSER("Begrunnelser"),
        STANDARDBEGRUNNELSER("Standardbegrunnelser"),
        EØSBEGRUNNELSER("Eøsbegrunnelser"),
        FRITEKSTER("Fritekster"),
    }

    enum class DomenebegrepKompetanse(override val nøkkel: String) : Domenenøkkel {
        SØKERS_AKTIVITET("Søkers aktivitet"),
        ANNEN_FORELDERS_AKTIVITET("Annen forelders aktivitet"),
        SØKERS_AKTIVITETSLAND("Søkers aktivitetsland"),
        ANNEN_FORELDERS_AKTIVITETSLAND("Annen forelders aktivitetsland"),
        BARNETS_BOSTEDSLAND("Barnets bostedsland"),
        RESULTAT("Resultat"),
    }

    enum class DomenebegrepValutakurs(override val nøkkel: String) : Domenenøkkel {
        VALUTAKURSDATO("Valutakursdato"),
        VALUTA_KODE("Valuta kode"),
        KURS("Kurs"),
    }

    enum class DomenebegrepUtenlandskPeriodebeløp(override val nøkkel: String) : Domenenøkkel {
        BELØP("Beløp"),
        VALUTA_KODE("Valuta kode"),
        INTERVALL("Intervall"),
        UTBETALINGSLAND("Utbetalingsland"),
    }

    enum class DomenebegrepEndretUtbetaling(override val nøkkel: String) : Domenenøkkel {
        PROSENT("Prosent"),
        ÅRSAK("Årsak"),
    }

    enum class DomenebegrepAndelTilkjentYtelse(override val nøkkel: String) : Domenenøkkel {
        YTELSE_TYPE("Ytelse type"),
    }
}