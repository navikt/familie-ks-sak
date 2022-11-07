package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import java.time.LocalDate

data class Avslagsperiode(
    override val periodeFom: LocalDate?,
    override val periodeTom: LocalDate?,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.AVSLAG
) : Vedtaksperiode
