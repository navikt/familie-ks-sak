package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.avslagperiode.Avslagsperiode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.opphørsperiode.Opphørsperiode
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "vedtaksperiodetype")
@JsonSubTypes(
    JsonSubTypes.Type(value = Utbetalingsperiode::class, name = "UTBETALING"),
    JsonSubTypes.Type(value = Avslagsperiode::class, name = "AVSLAG"),
    JsonSubTypes.Type(value = Opphørsperiode::class, name = "OPPHØR"),
)
interface Vedtaksperiode {
    val periodeFom: LocalDate?
    val periodeTom: LocalDate?
    val vedtaksperiodetype: Vedtaksperiodetype
}

enum class Vedtaksperiodetype(
    val tillatteBegrunnelsestyper: List<BegrunnelseType>,
) {
    UTBETALING(
        listOf(
            BegrunnelseType.INNVILGET,
            BegrunnelseType.EØS_INNVILGET,
            BegrunnelseType.REDUKSJON,
            BegrunnelseType.FORTSATT_INNVILGET,
            BegrunnelseType.ETTER_ENDRET_UTBETALING,
            BegrunnelseType.ENDRET_UTBETALING,
        ),
    ),
    OPPHØR(listOf(BegrunnelseType.OPPHØR, BegrunnelseType.EØS_OPPHØR, BegrunnelseType.ETTER_ENDRET_UTBETALING)),
    AVSLAG(listOf(BegrunnelseType.AVSLAG, BegrunnelseType.EØS_AVSLAG)),
    FORTSATT_INNVILGET(listOf(BegrunnelseType.FORTSATT_INNVILGET)),
}
